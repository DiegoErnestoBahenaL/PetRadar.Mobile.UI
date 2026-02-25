package com.example.petradar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.api.VeterinaryAppointmentCreateModel
import com.example.petradar.api.VeterinaryAppointmentUpdateModel
import com.example.petradar.viewmodel.AppointmentViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentFormScreen(
    viewModel: AppointmentViewModel,
    isEditMode: Boolean,
    petId: Long,
    userId: Long,
    initialDate: LocalDate? = null,
    onBack: () -> Unit
) {
    val selected by viewModel.selected.observeAsState()
    val isLoading = viewModel.isLoading.observeAsState(false).value
    val error by viewModel.error.observeAsState()
    val saveSuccess = viewModel.saveSuccess.observeAsState(false).value
    val userPets by viewModel.userPets.observeAsState(emptyList())

    var selectedPet by remember { mutableStateOf<UserPetViewModel?>(null) }
    var petExpanded by remember { mutableStateOf(false) }
    var petError by remember { mutableStateOf<String?>(null) }

    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = (initialDate ?: LocalDate.now())
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = true
    )

    var vetName by remember { mutableStateOf("") }
    var typeLabel by remember { mutableStateOf("") }
    var typeValue by remember { mutableStateOf("") }
    var statusLabel by remember { mutableStateOf("Programada") }
    var statusValue by remember { mutableStateOf("Scheduled") }
    var duration by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var prescriptions by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var typeError by remember { mutableStateOf<String?>(null) }
    var reasonError by remember { mutableStateOf<String?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val typeOptions = listOf(
        "Revisión" to "Checkup", "Vacuna" to "Vaccination", "Cirugía" to "Surgery",
        "Estética" to "Grooming", "Consulta" to "Consultation", "Otra" to "Other"
    )
    val statusOptions = listOf("Programada" to "Scheduled", "Cancelada" to "Cancelled")

    val dateDisplay = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale("es")))
            .replaceFirstChar { it.uppercase() }
    }
    val timeDisplay = remember(selectedTime) {
        selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    LaunchedEffect(Unit) { visible = true; viewModel.loadPetsByUser(userId) }

    LaunchedEffect(userPets) {
        if (userPets.isEmpty() || selectedPet != null) return@LaunchedEffect
        val targetId = if (petId > 0) petId else selected?.petId ?: -1L
        if (targetId > 0) selectedPet = userPets.find { it.id == targetId }
    }

    LaunchedEffect(selected) {
        val a = selected ?: return@LaunchedEffect
        if (selectedPet == null) selectedPet = userPets.find { it.id == a.petId }
        vetName = a.veterinaryName ?: ""
        notes = a.notes ?: ""
        diagnosis = a.diagnosis ?: ""
        treatment = a.treatment ?: ""
        prescriptions = a.prescriptions ?: ""
        cost = a.cost?.toString() ?: ""
        address = a.addressText ?: ""
        duration = a.durationInMinutes?.toString() ?: ""
        reason = a.reasonForVisit ?: ""
        val foundType = typeOptions.find { (_, v) -> v == a.appointmentType }
        typeLabel = foundType?.first ?: a.appointmentType ?: ""
        typeValue = foundType?.second ?: a.appointmentType ?: ""
        val foundStatus = statusOptions.find { (_, v) -> v == a.appointmentStatus }
        statusLabel = foundStatus?.first ?: a.appointmentStatus ?: ""
        statusValue = foundStatus?.second ?: a.appointmentStatus ?: ""
        runCatching {
            val dt = LocalDateTime.parse(a.appointmentDate, DateTimeFormatter.ISO_DATE_TIME)
            selectedDate = dt.toLocalDate()
            selectedTime = dt.toLocalTime()
        }
    }

    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(saveSuccess) { if (saveSuccess) { viewModel.clearSaveSuccess(); onBack() } }

    fun buildIsoDateTime() =
        LocalDateTime.of(selectedDate, selectedTime)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    fun validate(): Boolean {
        var ok = true
        if (selectedPet == null) { petError = "Selecciona una mascota"; ok = false }
        if (typeValue.isBlank()) { typeError = "Selecciona el tipo"; ok = false }
        if (reason.isBlank()) { reasonError = "Motivo requerido"; ok = false }
        return ok
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Selecciona la fecha", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                headline = null,
                showModeToggle = false
            )
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Selecciona la hora") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Editar Cita" else "Nueva Cita", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }) {
                Text("Datos de la cita", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 80)) + slideInVertically(tween(400, 80)) { 40 }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        ExposedDropdownMenuBox(expanded = petExpanded, onExpandedChange = { petExpanded = it }) {
                            OutlinedTextField(
                                value = selectedPet?.let { p ->
                                    val emoji = if (p.species?.lowercase() == "cat") "🐱" else "🐶"
                                    "$emoji ${p.name ?: "Sin nombre"}"
                                } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mascota *") },
                                leadingIcon = { Icon(Icons.Default.Face, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(petExpanded) },
                                placeholder = { Text(if (userPets.isEmpty()) "Cargando mascotas…" else "Selecciona una mascota") },
                                isError = petError != null,
                                supportingText = petError?.let { e -> { Text(e) } },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                enabled = !isLoading && userPets.isNotEmpty()
                            )
                            ExposedDropdownMenu(expanded = petExpanded, onDismissRequest = { petExpanded = false }) {
                                if (userPets.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No tienes mascotas registradas") }, onClick = { petExpanded = false }, enabled = false)
                                } else {
                                    userPets.forEach { pet ->
                                        val emoji = if (pet.species?.lowercase() == "cat") "🐱" else "🐶"
                                        val breed = pet.breed?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("$emoji ${pet.name ?: "Sin nombre"}", fontWeight = FontWeight.SemiBold)
                                                    Text("${pet.species ?: ""}$breed", style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                }
                                            },
                                            onClick = { selectedPet = pet; petError = null; petExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                            OutlinedTextField(
                                value = typeLabel, onValueChange = {}, readOnly = true,
                                label = { Text("Tipo de cita *") },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                                isError = typeError != null,
                                supportingText = typeError?.let { e -> { Text(e) } },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                enabled = !isLoading
                            )
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                typeOptions.forEach { (label, value) ->
                                    DropdownMenuItem(text = { Text(label) },
                                        onClick = { typeLabel = label; typeValue = value; typeExpanded = false; typeError = null })
                                }
                            }
                        }

                        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                            OutlinedTextField(
                                value = statusLabel, onValueChange = {}, readOnly = true,
                                label = { Text("Estado *") },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                enabled = !isLoading
                            )
                            ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                statusOptions.forEach { (label, value) ->
                                    DropdownMenuItem(text = { Text(label) },
                                        onClick = { statusLabel = label; statusValue = value; statusExpanded = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = dateDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha *") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            trailingIcon = {
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.EditCalendar, contentDescription = "Seleccionar fecha")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = timeDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hora *") },
                            leadingIcon = { Icon(Icons.Default.Schedule, null) },
                            trailingIcon = {
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Seleccionar hora")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = reason, onValueChange = { reason = it; reasonError = null },
                            label = { Text("Motivo de visita *") },
                            leadingIcon = { Icon(Icons.Default.NoteAlt, null) },
                            isError = reasonError != null,
                            supportingText = reasonError?.let { e -> { Text(e) } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = vetName, onValueChange = { vetName = it },
                            label = { Text("Veterinario") },
                            leadingIcon = { Icon(Icons.Default.LocalHospital, null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = duration, onValueChange = { duration = it },
                                label = { Text("Duración (min)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading, singleLine = true
                            )
                            OutlinedTextField(
                                value = cost, onValueChange = { cost = it },
                                label = { Text("Costo") },
                                leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading, singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = address, onValueChange = { address = it },
                            label = { Text("Dirección / Clínica") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                    }
                }
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 160)) + slideInVertically(tween(400, 160)) { 40 }) {
                Text("Notas clínicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 200)) + slideInVertically(tween(400, 200)) { 40 }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !isLoading)
                        OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text("Diagnóstico") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !isLoading)
                        OutlinedTextField(value = treatment, onValueChange = { treatment = it }, label = { Text("Tratamiento") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !isLoading)
                        OutlinedTextField(value = prescriptions, onValueChange = { prescriptions = it }, label = { Text("Prescripciones") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !isLoading)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 280)) + slideInVertically(tween(400, 280)) { 60 }) {
                Button(
                    onClick = {
                        if (!validate()) return@Button
                        val isoDate = buildIsoDateTime()
                        val chosenPetId = selectedPet!!.id
                        if (isEditMode && selected != null) {
                            viewModel.update(
                                selected!!.id,
                                VeterinaryAppointmentUpdateModel(
                                    veterinaryName = vetName.trim().ifEmpty { null },
                                    appointmentType = typeValue,
                                    appointmentStatus = statusValue,
                                    appointmentDate = isoDate,
                                    durationInMinutes = duration.trim().toIntOrNull(),
                                    reasonForVisit = reason.trim(),
                                    notes = notes.trim().ifEmpty { null },
                                    diagnosis = diagnosis.trim().ifEmpty { null },
                                    treatment = treatment.trim().ifEmpty { null },
                                    prescriptions = prescriptions.trim().ifEmpty { null },
                                    cost = cost.trim().toDoubleOrNull(),
                                    addressText = address.trim().ifEmpty { null }
                                ),
                                userId
                            )
                        } else {
                            viewModel.create(
                                VeterinaryAppointmentCreateModel(
                                    petId = chosenPetId,
                                    veterinaryName = vetName.trim().ifEmpty { null },
                                    appointmentType = typeValue,
                                    appointmentStatus = statusValue,
                                    appointmentDate = isoDate,
                                    durationInMinutes = duration.trim().toIntOrNull(),
                                    reasonForVisit = reason.trim(),
                                    notes = notes.trim().ifEmpty { null },
                                    diagnosis = diagnosis.trim().ifEmpty { null },
                                    treatment = treatment.trim().ifEmpty { null },
                                    prescriptions = prescriptions.trim().ifEmpty { null },
                                    cost = cost.trim().toDoubleOrNull(),
                                    addressText = address.trim().ifEmpty { null }
                                ),
                                userId
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEditMode) "Actualizar Cita" else "Registrar Cita", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
