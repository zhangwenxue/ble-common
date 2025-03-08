package android.boot.ble.common.permission

import android.app.Activity
import android.app.AlertDialog
import android.boot.ble.common.R

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
                activity.getString(R.string.bluetooth_not_enabled),
                activity.getString(R.string.enable_bt_tips),
                ButtonHandle(activity.getString(R.string.agree)) {
                    it.proceed()
                },
                ButtonHandle(activity.getString(R.string.cancel)) {
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
) = AlertDialog.Builder(
    activity,
    android.R.style.Theme_Material
).setCancelable(cancelAble).setTitle(title)
    .apply {
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
