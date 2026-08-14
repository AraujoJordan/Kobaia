package com.araujo.jordan.kobaiasample

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.araujo.jordan.kobaiasample.databinding.ActivityMainBinding

class KobaiaTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.clickMeButton.setOnClickListener { binding.clickMeText.setTextColor(Color.GREEN) }
        binding.fluffyButton.setOnClickListener { binding.clickByDescriptionText.setTextColor(Color.GREEN) }
        binding.editField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(t: Editable?) {
                if (t?.toString() == "133.37") binding.tapNumpadText.setTextColor(Color.GREEN)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.countDownButton.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                binding.countDownButton.setOnClickListener {
                    binding.countDownText.setTextColor(Color.GREEN)
                }
                binding.countDownButton.text = "YOU CAN CLICK ME!"
            }
        }.start()
    }

    override fun onPause() {
        binding.closeAppText.setTextColor(Color.GREEN)
        super.onPause()
    }
}
