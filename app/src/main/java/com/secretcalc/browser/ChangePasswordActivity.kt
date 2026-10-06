package com.secretcalc.browser

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        BrowserActivity.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val etNew = findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave = findViewById<Button>(R.id.btnSavePassword)

        btnSave.setOnClickListener {
            val newPin = etNew.text.toString()
            val confirmPin = etConfirm.text.toString()
            if (!PinStore.isValid(newPin)) {
                Toast.makeText(this, "请输入6位数字密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPin != confirmPin) {
                Toast.makeText(this, "两次输入不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PinStore.set(this, newPin)
            Toast.makeText(this, "密码已修改", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
