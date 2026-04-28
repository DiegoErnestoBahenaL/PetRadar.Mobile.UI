@file:OptIn(ExperimentalMaterial3Api::class)

package com.petradar.mobileui.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.petradar.mobileui.utils.PetImageUrlResolver
import com.petradar.mobileui.viewmodel.AdoptionAnimalDetailViewModel
import androidx.core.net.toUri
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Form for creating or editing an adoption animal.
 *
 * In **edit mode** ([isEditMode] = true):
 *  - Pre-fills fields with data loaded in [AdoptionAnimalDetailViewModel.animal].
 *  - Shows the locally saved photo from [AdoptionPhotoStore] if available.
 *  - On save calls PUT /api/AdoptionAnimals/{id}.
 *
 * In **create mode** ([isEditMode] = false):
 *  - All fields start empty.
 *  - On save calls POST /api/AdoptionAnimals.
 *
 * Features:
 *  - Photo: pick from gallery or take with camera. Stored locally via [AdoptionPhotoStore].
 *  - Age: numeric value + unit selector (months / years). The API field `approximateAge`
 *    stores the age in years (Double), so months are converted (value / 12).
 *
 * When [AdoptionAnimalDetailViewModel.saveSuccess] is true, closes the screen.
 *
 * @param viewModel  ViewModel with the animal data and CRUD operations.
 * @param isEditMode true = editing an existing animal; false = creating a new one.
 * @param onBack     Callback to close the form.
 */
