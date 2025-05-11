package com.hamid.budgetbuddy

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*

class Transfer : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // UI Components
    private lateinit var saveText: TextView
    private lateinit var cancelText: TextView
    private lateinit var amountText: EditText
    private lateinit var noteEditText: EditText
    private lateinit var currencyTextView: TextView
    private lateinit var incomeBox: LinearLayout
    private lateinit var expenseBox: LinearLayout
    private lateinit var transferBox: LinearLayout
    private lateinit var etFrom: EditText
    private lateinit var etTo: EditText
    private lateinit var dateBox: LinearLayout
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private lateinit var recurringCheckbox: CheckBox
    private lateinit var recurringSpinner: Spinner

    private var selectedType = "Transfer"
    private var selectedCurrency = "USD"
    private var isRecurring = false
    private var recurringFrequency = "Monthly"

    private val conversionRates = mapOf(
        "USD" to 1.0,
        "EUR" to 1.1,
        "INR" to 0.012,
        "PKR" to 0.0036,
        "GBP" to 1.3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transfer)

        initializeFirebase()
        saveFcmTokenToFirestore()
        initializeViews()
        setupUI()
        setCurrentDateTime()
        setupRecurringOptions()
        createNotificationChannel()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun saveFcmTokenToFirestore() {
        val userId = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                db.collection("Users").document(userId)
                    .update("fcmToken", token)
            }
        }
    }

    private fun initializeViews() {
        saveText = findViewById(R.id.saveText)
        cancelText = findViewById(R.id.cancelText)
        amountText = findViewById(R.id.amount)
        noteEditText = findViewById(R.id.description)
        currencyTextView = findViewById(R.id.currencyTextView)
        incomeBox = findViewById(R.id.income)
        expenseBox = findViewById(R.id.expense)
        transferBox = findViewById(R.id.transfer)
        etFrom = findViewById(R.id.etfrom)
        etTo = findViewById(R.id.etTo)
        dateBox = findViewById(R.id.dateBox)
        dateText = findViewById(R.id.dateText)
        timeText = findViewById(R.id.timeText)
        recurringCheckbox = findViewById(R.id.recurringCheckbox)
        recurringSpinner = findViewById(R.id.recurringSpinner)
    }

    private fun setupUI() {
        highlightCurrentPage()

        incomeBox.setOnClickListener { navigateTo(Income::class.java) }
        expenseBox.setOnClickListener { navigateTo(Expense::class.java) }
        transferBox.setOnClickListener { }

        dateBox.setOnClickListener { showDatePicker() }
        timeText.setOnClickListener { showTimePicker() }

        saveText.setOnClickListener { saveEntryToFirestore() }
        cancelText.setOnClickListener { finish() }

        currencyTextView.text = "$selectedCurrency ▼"
        currencyTextView.setOnClickListener { showCurrencyDialog() }
    }

    private fun setCurrentDateTime() {
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        dateText.text = currentDate
        timeText.text = currentTime
    }

    private fun setupRecurringOptions() {
        val frequencies = arrayOf("Daily", "Weekly", "Monthly", "Yearly")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        recurringSpinner.adapter = adapter

        recurringCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isRecurring = isChecked
            recurringSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) recurringFrequency = recurringSpinner.selectedItem.toString()
        }

        recurringSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                recurringFrequency = frequencies[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                recurringFrequency = "Monthly"
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }

    private fun highlightCurrentPage() {
        transferBox.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
        incomeBox.setBackgroundColor(Color.TRANSPARENT)
        expenseBox.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun saveEntryToFirestore() {
        val amountStr = amountText.text.toString().trim()
        val note = noteEditText.text.toString().trim()
        val fromAccount = etFrom.text.toString().trim()
        val toAccount = etTo.text.toString().trim()

        if (amountStr.isEmpty() || fromAccount.isEmpty() || toAccount.isEmpty()) {
            showToast("Please fill all fields")
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val originalAmount = amountStr.toDouble()
        val amount = convertToUSD(originalAmount)

        val transferData = hashMapOf(
            "amount" to amount,
            "from" to fromAccount,
            "to" to toAccount,
            "note" to note,
            "type" to selectedType,
            "timestamp" to System.currentTimeMillis(),
            "originalAmount" to originalAmount,
            "currency" to selectedCurrency,
            "isRecurring" to isRecurring,
            "recurringFrequency" to if (isRecurring) recurringFrequency else null,
            "nextOccurrence" to if (isRecurring) getNextOccurrenceDate() else null
        )

        val userDocRef = db.collection("Users").document(userId)

        userDocRef.collection("Transfer")
            .add(transferData)
            .addOnSuccessListener {
                sendNotification(userDocRef, originalAmount, fromAccount, toAccount)
            }
            .addOnFailureListener { e ->
                showToast("Failed to save transfer: ${e.message}")
            }
    }

    private fun sendNotification(
        userDocRef: com.google.firebase.firestore.DocumentReference,
        originalAmount: Double,
        fromAccount: String,
        toAccount: String
    ) {
        val message = if (isRecurring) {
            "Recurring transfer set: ${String.format("%.2f", originalAmount)} $selectedCurrency from '$fromAccount' to '$toAccount' ($recurringFrequency)"
        } else {
            "You transferred ${String.format("%.2f", originalAmount)} $selectedCurrency from '$fromAccount' to '$toAccount'"
        }

        val notification = hashMapOf(
            "title" to "Transfer Made",
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "type" to "transfer"
        )

        userDocRef.collection("Notifications")
            .add(notification)
            .addOnSuccessListener {
                showToast("Transfer saved successfully")
                navigateToProfile()
            }
            .addOnFailureListener { e ->
                showToast("Transfer saved, but notification failed: ${e.message}")
                navigateToProfile()
            }
    }

    private fun getNextOccurrenceDate(): Long {
        val calendar = Calendar.getInstance()
        when (recurringFrequency) {
            "Daily" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> calendar.add(Calendar.MONTH, 1)
            "Yearly" -> calendar.add(Calendar.YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    .format(GregorianCalendar(year, month, dayOfMonth).time)
                dateText.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                timeText.text = String.format("%02d:%02d", hourOfDay, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun showCurrencyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Choose Currency")
            .setItems(conversionRates.keys.toTypedArray()) { _, which ->
                selectedCurrency = conversionRates.keys.elementAt(which)
                currencyTextView.text = "$selectedCurrency ▼"
            }
            .show()
    }

    private fun convertToUSD(originalAmount: Double): Double {
        return originalAmount * (conversionRates[selectedCurrency] ?: 1.0)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToProfile() {
        startActivity(Intent(this, profile::class.java))
        finish()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "budget_channel",
                "Budget Buddy Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Used for transfer notifications"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
