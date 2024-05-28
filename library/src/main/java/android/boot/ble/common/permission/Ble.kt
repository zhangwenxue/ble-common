package android.boot.ble.common.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.ScanFilter
import androidx.bluetooth.ScanResult
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class Ble(private val context: Context) {
    private val innerScope = CoroutineScope(Dispatchers.Default)
    private var scanResultList = listOf<ScanResult>()

    @Volatile
    private var scanJobs: Job? = null
    private val ble by lazy {
        BluetoothLe(context)
    }

    private val _scanResultFlow = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResultFlow: MutableStateFlow<List<ScanResult>> = _scanResultFlow

    fun scan(
        scope: CoroutineScope,
        scanFilters: List<ScanFilter> = emptyList(),
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) throw RuntimeException("permission android.permission.BLUETOOTH_SCAN not granted!")
        scanResultList = emptyList()
        scanJobs?.cancel()
        val job = innerScope.launch {
            ble.scan(scanFilters).stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = null
            ).collect { result ->
                if (result == null) return@collect
                val idx =
                    scanResultList.indexOfFirst { result.deviceAddress.address == it.deviceAddress.address }
                if (idx >= 0) {
                    scanResultList = scanResultList.toMutableList().apply {
                        this[idx] = result
                    }.toList()
                } else {
                    scanResultList = scanResultList.toMutableList().apply {
                        add(result)
                    }.toList()
                }
                _scanResultFlow.emit(scanResultList.filter { it.isConnectable() })
            }
        }
        scanJobs = job
    }

    fun stopScan() {
        scanJobs?.cancel()
    }

    /*suspend fun connectGatt(bluetoothDevice: BluetoothDevice) {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) throw RuntimeException("permission android.permission.BLUETOOTH_SCAN not granted!")

        ble.connectGatt(bluetoothDevice) {
            val gattService = getService(UUID.fromString("0000fe40-cc7a-482a-984a-7f2ed5b3e58f"))
            val notifyChar =
                gattService?.getCharacteristic(UUID.fromString("0000fe42-8e22-4541-9d4c-21edae82ed19"))
            val writeCharacteristics =
                gattService?.getCharacteristic(UUID.fromString("0000fe41-8e22-4541-9d4c-21edae82ed19"))

            val cmd = byteArrayOf(0xA5.toByte(), 0x09, 0x00, 0x09, 0x5A)
            writeCharacteristics?.let {
                writeCharacteristic(it, cmd).onSuccess {
                    Log.i("_BleData", "write success")
                }.onFailure {
                    Log.i("_BleData", "write failed")
                }
            }

            if (notifyChar != null) {
                this.subscribeToCharacteristic(notifyChar).collect {
                    Log.i("_BleData", it.joinToString { ch -> String.format("%02x", ch) })
                }
            }
        }
    }*/
}

