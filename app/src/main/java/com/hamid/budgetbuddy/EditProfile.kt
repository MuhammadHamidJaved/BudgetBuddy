package com.hamid.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfile : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var saveChangesButton: Button
    private lateinit var cancelButton: ImageView
    private lateinit var profileImageView: ImageView
    private lateinit var homeBtn: ImageView
    private lateinit var addBtn: ImageView
    private lateinit var transactionBtn: ImageView
    private lateinit var profileBtn: ImageView
    private lateinit var analysisBtn : ImageView
    private lateinit var notificationBtn: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        saveChangesButton = findViewById(R.id.saveChangesButton)
        cancelButton = findViewById(R.id.cancelButton)
        profileImageView = findViewById(R.id.profileImage)
        homeBtn = findViewById(R.id.homeBtn)
        transactionBtn = findViewById(R.id.transactionBtn)
        profileBtn = findViewById(R.id.profileBtn)
        analysisBtn = findViewById(R.id.AnalysisBtn)
        addBtn = findViewById(R.id.add)
        notificationBtn = findViewById(R.id.notificationIcon)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadUserData()

        saveChangesButton.setOnClickListener {
            saveChanges()
        }

        cancelButton.setOnClickListener {
            finish() // Go back
        }

        profileImageView.setOnClickListener {
            Toast.makeText(this, "Change photo functionality not implemented yet.", Toast.LENGTH_SHORT).show()
        }

        analysisBtn.setOnClickListener{
            startActivity(Intent(this, Analysis::class.java))
        }

        homeBtn.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        transactionBtn.setOnClickListener {
            startActivity(Intent(this, TransactionsList::class.java))
        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, profile::class.java))
        }

        addBtn.setOnClickListener {
            startActivity(Intent(this, Income::class.java))
        }

        notificationBtn.setOnClickListener {
            startActivity(Intent(this, Notification::class.java))
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: ""
                    val email = document.getString("email") ?: ""
                    val phone = document.getString("phone") ?: ""

                    usernameEditText.setText(username)
                    emailEditText.setText(email)
                    phoneEditText.setText(phone)

                    // You can later load profile picture here too if you add that functionality
                }
                else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChanges() {
        val userId = auth.currentUser?.uid ?: return

        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData = mapOf(
            "username" to username,
            "email" to email,
            "phone" to phone
        )

        firestore.collection("Users").document(userId)
            .update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}
