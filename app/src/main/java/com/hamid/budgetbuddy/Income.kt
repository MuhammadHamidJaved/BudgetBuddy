package com.hamid.budgetbuddy

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Income : AppCompatActivity() {

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
    private lateinit var interest: LinearLayout
    private lateinit var invest: LinearLayout
    private lateinit var business: LinearLayout
    private lateinit var salary: LinearLayout
    private lateinit var other: LinearLayout
    private lateinit var dateBox: LinearLayout
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private lateinit var recurringCheckbox: CheckBox
    private lateinit var recurringSpinner: Spinner

    // Data variables
    private var selectedCategory: String = ""
    private var selectedType: String = "Income"
    private var selectedCurrency = "USD"
    private var isRecurring = false
    private var recurringFrequency = "Monthly"

    // Currency conversion rates
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
        setContentView(R.layout.activity_income)

        initializeFirebase()
        initializeViews()
        setupUI()
        setCurrentDateTime()
        setupRecurringOptions()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
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
        interest = findViewById(R.id.interest)
        invest = findViewById(R.id.invest)
        business = findViewById(R.id.business)
        salary = findViewById(R.id.salary)
        other = findViewById(R.id.other)
        dateBox = findViewById(R.id.dateBox)
        dateText = findViewById(R.id.dateText)
        timeText = findViewById(R.id.timeText)
        recurringCheckbox = findViewById(R.id.recurringCheckbox)
        recurringSpinner = findViewById(R.id.recurringSpinner)
    }

    private fun setupUI() {
        highlightCurrentPage()

        // Set click listeners
        incomeBox.setOnClickListener { /* Already on income page */ }
        expenseBox.setOnClickListener { navigateTo(Expense::class.java) }
        transferBox.setOnClickListener { navigateTo(Transfer::class.java) }

        invest.setOnClickListener { selectCategory("invest", invest) }
        interest.setOnClickListener { selectCategory("interest", interest) }
        business.setOnClickListener { selectCategory("business", business) }
        salary.setOnClickListener { selectCategory("salary", salary) }
        other.setOnClickListener { selectCategory("other", other) }

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
            if (isChecked) {
                recurringFrequency = recurringSpinner.selectedItem.toString()
            }
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

    private fun selectCategory(category: String, selectedLayout: LinearLayout) {
        selectedCategory = category
        val allCategories = listOf(interest, invest, business, salary, other)
        for (layout in allCategories) {
            layout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        }
        selectedLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
    }

    private fun highlightCurrentPage() {
        incomeBox.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
    }

    private fun saveEntryToFirestore() {
        val amountStr = amountText.text.toString().trim()
        val note = noteEditText.text.toString().trim()

        if (amountStr.isEmpty()) {
            showToast("Enter amount")
            return
        }

        if (selectedCategory.isEmpty()) {
            showToast("Select Category")
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val originalAmount = amountStr.toDouble()
        val amount = convertToUSD(originalAmount)

        val transactionData = hashMapOf(
            "amount" to amount,
            "note" to note,
            "category" to selectedCategory,
            "type" to selectedType,
            "timestamp" to System.currentTimeMillis(),
            "currency" to selectedCurrency,
            "originalAmount" to originalAmount,
            "isRecurring" to isRecurring,
            "recurringFrequency" to if (isRecurring) recurringFrequency else null,
            "nextOccurrence" to if (isRecurring) getNextOccurrenceDate() else null
        )

        val userDocRef = db.collection("Users").document(userId)

        userDocRef.collection("Transaction")
            .add(transactionData)
            .addOnSuccessListener {
                updateUserIncome(userDocRef, amount, originalAmount)
            }
            .addOnFailureListener { e ->
                showToast("Failed to save transaction: ${e.message}")
            }
    }

    private fun updateUserIncome(userDocRef: com.google.firebase.firestore.DocumentReference, amount: Double, originalAmount: Double) {
        userDocRef.update("income", com.google.firebase.firestore.FieldValue.increment(amount))
            .addOnSuccessListener {
                sendNotification(userDocRef, originalAmount)
            }
            .addOnFailureListener { e ->
                showToast("Failed to update income: ${e.message}")
            }
    }

    private fun sendNotification(userDocRef: com.google.firebase.firestore.DocumentReference, originalAmount: Double) {
        val message = if (isRecurring) {
            "Recurring income set: ${String.format("%.2f", originalAmount)} $selectedCurrency ($recurringFrequency)"
        } else {
            "You added ${String.format("%.2f", originalAmount)} $selectedCurrency under '$selectedCategory'"
        }

        val notification = hashMapOf(
            "title" to "Income Recorded",
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "type" to "income"
        )

        userDocRef.collection("Notifications")
            .add(notification)
            .addOnSuccessListener {
                showToast("Saved successfully")
                finish()
            }
            .addOnFailureListener { e ->
                showToast("Transaction saved, but notification failed: ${e.message}")
                finish()
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
}