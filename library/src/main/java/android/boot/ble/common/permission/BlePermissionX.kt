package android.boot.ble.common.permission

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.boot.ble.common.R
import android.boot.ble.common.permission.BLEPermission.InteractiveToken
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX


class BlePermissionX(private val activity: FragmentActivity) {

    private val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private var onOpenBt: (Boolean) -> Unit = { }

    private val enableBtLauncher =
        activity.registerForActivityResult(object : ActivityResultContract<Void?, Boolean>() {

            override fun createIntent(context: Context, input: Void?): Intent {
                return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
                return resultCode == Activity.RESULT_OK
            }
        }) {
            onOpenBt(it)
        }


    fun withBlePermission(onDenied: () -> Unit, onReady: () -> Unit) {


        PermissionX.init(activity)
            .permissions(*blePermissions)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    activity.getString(R.string.ble_permission_request_msg),
                    activity.getString(R.string.agree),
                    activity.getString(R.string.cancel)
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    activity.getString(R.string.ble_permission_setting_cfg),
                    activity.getString(R.string.go_to_settings),
                    activity.getString(R.string.cancel)
                )
            }.request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    enableBle(activity) {
                        if (!it) onDenied() else onReady()

                    }
                } else onDenied()
            }

    }


    fun enableBle(
        activity: FragmentActivity,
        onFeatureUnavailable: () -> Unit = {},
        enableBtPrompt: ((InteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultEnableBtPrompt(
            activity
        ),
        onResult: (Boolean) -> Unit = { }
    ) {
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (bluetoothManager == null || adapter == null) {
            onFeatureUnavailable()
            return
        }

        if (adapter.isEnabled) {
            onResult(true)
            return
        }

        enableBle(
            activity,
            adapter,
            onResult = onResult,
            onFeatureUnavailable = onFeatureUnavailable,
            enableBtPrompt = enableBtPrompt
        )
    }


    private fun enableBle(
        activity: FragmentActivity,
        bluetoothAdapter: BluetoothAdapter,
        onResult: (Boolean) -> Unit,
        onFeatureUnavailable: () -> Unit,
        enableBtPrompt: ((InteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultEnableBtPrompt(
            activity
        ),
    ) {
        if (bluetoothAdapter.isEnabled) {
            onFeatureUnavailable()
            return
        }
        onOpenBt = onResult


        if (enableBtPrompt != null) {
            val prompt = object : InteractiveToken {
                override fun proceed() {
                    enableBtLauncher.launch(null)
                }

                override fun cancel() {
                    onResult(false)
                }
            }
            enableBtPrompt(prompt)
        } else {
            enableBtLauncher.launch(null)
        }
    }
}