fun AdoptionAnimalFormScreen(
    viewModel: AdoptionAnimalDetailViewModel,
    isEditMode: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit = onBack
) {
    val context = LocalContext.current
    val animalData by viewModel.animal.observeAsState()
    val isLoadingRaw by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingRaw
    val errorMessage by viewModel.errorMessage.observeAsState()
    val saveSuccessRaw by viewModel.saveSuccess.observeAsState(false)
    val saveSuccess = saveSuccessRaw
    val additionalPhotos by viewModel.additionalPhotos.observeAsState(emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    var visible by remember { mutableStateOf(false) }

    // ── Photo state ──────────────────────────────────────────────────────
    // photoUri = local URI selected by the user (replaces API photo on display)
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    // pendingAdditionalPhotos = new photos selected by the user, to upload after save
    var pendingAdditionalPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Temporary file URI for camera captures
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // Gallery picker (main photo)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) photoUri = uri }

    // Gallery picker (additional photos — multiple selection)
    val additionalGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val totalAllowed = (5 - additionalPhotos.size - pendingAdditionalPhotos.size).coerceAtLeast(0)
            pendingAdditionalPhotos = pendingAdditionalPhotos + uris.take(totalAllowed)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            photoUri = cameraImageUri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                .let { File(it, "adoption_${System.currentTimeMillis()}.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                .let { File(it, "adoption_${System.currentTimeMillis()}.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── Form fields ──────────────────────────────────────────────────────
    var animalName by remember { mutableStateOf("") }
    var speciesLabel by remember { mutableStateOf("") }
    var speciesValue by remember { mutableStateOf("") }
    var sexLabel by remember { mutableStateOf("") }
    var sexValue by remember { mutableStateOf("") }
    var sizeLabel by remember { mutableStateOf("") }
    var sizeValue by remember { mutableStateOf("") }
    var animalBreed by remember { mutableStateOf("") }
    var animalColor by remember { mutableStateOf("") }
    // Age: split into value + unit (months / years)
    var ageValue by remember { mutableStateOf("") }
    var ageUnitLabel by remember { mutableStateOf("Años") }
    var ageUnitValue by remember { mutableStateOf("years") } // "months" or "years"
    var ageUnitExpanded by remember { mutableStateOf(false) }
    val ageUnitOptions = listOf("Meses" to "months", "Años" to "years")

    var animalWeight by remember { mutableStateOf("") }
    var animalDescription by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var isNeutered by remember { mutableStateOf(false) }
    var isVaccinated by remember { mutableStateOf(false) }
    var goodWithKids by remember { mutableStateOf(false) }
    var goodWithDogs by remember { mutableStateOf(false) }
    var goodWithCats by remember { mutableStateOf(false) }
    var needsSpecialCare by remember { mutableStateOf(false) }
    var specialCareDetails by remember { mutableStateOf("") }

    // Edit-only fields
    var statusLabel by remember { mutableStateOf("") }
    var statusValue by remember { mutableStateOf("") }
    var adoptionDate by remember { mutableStateOf("") }
    var adopterId by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var speciesError by remember { mutableStateOf<String?>(null) }

    // ── Discard-changes guard ────────────────────────────────────────────
    // origXxx = snapshot of values loaded from the API (edit mode) or empty (create mode).
    // formInitialized turns true once the snapshot is captured so hasChanges is stable.
    var origName        by remember { mutableStateOf("") }
    var origSpecies     by remember { mutableStateOf("") }
    var origSex         by remember { mutableStateOf("") }
    var origSize        by remember { mutableStateOf("") }
    var origBreed       by remember { mutableStateOf("") }
    var origColor       by remember { mutableStateOf("") }
    var origAgeValue    by remember { mutableStateOf("") }
    var origAgeUnit     by remember { mutableStateOf("years") }
    var origWeight      by remember { mutableStateOf("") }
    var origDescription by remember { mutableStateOf("") }
    var origPersonality by remember { mutableStateOf("") }
    var origNeutered    by remember { mutableStateOf(false) }
    var origVaccinated  by remember { mutableStateOf(false) }
    var origGoodKids    by remember { mutableStateOf(false) }
    var origGoodDogs    by remember { mutableStateOf(false) }
    var origGoodCats    by remember { mutableStateOf(false) }
    var origSpecialCare by remember { mutableStateOf(false) }
    var origSpecialDetails by remember { mutableStateOf("") }
    var origStatus      by remember { mutableStateOf("") }
    var formInitialized by remember { mutableStateOf(!isEditMode) }

    // Computed directly during composition so Compose always reads the latest state.
    val hasChanges = formInitialized && (
        animalName         != origName           ||
        speciesValue       != origSpecies        ||
        sexValue           != origSex            ||
        sizeValue          != origSize           ||
        animalBreed        != origBreed          ||
        animalColor        != origColor          ||
        ageValue           != origAgeValue       ||
        ageUnitValue       != origAgeUnit        ||
        animalWeight       != origWeight         ||
        animalDescription  != origDescription    ||
        personality        != origPersonality    ||
        isNeutered         != origNeutered       ||
        isVaccinated       != origVaccinated     ||
        goodWithKids       != origGoodKids       ||
        goodWithDogs       != origGoodDogs       ||
        goodWithCats       != origGoodCats       ||
        needsSpecialCare   != origSpecialCare    ||
        specialCareDetails != origSpecialDetails ||
        statusValue        != origStatus         ||
        photoUri           != null               ||
        pendingAdditionalPhotos.isNotEmpty()
    )

    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = hasChanges) { showDiscardDialog = true }

    var speciesExpanded by remember { mutableStateOf(false) }
    var sexExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val speciesOptions = listOf("Perro" to "Dog", "Gato" to "Cat")
    val sexOptions = listOf("Macho" to "Male", "Hembra" to "Female", "Desconocido" to "Unknown")
    val sizeOptions = listOf("Pequeño" to "Small", "Mediano" to "Medium", "Grande" to "Large")
    val statusOptions = listOf("Disponible" to "Available", "Adoptado" to "Adopted", "Reservado" to "Reserved")

    LaunchedEffect(Unit) {
        visible = true
        if (isEditMode && viewModel.currentAnimalId > 0) {
            viewModel.loadAdditionalPhotos(viewModel.currentAnimalId)
        }
    }

    LaunchedEffect(animalData) {
        val a = animalData ?: return@LaunchedEffect
        animalName = a.name ?: ""
        animalBreed = a.breed ?: ""
        animalColor = a.color ?: ""
        animalWeight = a.weight?.toString() ?: ""
        animalDescription = a.description ?: ""
        personality = a.personality ?: ""
        isNeutered = a.isNeutered ?: false
        isVaccinated = a.isVaccinated ?: false
        goodWithKids = a.goodWithKids ?: false
        goodWithDogs = a.goodWithDogs ?: false
        goodWithCats = a.goodWithCats ?: false
        needsSpecialCare = a.needsSpecialCare ?: false
        specialCareDetails = a.specialCareDetails ?: ""
        adoptionDate = a.adoptionDate?.take(10) ?: ""
        adopterId = a.adopterId?.toString() ?: ""

        // Convert API age (always in years) back to a friendly display.
        // If the value is < 1 year, show it in months; otherwise in years.
        val apiAge = a.approximateAge
        if (apiAge != null) {
            if (apiAge < 1.0 && apiAge > 0.0) {
                val months = (apiAge * 12).toInt()
                ageValue = months.toString()
                ageUnitLabel = "Meses"
                ageUnitValue = "months"
            } else {
                ageValue = if (apiAge == apiAge.toLong().toDouble()) apiAge.toLong().toString() else apiAge.toString()
                ageUnitLabel = "Años"
                ageUnitValue = "years"
            }
        }

        val foundSpecies = speciesOptions.find { (_, v) -> v.equals(a.species, ignoreCase = true) }
        speciesLabel = foundSpecies?.first ?: a.species ?: ""
        speciesValue = foundSpecies?.second ?: a.species ?: ""

        val foundSex = sexOptions.find { (_, v) -> v.equals(a.sex, ignoreCase = true) }
        sexLabel = foundSex?.first ?: a.sex ?: ""
        sexValue = foundSex?.second ?: a.sex ?: ""

        val foundSize = sizeOptions.find { (_, v) -> v.equals(a.size, ignoreCase = true) }
        sizeLabel = foundSize?.first ?: a.size ?: ""
        sizeValue = foundSize?.second ?: a.size ?: ""

        val foundStatus = statusOptions.find { (_, v) -> v.equals(a.status, ignoreCase = true) }
        statusLabel = foundStatus?.first ?: a.status ?: ""
        statusValue = foundStatus?.second ?: a.status ?: ""

        // Capture the original snapshot only on the first load.
        if (!formInitialized) {
            origName        = animalName
            origSpecies     = speciesValue
            origSex         = sexValue
            origSize        = sizeValue
            origBreed       = animalBreed
            origColor       = animalColor
            origAgeValue    = ageValue
            origAgeUnit     = ageUnitValue
            origWeight      = animalWeight
            origDescription = animalDescription
            origPersonality = personality
            origNeutered    = isNeutered
            origVaccinated  = isVaccinated
            origGoodKids    = goodWithKids
            origGoodDogs    = goodWithDogs
            origGoodCats    = goodWithCats
            origSpecialCare = needsSpecialCare
            origSpecialDetails = specialCareDetails
            origStatus      = statusValue
            formInitialized = true
        }

        // photoUri stays null; the deterministic API URL is shown as fallback in the UI
    }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) onSaved()
    }

    // Discard-changes confirmation dialog
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

    // Photo source dialog (Gallery vs Camera)
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
                    galleryLauncher.launch("image/*")
                }) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Galería")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Editar Animal" else "Publicar nuevo animal", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { if (hasChanges) showDiscardDialog = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
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

            // ── Photo Section ────────────────────────────────────────────
            // Display priority: local URI selected by user → deterministic API URL → empty state
            val apiPhotoUrl = if (isEditMode && viewModel.currentAnimalId > 0)
                PetImageUrlResolver.adoptionMainPictureEndpoint(viewModel.currentAnimalId)
            else null
            val displayPhoto: Any? = photoUri ?: apiPhotoUrl

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
                        if (displayPhoto != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(displayPhoto)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto del animal",
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
                            Text("Quitar foto nueva", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Additional Photos Section ────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(300, 80)) + slideInVertically(tween(300, 80)) { 30 }) {
                Text("Fotos Adicionales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(visible, enter = fadeIn(tween(300, 120)) + slideInVertically(tween(300, 120)) { 30 }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Existing photos from API (edit mode)
                        if (additionalPhotos.isNotEmpty()) {
                            Text("Fotos actuales", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(additionalPhotos) { photoName ->
                                    val photoUrl = com.petradar.mobileui.utils.PetImageUrlResolver
                                        .adoptionAdditionalPhotoUrl(viewModel.currentAnimalId, photoName)
                                    Box(modifier = Modifier.size(80.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(photoUrl).crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteAdditionalPhoto(viewModel.currentAnimalId, photoName)
                                            },
                                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Pending photos (selected but not yet uploaded)
                        if (pendingAdditionalPhotos.isNotEmpty()) {
                            Text("Nuevas fotos (${pendingAdditionalPhotos.size})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(pendingAdditionalPhotos) { index, uri ->
                                    Box(modifier = Modifier.size(80.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                        )
                                        IconButton(
                                            onClick = {
                                                pendingAdditionalPhotos = pendingAdditionalPhotos.toMutableList().also { it.removeAt(index) }
                                            },
                                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        val totalPhotos = additionalPhotos.size + pendingAdditionalPhotos.size
                        OutlinedButton(
                            onClick = { additionalGalleryLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && totalPhotos < 5
                        ) {
                            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (totalPhotos >= 5) "Límite de 5 fotos alcanzado" else "Agregar fotos")
                        }
                    }
                }
            }

            // ── Basic Information Section ─────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }) {
                Text("Información Básica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 80)) + slideInVertically(tween(400, 80)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = animalName, onValueChange = { animalName = it; nameError = null },
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
                        OutlinedTextField(value = animalBreed, onValueChange = { animalBreed = it }, label = { Text("Raza") },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = animalColor, onValueChange = { animalColor = it }, label = { Text("Color") },
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

            // ── Additional Details Section ───────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 150)) + slideInVertically(tween(400, 150)) { 40 }) {
                Text("Detalles Adicionales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 200)) + slideInVertically(tween(400, 200)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // Age: value + unit (months / years)
                        Text("Edad aproximada", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            OutlinedTextField(
                                value = ageValue,
                                onValueChange = { ageValue = it },
                                label = { Text("Cantidad") },
                                leadingIcon = { Icon(Icons.Default.Cake, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            )
                            ExposedDropdownMenuBox(
                                expanded = ageUnitExpanded,
                                onExpandedChange = { ageUnitExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = ageUnitLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Unidad") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ageUnitExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    enabled = !isLoading
                                )
                                ExposedDropdownMenu(expanded = ageUnitExpanded, onDismissRequest = { ageUnitExpanded = false }) {
                                    ageUnitOptions.forEach { (label, value) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = { ageUnitLabel = label; ageUnitValue = value; ageUnitExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(value = animalWeight, onValueChange = { animalWeight = it }, label = { Text("Peso (kg)") },
                            leadingIcon = { Icon(Icons.Default.MonitorWeight, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                        OutlinedTextField(value = animalDescription, onValueChange = { animalDescription = it }, label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !isLoading)
                        OutlinedTextField(value = personality, onValueChange = { personality = it }, label = { Text("Personalidad") },
                            leadingIcon = { Icon(Icons.Default.EmojiEmotions, null) },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                    }
                }
            }

            // ── Compatibility & Health Section ───────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 250)) + slideInVertically(tween(400, 250)) { 40 }) {
                Text("Compatibilidad y Salud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(visible, enter = fadeIn(tween(400, 300)) + slideInVertically(tween(400, 300)) { 40 }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isNeutered, onCheckedChange = { isNeutered = it }, enabled = !isLoading)
                            Spacer(Modifier.width(8.dp))
                            Text("Esterilizado/Castrado")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isVaccinated, onCheckedChange = { isVaccinated = it }, enabled = !isLoading)
                            Spacer(Modifier.width(8.dp))
                            Text("Vacunado")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = goodWithKids, onCheckedChange = { goodWithKids = it }, enabled = !isLoading)
                            Spacer(Modifier.width(8.dp))
                            Text("Bueno con niños")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = goodWithDogs, onCheckedChange = { goodWithDogs = it }, enabled = !isLoading)
                            Spacer(Modifier.width(8.dp))
                            Text("Bueno con perros")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = goodWithCats, onCheckedChange = { goodWithCats = it }, enabled = !isLoading)
                            Spacer(Modifier.width(8.dp))
                            Text("Bueno con gatos")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = needsSpecialCare, onCheckedChange = { needsSpecialCare = it }, enabled = !isLoading)
                            Spacer(Modifier.width(8.dp))
                            Text("Necesita cuidados especiales")
                        }
                        if (needsSpecialCare) {
                            OutlinedTextField(
                                value = specialCareDetails, onValueChange = { specialCareDetails = it },
                                label = { Text("Detalles de cuidados especiales") },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, null) },
                                modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !isLoading
                            )
                        }
                    }
                }
            }

            // ── Status Section (Edit mode only) ──────────────────────────
            if (isEditMode) {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 350)) + slideInVertically(tween(400, 350)) { 40 }) {
                    Text("Estado de Adopción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 400)) + slideInVertically(tween(400, 400)) { 40 }) {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                                OutlinedTextField(
                                    value = statusLabel, onValueChange = {}, readOnly = true,
                                    label = { Text("Estado") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
                                value = adoptionDate, onValueChange = { adoptionDate = it },
                                label = { Text("Fecha de adopción (YYYY-MM-DD)") },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                                modifier = Modifier.fillMaxWidth(), enabled = !isLoading
                            )
                            OutlinedTextField(
                                value = adopterId, onValueChange = { adopterId = it },
                                label = { Text("ID del adoptante") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(), enabled = !isLoading
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Save Button ──────────────────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(400, if (isEditMode) 450 else 350)) + slideInVertically(tween(400, if (isEditMode) 450 else 350)) { 60 }) {
                Button(
                    onClick = {
                        if (animalName.isBlank()) { nameError = "El nombre es requerido"; return@Button }
                        if (speciesValue.isBlank()) { speciesError = "Selecciona la especie"; return@Button }

                        // Convert age value + unit → approximateAge in years (Double)
                        val rawAge = ageValue.trim().toDoubleOrNull()
                        val approximateAgeInYears: Double? = when {
                            rawAge == null -> null
                            ageUnitValue == "months" -> rawAge / 12.0
                            else -> rawAge
                        }

                        viewModel.saveAnimal(
                            name = animalName.trim(),
                            speciesValue = speciesValue,
                            breed = animalBreed.trim().ifEmpty { null },
                            color = animalColor.trim().ifEmpty { null },
                            sexValue = sexValue.ifEmpty { null },
                            sizeValue = sizeValue.ifEmpty { null },
                            approximateAge = approximateAgeInYears,
                            weight = animalWeight.trim().toDoubleOrNull(),
                            description = animalDescription.trim().ifEmpty { null },
                            isNeutered = isNeutered,
                            personality = personality.trim().ifEmpty { null },
                            goodWithKids = goodWithKids,
                            goodWithDogs = goodWithDogs,
                            goodWithCats = goodWithCats,
                            isVaccinated = isVaccinated,
                            needsSpecialCare = needsSpecialCare,
                            specialCareDetails = specialCareDetails.trim().ifEmpty { null },
                            status = if (isEditMode) statusValue.ifEmpty { null } else null,
                            adoptionDate = if (isEditMode) adoptionDate.trim().ifEmpty { null } else null,
                            adopterId = if (isEditMode) adopterId.trim().toLongOrNull() else null,
                            photoUri = photoUri?.toString(),
                            additionalPhotoUris = pendingAdditionalPhotos,
                            context = context
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
                        Text(if (isEditMode) "Actualizar Animal" else "Guardar Animal", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
