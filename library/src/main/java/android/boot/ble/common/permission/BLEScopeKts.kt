package android.boot.ble.common.permission

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

class BLEPermission(private val activity: ComponentActivity) {
    interface InteractiveToken {
        fun proceed()
        fun cancel()
    }

    interface PermissionInteractiveToken : InteractiveToken {
        fun permissions(): Array<String>
    }

    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

    private val preferences by lazy {
        activity.getSharedPreferences("permissions", MODE_PRIVATE)
    }

    private val enableBtContract = object : ActivityResultContract<Void?, Boolean>() {

        override fun createIntent(context: Context, input: Void?): Intent {
            return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == Activity.RESULT_OK
        }
    }


    private var enableBtResultCallbacks = listOf<ActivityResultCallback<Boolean>>()

    // registerForActivityResult 方法必须在Activity onStart()生命周期之前，否则会抛异常。
    private val enableBtLauncher = activity.registerForActivityResult(enableBtContract) { result ->
        enableBtResultCallbacks.forEach { callback ->
            callback.onActivityResult(result)
        }
    }


    private var blePermissionResultCallbacks =
        listOf<ActivityResultCallback<Map<String, Boolean>>>()
    private val requestBluetoothPermissionsLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            blePermissionResultCallbacks.forEach { callback ->
                callback.onActivityResult(permissions)
            }
        }

    private fun hasAllBlePermissions() =
        blePermissions.all { activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }


    fun enableBLE(
        onFeatureUnavailable: () -> Unit = {},
        enableBtPrompt: ((InteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultEnableBtPrompt(
            activity
        ),
        onResult: (Boolean, BluetoothAdapter) -> Unit = { _, _ -> }
    ) {
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (bluetoothManager == null || adapter == null) {
            onFeatureUnavailable()
            return
        }

        if (adapter.isEnabled) {
            onResult(true, adapter)
            return
        }

        enableBLE(
            adapter,
            onResult = onResult,
            onFeatureUnavailable = onFeatureUnavailable,
            enableBtPrompt = enableBtPrompt
        )
    }


    fun enableBLE(
        bluetoothAdapter: BluetoothAdapter,
        onResult: (Boolean, BluetoothAdapter) -> Unit,
        onFeatureUnavailable: () -> Unit,
        enableBtPrompt: ((InteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultEnableBtPrompt(
            activity
        ),
    ) {
        if (bluetoothAdapter.isEnabled) {
            onFeatureUnavailable()
            return
        }


        @Suppress("ObjectLiteralToLambda")
        object : ActivityResultCallback<Boolean> {
            override fun onActivityResult(result: Boolean) {
                if (result) onResult(true, bluetoothAdapter) else onResult(false, bluetoothAdapter)
                enableBtResultCallbacks = enableBtResultCallbacks.filter { it != this }
            }

        }.apply {
            val callback = this
            enableBtResultCallbacks = enableBtResultCallbacks
                .toMutableList()
                .apply { add(callback) }
                .toList()
        }

        if (enableBtPrompt != null) {
            val prompt = object : InteractiveToken {
                override fun proceed() {
                    enableBtLauncher.launch(null)
                }

                override fun cancel() {
                    onResult(false, bluetoothAdapter)
                }
            }
            enableBtPrompt(prompt)
        } else {
            enableBtLauncher.launch(null)
        }
    }


    fun withBLE(
        onFeatureUnavailable: () -> Unit = {},
        onBleDisabled: (() -> Unit)? = null,
        enableBtPrompt: ((InteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultEnableBtPrompt(
            activity
        ),
        onPermissionDenied: (Array<String>) -> Unit = {},
        rationalePermissionPrompt: ((PermissionInteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultRationalBlePermissionPrompt(
            activity
        ),
        permanentlyDeniedPermissionPrompt: ((PermissionInteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultPermanentlyDeniedPermissionPrompt(
            activity
        ),
        goodToGo: (BluetoothAdapter) -> Unit = {}
    ) {
        withBLEPermission(
            rationalePermissionPrompt = rationalePermissionPrompt,
            permanentlyDeniedPermissionPrompt = permanentlyDeniedPermissionPrompt,
            onPermissionDenied = onPermissionDenied
        ) {
            enableBLE(
                onFeatureUnavailable = onFeatureUnavailable,
                enableBtPrompt = enableBtPrompt
            ) { enabled, adapter ->
                if (enabled) goodToGo(adapter) else onBleDisabled?.invoke()
            }
        }
    }

    fun withBLEPermission(
        onPermissionDenied: (Array<String>) -> Unit = {},
        rationalePermissionPrompt: ((PermissionInteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultRationalBlePermissionPrompt(
            activity
        ),
        permanentlyDeniedPermissionPrompt: ((PermissionInteractiveToken) -> Unit)? = InteractiveTokenDefaults.defaultPermanentlyDeniedPermissionPrompt(
            activity
        ),
        onGranted: () -> Unit
    ) {
        if (hasAllBlePermissions()) {
            onGranted()
            return
        }

        val remainPermissions =
            blePermissions
                .filter { activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                .toTypedArray()


        val permissionsToRationale = remainPermissions.filter {
            !isPermissionPermanentlyDenied(it)
        }.toTypedArray()

//        val permissionsToRequest = remainPermissions.filter {
//            !ActivityCompat.shouldShowRequestPermissionRationale(
//                activity,
//                it
//            ) && !isPermissionPermanentlyDenied(it)
//        }.toTypedArray()

        val permanentlyDeniedPermissions = remainPermissions.filter {
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                it
            ) && isPermissionPermanentlyDenied(it)
        }.toTypedArray()


        @Suppress("ObjectLiteralToLambda")
        object : ActivityResultCallback<Map<String, Boolean>> {
            override fun onActivityResult(result: Map<String, Boolean>) {
                val unGrantedPermissions =
                    result.entries.filter { !it.value }.map { it.key }.toTypedArray()
                if (unGrantedPermissions.isEmpty()) {
                    onGranted()
                } else {
                    onPermissionDenied(unGrantedPermissions)
                    unGrantedPermissions.filter {
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                    }.toTypedArray().run {
                        savePermanentlyDeniedPermissions(this)
                    }
                }
                blePermissionResultCallbacks = blePermissionResultCallbacks.filter { it != this }
            }
        }.apply {
            val callback = this
            blePermissionResultCallbacks = blePermissionResultCallbacks
                .toMutableList()
                .apply { add(callback) }
                .toList()
        }


//        if (permissionsToRequest.isNotEmpty()) {
//            requestBluetoothPermissionsLauncher.launch(permissionsToRequest)
//        }

        if (permissionsToRationale.isNotEmpty()) {
            if (rationalePermissionPrompt != null) {
                val token = object : PermissionInteractiveToken {
                    override fun permissions(): Array<String> {
                        return permissionsToRationale
                    }

                    override fun proceed() {
                        requestBluetoothPermissionsLauncher.launch(permissionsToRationale)
                    }

                    override fun cancel() {
                        onPermissionDenied(permissionsToRationale)
                    }
                }
                rationalePermissionPrompt(token)
            } else {
                requestBluetoothPermissionsLauncher.launch(permissionsToRationale)
            }
        }

        if (permanentlyDeniedPermissions.isNotEmpty()) {
            if (permanentlyDeniedPermissionPrompt != null) {
                val token = object : PermissionInteractiveToken {
                    override fun permissions(): Array<String> {
                        return permanentlyDeniedPermissions
                    }

                    override fun proceed() {
                        // Consider this as permission denied
                        onPermissionDenied(permanentlyDeniedPermissions)
                        openSettings()
                    }

                    override fun cancel() {
                        onPermissionDenied(permanentlyDeniedPermissions)
                    }
                }
                permanentlyDeniedPermissionPrompt(token)
            }
        }
    }


    private fun openSettings() {
        runCatching {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).run {
                setData(Uri.fromParts("package", activity.packageName, null))
                activity.startActivity(this)
            }
        }
    }

    private fun savePermanentlyDeniedPermissions(permissions: Array<String>) {
        permissions.forEach { preferences.edit().putBoolean(it, true).apply() }
    }

    private fun isPermissionPermanentlyDenied(permission: String): Boolean {
        return preferences.getBoolean(permission, false)
    }
}