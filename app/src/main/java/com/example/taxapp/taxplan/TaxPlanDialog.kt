package com.example.taxapp.taxplan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog for creating a new tax plan
 */
@Composable
fun TaxPlanCreateDialog(
    onDismiss: () -> Unit,
    onCreatePlan: (name: String, planType: String) -> Unit
) {
    var planName by remember { mutableStateOf("") }
    var selectedPlanType by remember { mutableStateOf("standard") }

    // Plan types
    val planTypes = listOf(
        "standard" to "Standard Tax Plan",
        "future" to "Future Income Plan (20% higher income)",
        "business" to "Business Venture Plan"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        title = { Text("Create New Tax Plan", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "Generate a new AI-powered tax plan with custom options.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Plan name field
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text("Plan Name (Optional)") },
                    placeholder = { Text("e.g., My Tax Plan 2025") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Plan type selection
                Text(
                    "Select Plan Type:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp)
                ) {
                    planTypes.forEach { (type, description) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedPlanType == type,
                                    onClick = { selectedPlanType = type },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPlanType == type,
                                onClick = null  // handled by row's selectable
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Use default name if empty
                    val finalName = if (planName.isBlank()) {
                        when (selectedPlanType) {
                            "future" -> "Future Income Tax Plan"
                            "business" -> "Business Tax Plan"
                            else -> "My Tax Plan"
                        }
                    } else {
                        planName
                    }

                    onCreatePlan(finalName, selectedPlanType)
                }
            ) {
                Text("Generate Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}