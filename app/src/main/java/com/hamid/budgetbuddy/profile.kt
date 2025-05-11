package com.hamid.budgetbuddy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class profile : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileId: TextView
    private lateinit var editProfileBtn: LinearLayout
    private lateinit var logoutBtn: LinearLayout
    private lateinit var leaderBtn: LinearLayout
    private lateinit var cancel: ImageView

    private lateinit var homeBtn: ImageView
    private lateinit var addBtn: ImageView
    private lateinit var transactionBtn: ImageView
    private lateinit var profileBtn: ImageView
    private lateinit var analysisBtn : ImageView
    private lateinit var notificationBtn: ImageView

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val PICK_IMAGE_REQUEST = 1
    private val PREFS_NAME = "ProfilePrefs"
    private val IMAGE_PATH_KEY = "imagePath"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initializing views
        profileImage = findViewById(R.id.profileImage)
        profileName = findViewById(R.id.profileName)
        profileId = findViewById(R.id.profileId)
        editProfileBtn = findViewById(R.id.editProfileBtn)
        logoutBtn = findViewById(R.id.logoutBtn)
        cancel = findViewById(R.id.backArrow)
        homeBtn = findViewById(R.id.homeBtn)
        transactionBtn = findViewById(R.id.transactionBtn)
        profileBtn = findViewById(R.id.profileBtn)
        analysisBtn = findViewById(R.id.AnalysisBtn)
        addBtn = findViewById(R.id.add)
        notificationBtn = findViewById(R.id.notificationIcon)
        leaderBtn = findViewById(R.id.leaderBtn)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadProfileInfo()
        loadProfileImage()

        profileImage.setOnClickListener {
            selectImageFromGallery()
        }

        editProfileBtn.setOnClickListener {
            startActivity(Intent(this, EditProfile::class.java))
        }

        cancel.setOnClickListener{
            finish()
        }

        logoutBtn.setOnClickListener {
            logout()
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

        leaderBtn.setOnClickListener {
            startActivity(Intent(this, Leaderboard::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileInfo()
        loadProfileImage()
    }

    private fun loadProfileInfo() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: "No Username"
                    val email = document.getString("email") ?: "No Email"

                    profileName.text = username
                    profileId.text = "Email: $email"
                } else {
                    Toast.makeText(this, "Profile does not exist", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileImage() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(IMAGE_PATH_KEY, null)

        if (path != null) {
            val imgFile = File(path)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                profileImage.setImageBitmap(bitmap)
            }
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let {
                saveImageToInternalStorage(it)
                profileImage.setImageURI(it)
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(filesDir, "profile_image.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(IMAGE_PATH_KEY, file.absolutePath).apply()

        inputStream?.close()
        outputStream.close()
    }

    private fun updateProfileInfo(name: String, id: String) {
        val userId = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "username" to name,
            "email" to id
        )

        firestore.collection("Users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()
                profileName.text = name
                profileId.text = "Email: $id"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            dialog.dismiss()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

}
