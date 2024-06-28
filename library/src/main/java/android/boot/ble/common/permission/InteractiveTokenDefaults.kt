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
                "蓝牙权限申请提示",
                "您需要授予蓝牙权限来使用所有功能，是否继续？",
                ButtonHandle("继续") { it.proceed() },
                ButtonHandle("拒绝") { it.cancel() },
            ).show()
        }

    fun defaultPermanentlyDeniedPermissionPrompt(activity: Activity): (BLEPermission.PermissionInteractiveToken) -> Unit =
        {
            baseInteractiveDialog(
                activity,
                "蓝牙权限申请提醒",
                "为了正常使用软件所有功能，您需要手动授权蓝牙权限",
                ButtonHandle("去设置") { it.proceed() },
                ButtonHandle("取消") { it.cancel() },
            ).show()
        }

    fun defaultEnableBtPrompt(activity: Activity): (BLEPermission.InteractiveToken) -> Unit =
        {
            baseInteractiveDialog(
                activity = activity,
                "温馨提示",
                "您的蓝牙未开启,无法正常使用软件。是否帮您开启？",
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
