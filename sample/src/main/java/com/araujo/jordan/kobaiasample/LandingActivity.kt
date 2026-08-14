package com.araujo.jordan.kobaiasample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.araujo.jordan.kobaiasample.databinding.ActivityLandingBinding

class LandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.landingLoginButton.setOnClickListener {
            startActivity(Intent(it.context, LoginActivity::class.java))
        }
    }
}
