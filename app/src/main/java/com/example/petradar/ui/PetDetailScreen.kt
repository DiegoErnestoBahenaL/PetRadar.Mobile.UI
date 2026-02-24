package com.example.petradar.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.ui.theme.PetTeal40
import com.example.petradar.utils.PetPhotoStore
import com.example.petradar.viewmodel.PetDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    viewModel: PetDetailViewModel,
    isEditMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val petData by viewModel.pet.observeAsState()
    val isLoadingRaw by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingRaw ?: false
    val errorMessage by viewModel.errorMessage.observeAsState()
    val saveSuccessRaw by viewModel.saveSuccess.observeAsState(false)
    val saveSuccess = saveSuccessRaw ?: false

    val snackbarHostState = remember { SnackbarHostState() }
    var visible by remember { mutableStateOf(false) }

    // ── Photo state ───────────────────────────────────────────────
    // Load existing stored URI when editing
    var photoUri by remember {
        mutableStateOf<Uri?>(
            if (isEditMode && viewModel.currentPetId > 0)
                PetPhotoStore.get(context, viewModel.currentPetId)?.let { Uri.parse(it) }
            else null
        )
    }

    // Gallery launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) photoUri = uri }

    // ── Form fields ───────────────────────────────────────────────
    var petName by remember { mutableStateOf("") }
    var speciesLabel by remember { mutableStateOf("") }
    var speciesValue by remember { mutableStateOf("") }
    var sexLabel by remember { mutableStateOf("") }
    var sexValue by remember { mutableStateOf("") }
    var sizeLabel by remember { mutableStateOf("") }
    var sizeValue by remember { mutableStateOf("") }
    var petBreed by remember { mutableStateOf("") }
    var petColor by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var petWeight by remember { mutableStateOf("") }
    var petDescription by remember { mutableStateOf("") }
    var isNeutered by remember { mutableStateOf(false) }
    var petAllergies by remember { mutableStateOf("") }
    var medicalNotes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var speciesError by remember { mutableStateOf<String?>(null) }

    var speciesExpanded by remember { mutableStateOf(false) }
    var sexExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }

    val speciesOptions = listOf("Perro" to "Dog", "Gato" to "Cat")
    val sexOptions = listOf("Macho" to "Male", "Hembra" to "Female", "Desconocido" to "Unknown")
    val sizeOptions = listOf("Pequeño" to "Small", "Mediano" to "Medium", "Grande" to "Large")

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(petData) {
        val p = petData ?: return@LaunchedEffect
        petName = p.name ?: ""
        petBreed = p.breed ?: ""
        petColor = p.color ?: ""
        birthDate = p.birthDate?.take(10) ?: ""
        petWeight = p.weight?.toString() ?: ""
        petDescription = p.description ?: ""
        isNeutered = p.isNeutered ?: false
        petAllergies = p.allergies ?: ""
        medicalNotes = p.medicalNotes ?: ""

        val foundSpecies = speciesOptions.find { (_, v) -> v.equals(p.species, ignoreCase = true) }
        speciesLabel = foundSpecies?.first ?: p.species ?: ""
        speciesValue = foundSpecies?.second ?: p.species ?: ""

        val foundSex = sexOptions.find { (_, v) -> v.equals(p.sex, ignoreCase = true) }
        sexLabel = foundSex?.first ?: p.sex ?: ""
        sexValue = foundSex?.second ?: p.sex ?: ""

        val foundSize = sizeOptions.find { (_, v) -> v.equals(p.size, ignoreCase = true) }
        sizeLabel = foundSize?.first ?: p.size ?: ""
        sizeValue = foundSize?.second ?: p.size ?: ""

        // Load photo from local store (fallback to API photoURL)
        if (photoUri == null) {
            val stored = PetPhotoStore.get(context, p.id)
            photoUri = when {
                stored != null -> Uri.parse(stored)
                !p.photoURL.isNullOrBlank() -> Uri.parse(p.photoURL)
                else -> null
            }
        }
    }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Editar Mascota" else "Nueva Mascota", fontWeight = FontWeight.Bold)
                },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── Photo picker ────────────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -30 }) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(PetTeal40.copy(alpha = 0.12f))
                            .border(2.dp, PetTeal40.copy(alpha = 0.4f), CircleShape)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto de mascota",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            // Pencil overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PetTeal40),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, null, tint = PetTeal40, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Agregar foto", fontSize = 11.sp, color = PetTeal40, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (photoUri != null) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { photoUri = null }) {
                            Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Quitar foto", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ─── Información Básica ───────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }) {
                Text("Información Básica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 80)) + slideInVertically(tween(400, 80)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = petName, onValueChange = { petName = it; nameError = null },
                            label = { Text("Nombre *") },
                            leadingIcon = { Icon(Icons.Default.Star, null) },
                            isError = nameError != null,
                            supportingText = nameError?.let { err -> { Text(err) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40),
                            enabled = !isLoading
                        )
                        ExposedDropdownMenuBox(expanded = speciesExpanded, onExpandedChange = { speciesExpanded = it }) {
                            OutlinedTextField(
                                value = speciesLabel, onValueChange = {}, readOnly = true,
                                label = { Text("Especie *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                                isError = speciesError != null,
                                supportingText = speciesError?.let { err -> { Text(err) } },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40),
                                enabled = !isLoading
                            )
                            ExposedDropdownMenu(expanded = speciesExpanded, onDismissRequest = { speciesExpanded = false }) {
                                speciesOptions.forEach { (label, value) ->
                                    DropdownMenuItem(text = { Text(label) },
                                        onClick = { speciesLabel = label; speciesValue = value; speciesExpanded = false; speciesError = null })
                                }
                            }
                        }
                        OutlinedTextField(value = petBreed, onValueChange = { petBreed = it }, label = { Text("Raza") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                        OutlinedTextField(value = petColor, onValueChange = { petColor = it }, label = { Text("Color") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                        ExposedDropdownMenuBox(expanded = sexExpanded, onExpandedChange = { sexExpanded = it }) {
                            OutlinedTextField(value = sexLabel, onValueChange = {}, readOnly = true, label = { Text("Sexo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sexExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                            ExposedDropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                                sexOptions.forEach { (label, value) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { sexLabel = label; sexValue = value; sexExpanded = false })
                                }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = sizeExpanded, onExpandedChange = { sizeExpanded = it }) {
                            OutlinedTextField(value = sizeLabel, onValueChange = {}, readOnly = true, label = { Text("Tamaño") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sizeExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                            ExposedDropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                                sizeOptions.forEach { (label, value) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { sizeLabel = label; sizeValue = value; sizeExpanded = false })
                                }
                            }
                        }
                    }
                }
            }

            // ─── Detalles Adicionales ─────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 150)) + slideInVertically(tween(400, 150)) { 40 }) {
                Text("Detalles Adicionales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 200)) + slideInVertically(tween(400, 200)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = birthDate, onValueChange = { birthDate = it },
                            label = { Text("Fecha de nacimiento (YYYY-MM-DD)") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                        OutlinedTextField(value = petWeight, onValueChange = { petWeight = it }, label = { Text("Peso (kg)") },
                            leadingIcon = { Icon(Icons.Default.MonitorWeight, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                        OutlinedTextField(value = petDescription, onValueChange = { petDescription = it }, label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isNeutered, onCheckedChange = { isNeutered = it }, enabled = !isLoading,
                                colors = CheckboxDefaults.colors(checkedColor = PetTeal40))
                            Spacer(Modifier.width(8.dp))
                            Text("Esterilizado/Castrado")
                        }
                    }
                }
            }

            // ─── Información Médica ───────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 250)) + slideInVertically(tween(400, 250)) { 40 }) {
                Text("Información Médica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 300)) + slideInVertically(tween(400, 300)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = petAllergies, onValueChange = { petAllergies = it }, label = { Text("Alergias") },
                            leadingIcon = { Icon(Icons.Default.MedicalServices, null) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                        OutlinedTextField(value = medicalNotes, onValueChange = { medicalNotes = it }, label = { Text("Notas médicas") },
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            modifier = Modifier.fillMaxWidth(), minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PetTeal40, focusedLabelColor = PetTeal40), enabled = !isLoading)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 350)) + slideInVertically(tween(400, 350)) { 60 }) {
                Button(
                    onClick = {
                        if (petName.isBlank()) { nameError = "El nombre es requerido"; return@Button }
                        if (speciesValue.isBlank()) { speciesError = "Selecciona la especie"; return@Button }

                        // Persist photo URI locally before saving
                        val currentId = viewModel.currentPetId
                        if (photoUri != null) {
                            // For new pets we won't have an ID yet; save after API returns
                            if (currentId > 0) PetPhotoStore.save(context, currentId, photoUri.toString())
                        } else if (currentId > 0) {
                            PetPhotoStore.delete(context, currentId)
                        }

                        viewModel.savePet(
                            name = petName.trim(), speciesValue = speciesValue,
                            breed = petBreed.trim().ifEmpty { null }, color = petColor.trim().ifEmpty { null },
                            sexValue = sexValue.ifEmpty { null }, sizeValue = sizeValue.ifEmpty { null },
                            birthDate = birthDate.trim().ifEmpty { null }, weight = petWeight.trim().toDoubleOrNull(),
                            description = petDescription.trim().ifEmpty { null }, isNeutered = isNeutered,
                            allergies = petAllergies.trim().ifEmpty { null },
                            medicalNotes = medicalNotes.trim().ifEmpty { null },
                            photoUri = photoUri?.toString()
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PetTeal40),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEditMode) "Actualizar Mascota" else "Guardar Mascota", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
