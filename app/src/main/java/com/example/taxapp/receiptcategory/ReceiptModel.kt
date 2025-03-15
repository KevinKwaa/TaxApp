package com.example.taxapp.receiptcategory

import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

data class ReceiptModel(
    val id: String = "",
    val userId: String = "",
    val merchantName: String = "",
    val total: Double = 0.0,
    val date: Date = Date(),
    val category: String = "",
    val imageUrl: String = "",
    val items: List<ExpenseItem> = emptyList(),
    val timestamp: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(), // Added ID field with auto-generation
    val description: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    // Added fields to make each expense item standalone
    val merchantName: String = "",
    val date: Date = Date()
)

