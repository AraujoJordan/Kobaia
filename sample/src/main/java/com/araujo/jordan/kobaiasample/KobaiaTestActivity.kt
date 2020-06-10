package com.araujo.jordan.kobaiasample

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class KobaiaTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clickMeButton.setOnClickListener { clickMeText?.setTextColor(Color.GREEN) }
        fluffyButton.setOnClickListener { clickByDescriptionText?.setTextColor(Color.GREEN) }
        editField.addTextChangedListener(object:TextWatcher {
            override fun afterTextChanged(t: Editable?) {
             if(t?.toString()=="133.37") tapNumpadText?.setTextColor(Color.GREEN)
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countDownButton?.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                countDownButton?.setOnClickListener {
                    countDownText?.setTextColor(Color.GREEN)
                }
                countDownButton?.text = "YOU CAN CLICK ME!"
            }
        }.start()
    }

    override fun onPause() {
        closeAppText?.setTextColor(Color.GREEN)
        super.onPause()
    }
}
