package com.example.otpview

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.main_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val otpView = findViewById<OtpView>(R.id.otp_view)
        val keyboardButton = findViewById<Button>(R.id.keyboard_button)
        val setErrorButton = findViewById<Button>(R.id.set_error_button)
        val clearErrorButton = findViewById<Button>(R.id.clear_error_button)

        otpView.setOnInputFilledListener {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }

        keyboardButton.setOnClickListener {
            otpView.showSoftKeyboard()
        }

        setErrorButton.setOnClickListener {
            otpView.isInputErrorEnabled = true
        }

        clearErrorButton.setOnClickListener {
            otpView.isInputErrorEnabled = false
        }
    }
}