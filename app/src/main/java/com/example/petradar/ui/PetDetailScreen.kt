package com.example.petradar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.utils.MedicationReminder
import com.example.petradar.utils.MedicationStore
import com.example.petradar.utils.PetImageUrlResolver
import com.example.petradar.utils.PetPhotoStore
import com.example.petradar.viewmodel.PetDetailViewModel
import androidx.core.net.toUri
import java.io.File
import androidx.compose.ui.tooling.preview.Preview
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.api.UserPetViewModel

@Composable
/**
 * Form for creating or editing a pet.
 *
 * In **edit mode** ([isEditMode] = true):
 *  - Pre-fills fields with data loaded in [PetDetailViewModel.pet].
 *  - Shows the photo from the API endpoint GET /api/UserPets/{id}/mainpicture if available.
 *  - On save calls PUT /api/UserPets/{id}.
 *
 * In **create mode** ([isEditMode] = false):
 *  - All fields start empty.
 *  - On save calls POST /api/UserPets.
 *
 * Form fields:
 *  - Name (required), Species (required), Sex, Size, Breed, Color,
 *    Date of birth, Weight, Description, Allergies, Medical notes, Neutered.
 *
 * Pet photo:
 *  - The user can pick an image from the gallery or camera.
 *  - The photo is uploaded to the API via PUT /api/UserPets/{id}/mainpicture
 *    and is fetched from GET /api/UserPets/{id}/mainpicture.
 *
 * When [PetDetailViewModel.saveSuccess] is true, closes the screen.
 *
 * @param viewModel  ViewModel with the pet data and CRUD operations.
 * @param isEditMode true = editing an existing pet; false = creating a new one.
 * @param onBack     Callback to close the form.
 */
