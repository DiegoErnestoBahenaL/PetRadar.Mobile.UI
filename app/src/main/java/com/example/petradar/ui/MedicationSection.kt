package com.example.petradar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petradar.utils.MedicationReminder

/**
 * Section embedded inside [PetDetailContent] that lets the user manage
 * medication reminders for a pet.
 *
 * Each reminder stores:
 *  - Medicine name
 *  - Time of day (hour : minute)
 *  - Frequency (every N hours or every N days)
 *  - Optional notes
 *
 * Changes are propagated upward via [onRemindersChanged] so the caller
 * (PetDetailContent) can persist them when the pet is saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationSection(
    reminders: List<MedicationReminder>,
    onRemindersChanged: (List<MedicationReminder>) -> Unit,
    isLoading: Boolean = false,
    visible: Boolean = true
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<MedicationReminder?>(null) }

    if (showAddDialog || editingReminder != null) {
        MedicationDialog(
            initial = editingReminder,
            onDismiss = { },
            onConfirm = { reminder ->
                val updated = if (editingReminder != null) {
                    reminders.map { if (it.id == reminder.id) reminder else it }
                } else {
                    reminders + reminder
                }
                onRemindersChanged(updated)
                editingReminder = null
            }
        )
    }

    AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { 40 }) {
        Text(
            "Medicamentos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }

    AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { 40 }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (reminders.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Sin medicamentos registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                } else {
                    reminders.forEach { reminder ->
                        MedicationReminderRow(
                            reminder = reminder,
                            onToggleActive = {
                                onRemindersChanged(
                                    reminders.map { r ->
                                        if (r.id == reminder.id) r.copy(isActive = !r.isActive) else r
                                    }
                                )
                            },
                            onEdit = { editingReminder = reminder },
                            onDelete = {
                                onRemindersChanged(reminders.filter { it.id != reminder.id })
                            }
                        )
                        if (reminders.last() != reminder) HorizontalDivider()
                    }
                }

                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar medicamento")
                }
            }
        }
    }
}

@Composable
private fun MedicationReminderRow(
    reminder: MedicationReminder,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar recordatorio") },
            text = { Text("¿Eliminar el recordatorio de \"${reminder.medicineName}\"?") },
            confirmButton = {
                TextButton(onClick = { ; onDelete() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancelar") }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Medication,
            contentDescription = null,
            tint = if (reminder.isActive) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                reminder.medicineName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (reminder.isActive) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            val freqLabel = when (reminder.frequencyType) {
                "hours" -> "Cada ${reminder.frequencyValue} hora${if (reminder.frequencyValue > 1) "s" else ""}"
                else    -> "Cada ${reminder.frequencyValue} día${if (reminder.frequencyValue > 1) "s" else ""}"
            }
            val timeLabel = "%02d:%02d".format(reminder.hour, reminder.minute)
            Text(
                "$timeLabel · $freqLabel" + if (reminder.notes.isNotBlank()) " · ${reminder.notes}" else "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        // Active toggle
        Switch(
            checked = reminder.isActive,
            onCheckedChange = { onToggleActive() },
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.Delete, null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp))
        }
    }
}

/** Dialog for adding or editing a single [MedicationReminder]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationDialog(
    initial: MedicationReminder?,
    onDismiss: () -> Unit,
    onConfirm: (MedicationReminder) -> Unit
) {
    var medicineName by remember { mutableStateOf(initial?.medicineName ?: "") }
    var notes        by remember { mutableStateOf(initial?.notes ?: "") }
    var hour         by remember { mutableStateOf(initial?.hour ?: 8) }
    var minute       by remember { mutableStateOf(initial?.minute ?: 0) }
    var freqType     by remember { mutableStateOf(initial?.frequencyType ?: "days") }
    var freqValue    by remember { mutableStateOf(initial?.frequencyValue?.toString() ?: "1") }

    var nameError    by remember { mutableStateOf(false) }
    var freqError    by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = hour, initialMinute = minute, is24Hour = true
    )
    var showTimePicker by remember { mutableStateOf(false) }
    var freqTypeExpanded by remember { mutableStateOf(false) }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Hora del recordatorio") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancelar") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Medication, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(if (initial == null) "Nuevo medicamento" else "Editar medicamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it; nameError = false },
                    label = { Text("Nombre del medicamento *") },
                    leadingIcon = { Icon(Icons.Default.Medication, null) },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("Nombre requerido") }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Time picker field
                OutlinedTextField(
                    value = "%02d:%02d".format(hour, minute),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hora de la dosis *") },
                    leadingIcon = { Icon(Icons.Default.Schedule, null) },
                    trailingIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.AccessTime, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = freqValue,
                        onValueChange = { freqValue = it.filter { c -> c.isDigit() }; freqError = false },
                        label = { Text("Cada") },
                        isError = freqError,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        expanded = freqTypeExpanded,
                        onExpandedChange = { freqTypeExpanded = it },
                        modifier = Modifier.weight(1.4f)
                    ) {
                        OutlinedTextField(
                            value = if (freqType == "hours") "hora(s)" else "día(s)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(freqTypeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(
                                ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = freqTypeExpanded,
                            onDismissRequest = { freqTypeExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("hora(s)") }, onClick = {
                                freqType = "hours"; freqTypeExpanded = false
                            })
                            DropdownMenuItem(text = { Text("día(s)") }, onClick = {
                                freqType = "days"; freqTypeExpanded = false
                            })
                        }
                    }
                }
                if (freqError) {
                    Text("Ingresa un valor válido (ej: 1, 8, 12)", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                nameError  = medicineName.isBlank()
                freqError  = freqValue.toIntOrNull()?.let { it < 1 } ?: true
                if (nameError || freqError) return@Button
                onConfirm(
                    MedicationReminder(
                        id            = initial?.id ?: System.currentTimeMillis(),
                        medicineName  = medicineName.trim(),
                        hour          = hour,
                        minute        = minute,
                        frequencyType = freqType,
                        frequencyValue = freqValue.toInt(),
                        notes         = notes.trim(),
                        isActive      = initial?.isActive ?: true
                    )
                )
            }) { Text(if (initial == null) "Agregar" else "Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

