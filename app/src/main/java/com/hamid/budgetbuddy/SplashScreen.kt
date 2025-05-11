package com.hamid.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        val logo: ImageView = findViewById(R.id.logo)

        val anim = AnimationUtils.loadAnimation(this, R.anim.translation)
        logo.startAnimation(anim)

        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Splash Started", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }
}