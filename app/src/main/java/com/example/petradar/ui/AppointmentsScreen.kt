@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.petradar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.api.VeterinaryAppointmentViewModel
import com.example.petradar.ui.theme.*
import com.example.petradar.utils.PetPhotoStore
import com.example.petradar.viewmodel.AppointmentViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    viewModel: AppointmentViewModel, userId: Long,
    onAddAppointment: (LocalDate) -> Unit,
    onEditAppointment: (VeterinaryAppointmentViewModel) -> Unit,
    onBack: () -> Unit
) {
    val appointments by viewModel.appointments.observeAsState(emptyList())
    val isLoadingRaw by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingRaw ?: false
    val error by viewModel.error.observeAsState()
    val userPets by viewModel.userPets.observeAsState(emptyList())

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) { if (!isLoading) isRefreshing = false }

    LaunchedEffect(userId) { viewModel.loadPetsByUser(userId) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDeleteDialog by remember { mutableStateOf<VeterinaryAppointmentViewModel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        val msg = error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    // Parse dates from appointments for calendar markers
    val appointmentDates: Set<LocalDate> = remember(appointments) {
        appointments.mapNotNull { appt ->
            runCatching { LocalDateTime.parse(appt.appointmentDate, DateTimeFormatter.ISO_DATE_TIME).toLocalDate() }.getOrNull()
        }.toSet()
    }

    // Filter appointments for the selected date
    val selectedDayAppointments = remember(appointments, selectedDate) {
        appointments.filter { appt ->
            runCatching {
                LocalDateTime.parse(appt.appointmentDate, DateTimeFormatter.ISO_DATE_TIME).toLocalDate() == selectedDate
            }.getOrDefault(false)
        }.sortedBy { it.appointmentDate }
    }

    // Delete dialog
    showDeleteDialog?.let { appt ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar cita") },
            text = { Text("¿Eliminar la cita del ${formatDateShort(appt.appointmentDate)}?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(appt.id, userId); showDeleteDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Citas Veterinarias", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddAppointment(selectedDate) },
                containerColor = PetTeal40,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva cita")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadByUser(userId)
                viewModel.loadPetsByUser(userId)
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Calendar Card ────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PetTeal40)
                        }
                        Text(
                            text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es"))
                                .replaceFirstChar { it.uppercase() } + " ${currentMonth.year}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = PetTeal40)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Day-of-week headers
                    val dayNames = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do")
                    Row(Modifier.fillMaxWidth()) {
                        dayNames.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Calendar grid
                    val firstDay = currentMonth.atDay(1)
                    // Monday-based offset (1=Mon..7=Sun)
                    val startOffset = (firstDay.dayOfWeek.value - 1)
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val totalCells = startOffset + daysInMonth
                    val rows = (totalCells + 6) / 7

                    for (row in 0 until rows) {
                        Row(Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - startOffset + 1
                                if (dayNum < 1 || dayNum > daysInMonth) {
                                    Box(Modifier.weight(1f).height(40.dp))
                                } else {
                                    val date = currentMonth.atDay(dayNum)
                                    val isSelected = date == selectedDate
                                    val isToday = date == LocalDate.now()
                                    val hasAppts = date in appointmentDates

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> PetTeal40
                                                    isToday -> PetTeal40.copy(alpha = 0.15f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday && !isSelected)
                                                    Modifier.border(1.dp, PetTeal40, CircleShape)
                                                else Modifier
                                            )
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> PetTeal40
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            if (hasAppts) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else PetAccent)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Selected day label ───────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = PetTeal40, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es")))
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PetTeal40
                )
                if (selectedDayAppointments.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = PetAccent, contentColor = Color.Black) {
                        Text(selectedDayAppointments.size.toString(), fontSize = 11.sp)
                    }
                }
            }

            // ── Appointment list for selected day ────────────────────
            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PetTeal40)
                }
            } else if (selectedDayAppointments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sin citas este día",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDayAppointments, key = { it.id }) { appt ->
                        AppointmentCard(
                            appointment = appt,
                            pet = userPets.find { it.id == appt.petId },
                            onEdit = { onEditAppointment(appt) },
                            onDelete = { showDeleteDialog = appt }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }  // end Column
        }  // end PullToRefreshBox
    }
}

// ── Appointment Card ─────────────────────────────────────────────
@Composable
fun AppointmentCard(
    appointment: VeterinaryAppointmentViewModel,
    pet: UserPetViewModel? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val typeColor = when (appointment.appointmentType?.lowercase()) {
        "vaccination" -> Color(0xFF43A047)
        "surgery"     -> Color(0xFFE53935)
        "grooming"    -> Color(0xFF8E24AA)
        "checkup"     -> PetTeal40
        "consultation"-> Color(0xFF1E88E5)
        else          -> Color(0xFF757575)
    }
    val statusColor = if (appointment.appointmentStatus == "Cancelled")
        MaterialTheme.colorScheme.error else Color(0xFF43A047)
    val typeLabel = when (appointment.appointmentType) {
        "Checkup"      -> "Revisión"
        "Vaccination"  -> "Vacuna"
        "Surgery"      -> "Cirugía"
        "Grooming"     -> "Estética"
        "Consultation" -> "Consulta"
        else           -> appointment.appointmentType ?: "Otra"
    }

    // Resolve pet photo: local store first, then API URL, then emoji fallback
    val petPhotoUri = remember(pet?.id) {
        pet?.let { p ->
            PetPhotoStore.get(context, p.id) ?: p.photoURL?.takeIf { it.isNotBlank() }
        }
    }
    val petEmoji = if (pet?.species?.lowercase() == "cat") "🐱" else "🐶"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color strip
            Box(
                modifier = Modifier.width(4.dp).height(56.dp)
                    .clip(RoundedCornerShape(2.dp)).background(typeColor)
            )
            Spacer(Modifier.width(10.dp))

            // Pet avatar
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(PetTeal40.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (petPhotoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(petPhotoUri).crossfade(true).build(),
                        contentDescription = pet?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(petEmoji, fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Pet name + time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pet?.name ?: "Mascota",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimeOnly(appointment.appointmentDate),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = typeColor
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(typeLabel, fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = typeColor.copy(alpha = 0.12f),
                            labelColor = typeColor
                        ),
                        border = null,
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = appointment.appointmentStatus ?: "",
                        fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = appointment.reasonForVisit ?: "Sin motivo",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (!appointment.veterinaryName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, null, modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.width(3.dp))
                        Text(appointment.veterinaryName, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, null, tint = PetTeal40, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────
fun formatDateShort(iso: String): String = runCatching {
    LocalDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME)
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}.getOrDefault(iso)

fun formatTimeOnly(iso: String): String = runCatching {
    LocalDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("--:--")

