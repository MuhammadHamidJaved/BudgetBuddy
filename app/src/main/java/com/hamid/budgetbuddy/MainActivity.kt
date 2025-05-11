package com.hamid.budgetbuddy

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberMeCheckBox: CheckBox
    private lateinit var forgotPasswordText: TextView
    private lateinit var loginButton: Button
    private lateinit var signUpText: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)
        loginButton = findViewById(R.id.loginButton)
        signUpText = findViewById(R.id.signUpText)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        checkRememberMe()

        loginButton.setOnClickListener {
            loginUser()
        }

        signUpText.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }

        forgotPasswordText.setOnClickListener {
            val email = usernameEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reset link sent to email", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to send reset link: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun loginUser() {
        val email = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        db.collection("Users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val username = document.getString("username")
                                    val phone = document.getString("phone")

                                    Toast.makeText(
                                        this,
                                        "Welcome back, $username!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    if (rememberMeCheckBox.isChecked) {
                                        saveLoginDetails(email, password)
                                    } else {
                                        clearLoginDetails()
                                    }

                                    startActivity(Intent(this, Home::class.java))
                                } else {
                                    Toast.makeText(
                                        this,
                                        "User data not found.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to fetch user data: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Login Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveLoginDetails(email: String, password: String) {
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.putBoolean("remember", true)
        editor.apply()
    }

    private fun clearLoginDetails() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun checkRememberMe() {
        val remember = sharedPreferences.getBoolean("remember", false)
        if (remember) {
            val email = sharedPreferences.getString("email", "")
            val password = sharedPreferences.getString("password", "")

            usernameEditText.setText(email)
            passwordEditText.setText(password)
            rememberMeCheckBox.isChecked = true

            if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
                loginUser()
            }
        }
    }
}
