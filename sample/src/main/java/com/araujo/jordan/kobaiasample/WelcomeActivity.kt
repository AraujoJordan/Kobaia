package com.araujo.jordan.kobaiasample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_welcome.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        welcomeSkipButton.setOnClickListener {
            welcomeSkipButton.visibility = View.GONE
            welcomeNextButton.visibility = View.GONE
            welcomeTutorialTextView.text = "Tutorial Page 1"
            welcomeGetStartedButton.visibility = View.VISIBLE
            welcomeTutorialTextView.text = "Tutorial Page 3"
        }

        welcomeGetStartedButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                delay(500)
                startActivity(Intent(it.context, LandingActivity::class.java))
            }
        }
    }
}
