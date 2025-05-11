package com.hamid.budgetbuddy

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Expense : AppCompatActivity() {

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
    private lateinit var education: LinearLayout
    private lateinit var gift: LinearLayout
    private lateinit var food: LinearLayout
    private lateinit var shopping: LinearLayout
    private lateinit var bill: LinearLayout
    private lateinit var traffic: LinearLayout
    private lateinit var medical: LinearLayout
    private lateinit var grocery: LinearLayout
    private lateinit var rental: LinearLayout
    private lateinit var investment: LinearLayout
    private lateinit var loan: LinearLayout
    private lateinit var other: LinearLayout
    private lateinit var dateBox: LinearLayout
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private lateinit var recurringCheckbox: CheckBox
    private lateinit var recurringSpinner: Spinner
    private lateinit var btnSetBudget: Button
    private lateinit var educationBudget: TextView
    private lateinit var giftBudget: TextView
    private lateinit var foodBudget: TextView
    private lateinit var shoppingBudget: TextView
    private lateinit var billBudget: TextView
    private lateinit var trafficBudget: TextView
    private lateinit var medicalBudget: TextView
    private lateinit var groceryBudget: TextView
    private lateinit var rentalBudget: TextView
    private lateinit var investmentBudget: TextView
    private lateinit var loanBudget: TextView
    private lateinit var otherBudget: TextView

    // Data variables
    private var selectedCategory: String = ""
    private var selectedType: String = "Expense"
    private var selectedCurrency = "USD"
    private var isRecurring = false
    private var recurringFrequency = "Monthly"
    private val budgetMap = mutableMapOf<String, Double>()

    // Currency conversion rates
    private val conversionRates = mapOf(
        "USD" to 1.0,
        "EUR" to 1.1,
        "INR" to 0.012,
        "PKR" to 0.0036,
        "GBP" to 1.3
    )

    private var categoryLayouts: List<Pair<LinearLayout, String>> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense)

        initializeFirebase()
        initializeViews()
        setupCategoryList()
        setupUI()
        setCurrentDateTime()
        setupRecurringOptions()
        loadBudgets()
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
        education = findViewById(R.id.education)
        gift = findViewById(R.id.gift)
        food = findViewById(R.id.food)
        shopping = findViewById(R.id.shopping)
        bill = findViewById(R.id.bills)
        traffic = findViewById(R.id.traffic)
        medical = findViewById(R.id.medical)
        grocery = findViewById(R.id.grocery)
        rental = findViewById(R.id.rental)
        investment = findViewById(R.id.investment)
        loan = findViewById(R.id.loan)
        other = findViewById(R.id.other)
        dateBox = findViewById(R.id.dateBox)
        dateText = findViewById(R.id.dateText)
        timeText = findViewById(R.id.timeText)
        recurringCheckbox = findViewById(R.id.recurringCheckbox)
        recurringSpinner = findViewById(R.id.recurringSpinner)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        educationBudget = findViewById(R.id.educationBudget)
        giftBudget = findViewById(R.id.giftBudget)
        foodBudget = findViewById(R.id.foodBudget)
        shoppingBudget = findViewById(R.id.shoppingBudget)
        billBudget = findViewById(R.id.billBudget)
        trafficBudget = findViewById(R.id.trafficBudget)
        medicalBudget = findViewById(R.id.medicalBudget)
        groceryBudget = findViewById(R.id.groceryBudget)
        rentalBudget = findViewById(R.id.rentalBudget)
        investmentBudget = findViewById(R.id.investmentBudget)
        loanBudget = findViewById(R.id.loanBudget)
        otherBudget = findViewById(R.id.otherBudget)
    }

    private fun setupCategoryList() {
        categoryLayouts = listOf(
            education to "education",
            gift to "gift",
            food to "food",
            shopping to "shopping",
            bill to "bill",
            traffic to "traffic",
            medical to "medical",
            grocery to "grocery",
            rental to "rental",
            investment to "investment",
            loan to "loan",
            other to "other"
        )
    }

    private fun setupUI() {
        highlightCurrentTab()

        incomeBox.setOnClickListener { navigateTo(Income::class.java) }
        expenseBox.setOnClickListener { /* Already on expense page */ }
        transferBox.setOnClickListener { navigateTo(Transfer::class.java) }

        for ((layout, categoryName) in categoryLayouts) {
            layout.setOnClickListener {
                selectedCategory = categoryName
                highlightSelectedCategory()
                if (budgetMap.containsKey(categoryName)) {
                    updateBudgetTextView(categoryName, budgetMap[categoryName] ?: 0.0)
                }
            }
        }

        dateBox.setOnClickListener { showDatePicker() }
        timeText.setOnClickListener { showTimePicker() }

        saveText.setOnClickListener {
            if (selectedCategory.isNotEmpty()) {
                checkBudgetBeforeExpense()
            } else {
                showToast("Please select a category")
            }
        }

        cancelText.setOnClickListener { finish() }

        currencyTextView.text = "$selectedCurrency ▼"
        currencyTextView.setOnClickListener { showCurrencyDialog() }

        btnSetBudget.setOnClickListener { showBudgetDialog() }
    }

    private fun showBudgetDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_budget, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategories)
        val etAmount = dialogView.findViewById<EditText>(R.id.etBudgetAmount)
        val tvCurrency = dialogView.findViewById<TextView>(R.id.tvCurrency)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveBudget)

        tvCurrency.text = selectedCurrency

        // Setup spinner with categories
        val categories = categoryLayouts.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinner.adapter = adapter

        btnSave.setOnClickListener {
            val selectedCategory = spinner.selectedItem.toString()
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

            if (amount > 0) {
                saveBudgetToFirestore(selectedCategory, amount)
                showToast("Budget set for $selectedCategory")
                dialog.dismiss()
            } else {
                showToast("Please enter a valid amount")
            }
        }
        dialog.show()
    }

    private fun saveBudgetToFirestore(category: String, amount: Double) {
        val userId = auth.currentUser?.uid ?: return
        val currentMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())

        val budgetData = hashMapOf(
            "category" to category,
            "amount" to amount,
            "currency" to selectedCurrency,
            "monthYear" to currentMonth
        )

        db.collection("Users").document(userId)
            .collection("Budgets")
            .document(category)
            .set(budgetData)
            .addOnSuccessListener {
                budgetMap[category] = amount
                updateBudgetTextView(category, amount)
                showToast("Budget saved successfully")
            }
            .addOnFailureListener { e ->
                showToast("Failed to save budget: ${e.message}")
            }
    }

    private fun updateBudgetTextView(category: String, budgetAmount: Double) {
        val userId = auth.currentUser?.uid ?: return

        // Get total spent in this category
        db.collection("Users").document(userId)
            .collection("Transaction")
            .whereEqualTo("category", category)
            .whereEqualTo("type", "Expense")
            .get()
            .addOnSuccessListener { documents ->
                var totalSpent = 0.0
                for (doc in documents) {
                    totalSpent += doc.getDouble("amount") ?: 0.0
                }

                val remaining = budgetAmount - totalSpent
                val budgetText = "${String.format("%.2f", remaining)} / ${String.format("%.2f", budgetAmount)} $selectedCurrency"

                // Set color based on budget status
                val color = if (remaining < 0) Color.RED else Color.GREEN

                when (category) {
                    "education" -> {
                        educationBudget.text = budgetText
                        educationBudget.setTextColor(color)
                    }
                    "gift" -> {
                        giftBudget.text = budgetText
                        giftBudget.setTextColor(color)
                    }
                    "food" -> {
                        foodBudget.text = budgetText
                        foodBudget.setTextColor(color)
                    }
                    "shopping" -> {
                        shoppingBudget.text = budgetText
                        shoppingBudget.setTextColor(color)
                    }
                    "bill" -> {
                        billBudget.text = budgetText
                        billBudget.setTextColor(color)
                    }
                    "traffic" -> {
                        trafficBudget.text = budgetText
                        trafficBudget.setTextColor(color)
                    }
                    "medical" -> {
                        medicalBudget.text = budgetText
                        medicalBudget.setTextColor(color)
                    }
                    "grocery" -> {
                        groceryBudget.text = budgetText
                        groceryBudget.setTextColor(color)
                    }
                    "rental" -> {
                        rentalBudget.text = budgetText
                        rentalBudget.setTextColor(color)
                    }
                    "investment" -> {
                        investmentBudget.text = budgetText
                        investmentBudget.setTextColor(color)
                    }
                    "loan" -> {
                        loanBudget.text = budgetText
                        loanBudget.setTextColor(color)
                    }
                    "other" -> {
                        otherBudget.text = budgetText
                        otherBudget.setTextColor(color)
                    }
                }
            }
    }

    private fun updateAllBudgetTextViews() {
        for ((_, category) in categoryLayouts) {
            if (budgetMap.containsKey(category)) {
                updateBudgetTextView(category, budgetMap[category] ?: 0.0)
            }
        }
    }

    private fun loadBudgets() {
        val userId = auth.currentUser?.uid ?: return
        val currentMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())

        db.collection("Users").document(userId)
            .collection("Budgets")
            .whereEqualTo("monthYear", currentMonth)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val category = document.getString("category") ?: continue
                    val amount = document.getDouble("amount") ?: continue
                    budgetMap[category] = amount
                }
                updateAllBudgetTextViews()
            }
    }

    private fun checkBudgetBeforeExpense() {
        val amountStr = amountText.text.toString().trim()
        val note = noteEditText.text.toString().trim()

        if (amountStr.isEmpty()) {
            showToast("Please enter amount")
            return
        }

        val amount = amountStr.toDouble()
        val userId = auth.currentUser?.uid ?: return
        val currentMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())

        // Check if budget exists for this category
        if (budgetMap.containsKey(selectedCategory)) {
            // Get total spent this month in this category
            db.collection("Users").document(userId)
                .collection("Transaction")
                .whereEqualTo("category", selectedCategory)
                .whereEqualTo("type", "Expense")
                .get()
                .addOnSuccessListener { documents ->
                    var totalSpent = 0.0
                    for (document in documents) {
                        totalSpent += document.getDouble("amount") ?: 0.0
                    }

                    val budgetLimit = budgetMap[selectedCategory] ?: 0.0
                    val remainingBudget = budgetLimit - totalSpent

                    if (amount > remainingBudget) {
                        showBudgetWarningDialog(amount, remainingBudget)
                    } else {
                        saveEntryToFirestore()
                    }
                }
        } else {
            saveEntryToFirestore()
        }
    }

    private fun showBudgetWarningDialog(attemptedAmount: Double, remainingBudget: Double) {
        AlertDialog.Builder(this)
            .setTitle("Budget Warning")
            .setMessage("This expense will exceed your budget!\n\n" +
                    "Remaining budget: ${String.format("%.2f", remainingBudget)} $selectedCurrency\n" +
                    "Attempted expense: ${String.format("%.2f", attemptedAmount)} $selectedCurrency")
            .setPositiveButton("Save Anyway") { _, _ -> saveEntryToFirestore() }
            .setNegativeButton("Cancel", null)
            .show()
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
                updateUserExpenditure(userDocRef, amount, originalAmount)
                if (budgetMap.containsKey(selectedCategory)) {
                    updateBudgetTextView(selectedCategory, budgetMap[selectedCategory] ?: 0.0)
                }
            }
            .addOnFailureListener { e ->
                showToast("Failed to save transaction: ${e.message}")
            }
    }

    private fun updateUserExpenditure(userDocRef: com.google.firebase.firestore.DocumentReference, amount: Double, originalAmount: Double) {
        userDocRef.update("expenditure", com.google.firebase.firestore.FieldValue.increment(amount))
            .addOnSuccessListener {
                sendNotification(userDocRef, originalAmount)
            }
            .addOnFailureListener { e ->
                showToast("Failed to update expenditure: ${e.message}")
            }
    }

    private fun sendNotification(userDocRef: com.google.firebase.firestore.DocumentReference, originalAmount: Double) {
        val message = if (isRecurring) {
            "Recurring expense set: ${String.format("%.2f", originalAmount)} $selectedCurrency ($recurringFrequency) for '$selectedCategory'"
        } else {
            "You spent ${String.format("%.2f", originalAmount)} $selectedCurrency on '$selectedCategory'"
        }

        val notification = hashMapOf(
            "title" to "Expense Recorded",
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "type" to "expense"
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

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }

    private fun highlightCurrentTab() {
        expenseBox.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
        incomeBox.setBackgroundColor(Color.TRANSPARENT)
        transferBox.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun highlightSelectedCategory() {
        for ((layout, _) in categoryLayouts) {
            layout.setBackgroundColor(Color.TRANSPARENT)
        }
        val selectedLayout = categoryLayouts.firstOrNull { it.second == selectedCategory }?.first
        selectedLayout?.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
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
                updateAllBudgetTextViews()
            }
            .show()
    }

    private fun convertToUSD(originalAmount: Double): Double {
        return originalAmount * (conversionRates[selectedCurrency] ?: 1.0)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (selectedCategory.isNotEmpty()) {
            if (budgetMap.containsKey(selectedCategory)) {
                updateBudgetTextView(selectedCategory, budgetMap[selectedCategory] ?: 0.0)
            }
        }
    }
}