fun PetDetailScreen(
    viewModel: PetDetailViewModel,
    isEditMode: Boolean,
    onBack: () -> Unit,
    onSaveMedications: ((List<MedicationReminder>) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReportLost: (() -> Unit)? = null
) {
    val petData by viewModel.pet.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val saveSuccess by viewModel.saveSuccess.observeAsState(false)
    val existingAdditionalPhotos by viewModel.additionalPhotos.observeAsState(emptyList())

    PetDetailContent(
        petData = petData,
        isLoading = isLoading,
        errorMessage = errorMessage,
        saveSuccess = saveSuccess,
        isEditMode = isEditMode,
        currentPetId = viewModel.currentPetId,
        existingAdditionalPhotos = existingAdditionalPhotos,
        onDeleteAdditionalPhoto = { photoName -> viewModel.deleteAdditionalPhoto(viewModel.currentPetId, photoName) },
        onBack = onBack,
        onDelete = if (isEditMode && onDelete != null) onDelete else null,
        onReportLost = if (isEditMode && onReportLost != null) onReportLost else null,
        onSaveMedications = onSaveMedications,
        onSave = { name, speciesValue, breed, color, sexValue, sizeValue, birthDate, weight, description, isNeutered, allergies, medicalNotes, photoUri, additionalPhotoUris, context ->
            viewModel.savePet(
                name = name,
                speciesValue = speciesValue,
                breed = breed,
                color = color,
                sexValue = sexValue,
                sizeValue = sizeValue,
                birthDate = birthDate,
                weight = weight,
                description = description,
                isNeutered = isNeutered,
                allergies = allergies,
                medicalNotes = medicalNotes,
                photoUri = photoUri,
                additionalPhotoUris = additionalPhotoUris,
                context = context
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailContent(
    petData: UserPetViewModel?,
    isLoading: Boolean,
    errorMessage: String?,
    saveSuccess: Boolean,
    isEditMode: Boolean,
    currentPetId: Long,
    existingAdditionalPhotos: List<String> = emptyList(),
    onDeleteAdditionalPhoto: ((String) -> Unit)? = null,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onReportLost: (() -> Unit)? = null,
    onSaveMedications: ((List<MedicationReminder>) -> Unit)? = null,
    onSave: (
        name: String, speciesValue: String, breed: String?, color: String?,
        sexValue: String?, sizeValue: String?, birthDate: String?,
        weight: Double?, description: String?, isNeutered: Boolean,
        allergies: String?, medicalNotes: String?,
        photoUri: String?,
        additionalPhotoUris: List<Uri>,
        context: android.content.Context
    ) -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val snackbarHostState = remember { SnackbarHostState() }
    var visible by remember { mutableStateOf(isInPreview) }

    var photoUri by remember {
        mutableStateOf<Uri?>(null)
    }
    // Temporary file URI for camera captures
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var pendingAdditionalPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Activity result launchers are not available in preview mode.
    // Guard them with isInPreview to prevent render crashes.
    val galleryLauncher = if (!isInPreview) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? -> if (uri != null) photoUri = uri }
    } else null

    val additionalGalleryLauncher = if (!isInPreview) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                val totalAllowed = (5 - existingAdditionalPhotos.size - pendingAdditionalPhotos.size).coerceAtLeast(0)
                pendingAdditionalPhotos = pendingAdditionalPhotos + uris.take(totalAllowed)
            }
        }
    } else null

    val cameraLauncher = if (!isInPreview) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success: Boolean ->
            if (success && cameraImageUri != null) {
                photoUri = cameraImageUri
            }
        }
    } else null

    val cameraPermissionLauncher = if (!isInPreview) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                val photoFile = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                    .let { File(it, "pet_${System.currentTimeMillis()}.jpg") }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                cameraImageUri = uri
                cameraLauncher?.launch(uri)
            }
        }
    } else null

    fun launchCamera() {
        if (isInPreview) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                .let { File(it, "pet_${System.currentTimeMillis()}.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraImageUri = uri
            cameraLauncher?.launch(uri)
        } else {
            cameraPermissionLauncher?.launch(Manifest.permission.CAMERA)
        }
    }

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
    var showPhotoRequiredDialog by remember { mutableStateOf(false) }
    var photoPromptDismissed by remember { mutableStateOf(false) }

    // ── Discard-changes guard ────────────────────────────────────────────
    var origName        by remember { mutableStateOf("") }
    var origSpecies     by remember { mutableStateOf("") }
    var origSex         by remember { mutableStateOf("") }
    var origSize        by remember { mutableStateOf("") }
    var origBreed       by remember { mutableStateOf("") }
    var origColor       by remember { mutableStateOf("") }
    var origBirthDate   by remember { mutableStateOf("") }
    var origWeight      by remember { mutableStateOf("") }
    var origDescription by remember { mutableStateOf("") }
    var origNeutered    by remember { mutableStateOf(false) }
    var origAllergies   by remember { mutableStateOf("") }
    var origMedNotes    by remember { mutableStateOf("") }
    var formInitialized by remember { mutableStateOf(!isEditMode) }

    val hasChanges = formInitialized && (
        petName        != origName        ||
        speciesValue   != origSpecies     ||
        sexValue       != origSex         ||
        sizeValue      != origSize        ||
        petBreed       != origBreed       ||
        petColor       != origColor       ||
        birthDate      != origBirthDate   ||
        petWeight      != origWeight      ||
        petDescription != origDescription ||
        isNeutered     != origNeutered    ||
        petAllergies   != origAllergies   ||
        medicalNotes   != origMedNotes    ||
        photoUri       != null            ||
        pendingAdditionalPhotos.isNotEmpty()
    )

    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = hasChanges) { showDiscardDialog = true }

    var speciesExpanded by remember { mutableStateOf(false) }
    var sexExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }

    // Medication reminders – loaded from local store in edit mode
    var medicationReminders by remember {
        mutableStateOf<List<MedicationReminder>>(
            if (!isInPreview && isEditMode && currentPetId > 0)
                MedicationStore.getAll(context, currentPetId)
            else emptyList()
        )
    }

    val speciesOptions = listOf("Perro" to "Dog", "Gato" to "Cat")
    val sexOptions = listOf("Macho" to "Male", "Hembra" to "Female", "Desconocido" to "Unknown")
    val sizeOptions = listOf("Pequeño" to "Small", "Mediano" to "Medium", "Grande" to "Large")

    val pictureUrl = if (currentPetId > 0) {
        PetImageUrlResolver.mainPictureEndpoint(currentPetId)
    } else null

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

        if (!formInitialized) {
            origName        = petName
            origSpecies     = speciesValue
            origSex         = sexValue
            origSize        = sizeValue
            origBreed       = petBreed
            origColor       = petColor
            origBirthDate   = birthDate
            origWeight      = petWeight
            origDescription = petDescription
            origNeutered    = isNeutered
            origAllergies   = petAllergies
            origMedNotes    = medicalNotes
            formInitialized = true
        }
    }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess && photoUri != null && currentPetId > 0) {
            photoUri = null
        }
        if (saveSuccess) onBack()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Tienes cambios sin guardar. ¿Deseas descartarlos?") },
            confirmButton = {
                TextButton(
                    onClick = { showDiscardDialog = false; onBack() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Seguir editando") }
            }
        )
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Agregar foto") },
            text = { Text("¿Cómo deseas agregar la foto?") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    launchCamera()
                }) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    galleryLauncher?.launch("image/*")
                }) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Galería")
                }
            }
        )
    }

    if (showPhotoRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoRequiredDialog = false },
            icon = { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Agregar foto principal") },
            text = { Text("Tu mascota no tiene foto principal. Agregar una ayuda a identificarla más fácilmente.") },
            confirmButton = {
                TextButton(onClick = { showPhotoRequiredDialog = false; showPhotoSourceDialog = true }) {
                    Text("Agregar foto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoRequiredDialog = false; photoPromptDismissed = true }) {
                    Text("Continuar sin foto")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Eliminar mascota") },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar a ${petData?.name ?: "esta mascota"}? " +
                    "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isLoading
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Editar Mascota" else "Nueva Mascota", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { if (hasChanges) showDiscardDialog = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (isEditMode && onDelete != null) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar mascota",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            AnimatedVisibility(visible, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -30 }) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                            .clickable { showPhotoSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        // Determine which image to display:
                        // 1. If photoUri is set (locally selected but not yet uploaded), show that
                        // 2. Otherwise, use the deterministic /mainpicture endpoint (auth-aware)
                        // 3. Fall back to local store
                        val displayImageUrl = when {
                            photoUri != null -> photoUri.toString()
                            !pictureUrl.isNullOrBlank() -> pictureUrl
                            currentPetId > 0 -> PetPhotoStore.get(context, currentPetId)
                            else -> null
                        }

                        if (!displayImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(displayImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto de mascota",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Agregar foto", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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

            // ── Additional Photos (right below main photo) ─────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 50)) + slideInVertically(tween(400, 50)) { 30 }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val totalPhotos = existingAdditionalPhotos.size + pendingAdditionalPhotos.size
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Fotos adicionales ($totalPhotos/5)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (existingAdditionalPhotos.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(existingAdditionalPhotos) { photoName ->
                                    val url = com.example.petradar.utils.PetImageUrlResolver.petAdditionalPhotoUrl(currentPetId, photoName)
                                    Box(modifier = Modifier.size(80.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                                            contentDescription = null, contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                        )
                                        IconButton(onClick = { onDeleteAdditionalPhoto?.invoke(photoName) }, modifier = Modifier.align(Alignment.TopEnd).size(26.dp)) {
                                            Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                        if (pendingAdditionalPhotos.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(pendingAdditionalPhotos) { uri ->
                                    Box(modifier = Modifier.size(80.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                                            contentDescription = null, contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                        )
                                        IconButton(onClick = { pendingAdditionalPhotos = pendingAdditionalPhotos - uri }, modifier = Modifier.align(Alignment.TopEnd).size(26.dp)) {
                                            Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { additionalGalleryLauncher?.launch("image/*") },
                            enabled = !isLoading && totalPhotos < 5,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (totalPhotos >= 5) "Límite de 5 fotos alcanzado" else "Agregar fotos adicionales")
                        }
                    }
                }
            }
            // ── End Additional Photos ──────────────────────────────────────

            AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }) {
                Text("Información Básica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 80)) + slideInVertically(tween(400, 80)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = petName, onValueChange = { petName = it; nameError = null },
                            label = { Text("Nombre *") },
                            leadingIcon = { Icon(Icons.Default.Star, null) },
                            isError = nameError != null,
                            supportingText = nameError?.let { err -> { Text(err) } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                        ExposedDropdownMenuBox(expanded = speciesExpanded, onExpandedChange = { speciesExpanded = it }) {
                            OutlinedTextField(
                                value = speciesLabel, onValueChange = {}, readOnly = true,
                                label = { Text("Especie *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                                isError = speciesError != null,
                                supportingText = speciesError?.let { err -> { Text(err) } },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = petColor, onValueChange = { petColor = it }, label = { Text("Color") },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        ExposedDropdownMenuBox(expanded = sexExpanded, onExpandedChange = { sexExpanded = it }) {
                            OutlinedTextField(value = sexLabel, onValueChange = {}, readOnly = true, label = { Text("Sexo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sexExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable), enabled = !isLoading)
                            ExposedDropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                                sexOptions.forEach { (label, value) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { sexLabel = label; sexValue = value; sexExpanded = false })
                                }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = sizeExpanded, onExpandedChange = { sizeExpanded = it }) {
                            OutlinedTextField(value = sizeLabel, onValueChange = {}, readOnly = true, label = { Text("Tamaño") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sizeExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable), enabled = !isLoading)
                            ExposedDropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                                sizeOptions.forEach { (label, value) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { sizeLabel = label; sizeValue = value; sizeExpanded = false })
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 150)) + slideInVertically(tween(400, 150)) { 40 }) {
                Text("Detalles Adicionales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 200)) + slideInVertically(tween(400, 200)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = birthDate, onValueChange = { birthDate = it },
                            label = { Text("Fecha de nacimiento (YYYY-MM-DD)") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = petWeight, onValueChange = { petWeight = it }, label = { Text("Peso (kg)") },
                            leadingIcon = { Icon(Icons.Default.MonitorWeight, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = petDescription, onValueChange = { petDescription = it }, label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !isLoading)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isNeutered, onCheckedChange = { isNeutered = it }, enabled = !isLoading)
                            Spacer(Modifier.width(8.dp))
                            Text("Esterilizado/Castrado")
                        }
                    }
                }
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 250)) + slideInVertically(tween(400, 250)) { 40 }) {
                Text("Información Médica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 300)) + slideInVertically(tween(400, 300)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = petAllergies, onValueChange = { petAllergies = it }, label = { Text("Alergias") },
                            leadingIcon = { Icon(Icons.Default.MedicalServices, null) },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = medicalNotes, onValueChange = { medicalNotes = it }, label = { Text("Notas médicas") },
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !isLoading)
                    }
                }
            }

            MedicationSection(
                reminders = medicationReminders,
                onRemindersChanged = { medicationReminders = it },
                isLoading = isLoading,
                visible = visible
            )

            Spacer(Modifier.height(8.dp))

            if (isEditMode && onReportLost != null) {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 340)) + slideInVertically(tween(400, 340)) { 60 }) {
                    OutlinedButton(
                        onClick = onReportLost,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Report, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Reportar como perdida", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 350)) + slideInVertically(tween(400, 350)) { 60 }) {
                Button(
                    onClick = {
                        if (petName.isBlank()) { nameError = "El nombre es requerido"; return@Button }
                        if (speciesValue.isBlank()) { speciesError = "Selecciona la especie"; return@Button }

                        // For new pets, prompt for a main photo if none was selected (only once)
                        if (!isEditMode && photoUri == null && !photoPromptDismissed) {
                            showPhotoRequiredDialog = true
                            return@Button
                        }

                        if (photoUri != null) {
                            if (currentPetId > 0) PetPhotoStore.save(context, currentPetId, photoUri.toString())
                        }

                        // Persist medication reminders locally before the API call
                        if (currentPetId > 0) {
                            onSaveMedications?.invoke(medicationReminders)
                        }

                        onSave(
                            petName.trim(), speciesValue,
                            petBreed.trim().ifEmpty { null }, petColor.trim().ifEmpty { null },
                            sexValue.ifEmpty { null }, sizeValue.ifEmpty { null },
                            birthDate.trim().ifEmpty { null }, petWeight.trim().toDoubleOrNull(),
                            petDescription.trim().ifEmpty { null }, isNeutered,
                            petAllergies.trim().ifEmpty { null },
                            medicalNotes.trim().ifEmpty { null },
                            photoUri?.toString(),
                            pendingAdditionalPhotos,
                            context
                        )
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
                        Text(if (isEditMode) "Actualizar Mascota" else "Guardar Mascota", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PetDetailScreenCreatePreview() {
    PetRadarTheme {
        PetDetailContent(
            petData = null,
            isLoading = false,
            errorMessage = null,
            saveSuccess = false,
            isEditMode = false,
            currentPetId = -1L,
            onBack = {},
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PetDetailScreenEditPreview() {
    val samplePet = UserPetViewModel(
        id = 1L,
        userId = 1L,
        name = "Luna",
        species = "Dog",
        breed = "Golden Retriever",
        color = "Dorado",
        sex = "Female",
        size = "Large",
        birthDate = "2020-05-15",
        approximateAge = 3.5,
        weight = 25.0,
        description = "Una perra muy amigable.",
        photoURL = null,
        additionalPhotosURL = null,
        isNeutered = true,
        allergies = "Ninguna",
        medicalNotes = "Vacunas al día"
    )
    PetRadarTheme {
        PetDetailContent(
            petData = samplePet,
            isLoading = false,
            errorMessage = null,
            saveSuccess = false,
            isEditMode = true,
            currentPetId = 1L,
            onBack = {},
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}
