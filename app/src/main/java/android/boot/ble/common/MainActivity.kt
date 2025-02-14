package android.boot.ble.common

import android.boot.ble.common.permission.BlePermissionX
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity


class MainActivity : FragmentActivity() {
    private val BLEPermission = BlePermissionX(this)

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
            BLEPermission.withBlePermission( onDenied = {
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT)
                    .show()
            }){ Toast.makeText(this, "You are good to go", Toast.LENGTH_SHORT).show()}
//            BLEPermission.withBLE(
//                onBleDisabled = { Toast.makeText(this, "Ble disabled", Toast.LENGTH_SHORT).show() },
//                onPermissionDenied = {
//                    Toast.makeText(this, "权限被拒绝\n${it.joinToString("\n")}", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            ) {
//                Toast.makeText(this, "You are good to go", Toast.LENGTH_SHORT).show()
//                textView.text = ""
//            }
        }

    }
}