package android.boot.ble.common.permission

import android.app.Activity
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

data class ButtonHandle(
    val name: String,
    val dismissButtonOnClick: Boolean = true,
    val onClick: () -> Unit = {}
)

object InteractiveTokenDefaults {

    fun defaultRationalBlePermissionPrompt(activity: Activity): (BLEPermission.PermissionInteractiveToken) -> Unit =
        {
            baseInteractiveDialog(
                activity,
                "是否允许蓝牙数据传输？",
                "本应用需要通过蓝牙与设备互相传输数据。",
                ButtonHandle("同意") { it.proceed() },
                ButtonHandle("拒绝") { it.cancel() },
            ).show()
        }

    fun defaultPermanentlyDeniedPermissionPrompt(activity: Activity): (BLEPermission.PermissionInteractiveToken) -> Unit =
        {
            baseInteractiveDialog(
                activity,
                "授权提示",
                "在使用过程中，本应用需要访问蓝牙扫描和蓝牙连接权限，用于扫描和连接蓝牙设备",
                ButtonHandle("去授权") { it.proceed() },
                ButtonHandle("取消") { it.cancel() },
            ).show()
        }

    fun defaultEnableBtPrompt(activity: Activity): (BLEPermission.InteractiveToken) -> Unit =
        {
            baseInteractiveDialog(
                activity = activity,
                "蓝牙开启提示",
                "本应用需要开启蓝牙功能，用于扫描和连接蓝牙设备。\n您的蓝牙未开启,是否帮您开启？",
                ButtonHandle("同意") {
                    it.proceed()
                },
                ButtonHandle("拒绝") {
                    Toast.makeText(activity, "请到设置页手动开启蓝牙开关", Toast.LENGTH_SHORT)
                        .show()
                    it.cancel()
                }
            ).show()
        }


}


fun baseInteractiveDialog(
    activity: Activity,
    title: String = "",
    desc: String? = null,
    positiveButtonHandle: ButtonHandle? = null,
    negativeButtonHandle: ButtonHandle? = null,
    neutralButtonHandle: ButtonHandle? = null,
    cancelAble: Boolean = false,
) = MaterialAlertDialogBuilder(activity).apply {

    setCancelable(cancelAble)

    setTitle(title)

    desc?.takeIf { it.isNotEmpty() }?.let {
        setMessage(it)
    }

    positiveButtonHandle?.let {
        setPositiveButton(it.name) { dialog, _ ->
            it.onClick()
            if (it.dismissButtonOnClick) dialog.dismiss()
        }
    }
    negativeButtonHandle?.let {
        setNegativeButton(it.name) { dialog, _ ->
            it.onClick()
            if (it.dismissButtonOnClick) dialog.dismiss()
        }
    }

    neutralButtonHandle?.let {
        setNeutralButton(it.name) { dialog, _ ->
            it.onClick
            if (it.dismissButtonOnClick) dialog.dismiss()
        }
    }

}.create()
