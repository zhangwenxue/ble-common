package android.boot.ble.common

import android.boot.ble.common.permission.Ble
import android.boot.ble.common.permission.BleScope
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.bluetooth.ScanFilter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private val bleScope = BleScope(this)
    private val bleScanner by lazy {
        Ble(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textView = findViewById<TextView>(R.id.text)
        textView.setMovementMethod(ScrollingMovementMethod())


        findViewById<View>(R.id.scan).setOnClickListener {
            bleScope.withBle(
                onBleDisabled = { Toast.makeText(this, "Ble disabled", Toast.LENGTH_SHORT).show() },
                onPermissionDenied = {
                    Toast.makeText(this, "权限被拒绝\n${it.joinToString("\n")}", Toast.LENGTH_SHORT)
                        .show()
                }
            ) {
                Toast.makeText(this, "You are good to go", Toast.LENGTH_SHORT).show()
                bleScanner.scan(
                    lifecycleScope,
                    listOf(ScanFilter(deviceName = "WWKECG12E"))
                )
                textView.text = ""
            }
        }

        findViewById<View>(R.id.stop).setOnClickListener {
            bleScanner.stopScan()
        }

        lifecycleScope.launch {
            bleScanner.scanResultFlow.collect {
                val text = it.joinToString(separator = "\n", transform = {
                    "${it.device.name}-${it.deviceAddress.address}(${it.rssi})${it.isConnectable()}-${it.device.bondState}"
                })
                textView.text = text
                it.firstOrNull()?.run {
                    bleScanner.stopScan()
                    bleScanner.connectGatt(this.device)
                }

            }
        }
    }
}