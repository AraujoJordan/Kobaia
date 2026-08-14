package com.araujo.jordan.kobaiasample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.araujo.jordan.kobaiasample.databinding.ActivityWelcomeBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.welcomeSkipButton.setOnClickListener {
            binding.welcomeSkipButton.visibility = View.GONE
            binding.welcomeNextButton.visibility = View.GONE
            binding.welcomeTutorialTextView.text = "Tutorial Page 1"
            binding.welcomeGetStartedButton.visibility = View.VISIBLE
            binding.welcomeTutorialTextView.text = "Tutorial Page 3"
        }

        binding.welcomeGetStartedButton.setOnClickListener {
            lifecycleScope.launch {
                delay(500)
                startActivity(Intent(it.context, LandingActivity::class.java))
            }
        }
    }
}
