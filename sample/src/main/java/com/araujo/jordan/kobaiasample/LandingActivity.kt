package com.araujo.jordan.kobaiasample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_landing.*

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        landingLoginButton.setOnClickListener {
            startActivity(Intent(it.context, LoginActivity::class.java))
        }
    }
}
