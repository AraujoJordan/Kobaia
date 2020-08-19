package com.araujo.jordan.kobaiasample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {

                activityLoginLoadingCircle.visibility = View.VISIBLE
                delay(2000)
                activityLoginLoadingCircle.visibility = View.GONE


                if (email.text.toString() == "right_email@kobaia.com" && pass.text.toString() == "12345678") {
                    loginCard.visibility = View.INVISIBLE
                    splashLogged.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@LoginActivity, "Wrong credentials!", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }
}
