package com.araujo.jordan.kobaiasample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.araujo.jordan.kobaiasample.databinding.ActivityLoginBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            lifecycleScope.launch {

                binding.activityLoginLoadingCircle.visibility = View.VISIBLE
                delay(2000)
                binding.activityLoginLoadingCircle.visibility = View.GONE


                if (binding.email.text.toString() == "right_email@kobaia.com" && binding.pass.text.toString() == "12345678") {
                    binding.loginCard.visibility = View.INVISIBLE
                    binding.splashLogged.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@LoginActivity, "Wrong credentials!", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }
}
