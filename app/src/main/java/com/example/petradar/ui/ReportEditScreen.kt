@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("ClickableViewAccessibility")

package com.example.petradar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.api.ReportUpdateModel
import com.example.petradar.utils.PetImageUrlResolver
import com.example.petradar.viewmodel.ReportEditViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportEditScreen(
    viewModel: ReportEditViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reportData by viewModel.report.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val saveSuccess by viewModel.saveSuccess.observeAsState(false)
    val additionalPhotos by viewModel.additionalPhotos.observeAsState(emptyList())

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Photo state ──────────────────────────────────────────────────────
    var mainPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAdditionalPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) mainPhotoUri = uri
    }
    val additionalGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val totalAllowed = (5 - additionalPhotos.size - pendingAdditionalPhotos.size).coerceAtLeast(0)
            pendingAdditionalPhotos = pendingAdditionalPhotos + uris.take(totalAllowed)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) mainPhotoUri = cameraImageUri
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = createCameraUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── Dropdown options ─────────────────────────────────────────────────
    val statusOptions = listOf("Activo" to "Active", "Encontrado" to "Found", "Cerrado" to "Closed")

    // ── Editable form fields ─────────────────────────────────────────────
    var description by remember { mutableStateOf("") }
    var statusLabel by remember { mutableStateOf("") }
    var statusValue by remember { mutableStateOf("") }
    var hasCollar by remember { mutableStateOf(false) }
    var hasTag by remember { mutableStateOf(false) }
    var searchRadiusMeters by remember { mutableStateOf("") }
    var useAlternateContact by remember { mutableStateOf(false) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var offersReward by remember { mutableStateOf(false) }
    var rewardAmount by remember { mutableStateOf("") }

    // ── Map / location ───────────────────────────────────────────────────
    val defaultPoint = remember { GeoPoint(19.4326, -99.1332) }
    var selectedPoint by remember { mutableStateOf(defaultPoint) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var addressText by remember { mutableStateOf("") }
    var locationError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var isGeocoding by remember { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }

    // ── Status dropdown expanded ─────────────────────────────────────────
    var statusExpanded by remember { mutableStateOf(false) }

    // ── Discard-changes guard ────────────────────────────────────────────
    var origDescription by remember { mutableStateOf("") }
    var origStatus by remember { mutableStateOf("") }
    var origHasCollar by remember { mutableStateOf(false) }
    var origHasTag by remember { mutableStateOf(false) }
    var origSearchRadius by remember { mutableStateOf("") }
    var origUseAlternate by remember { mutableStateOf(false) }
    var origContactName by remember { mutableStateOf("") }
    var origContactPhone by remember { mutableStateOf("") }
    var origContactEmail by remember { mutableStateOf("") }
    var origOffersReward by remember { mutableStateOf(false) }
    var origRewardAmount by remember { mutableStateOf("") }
    var origAddress by remember { mutableStateOf("") }
    var origLat by remember { mutableDoubleStateOf(0.0) }
    var origLon by remember { mutableDoubleStateOf(0.0) }
    var formInitialized by remember { mutableStateOf(false) }

    val hasChanges = formInitialized && (
        description != origDescription ||
        statusValue != origStatus ||
        hasCollar != origHasCollar ||
        hasTag != origHasTag ||
        searchRadiusMeters != origSearchRadius ||
        useAlternateContact != origUseAlternate ||
        contactName != origContactName ||
        contactPhone != origContactPhone ||
        contactEmail != origContactEmail ||
        offersReward != origOffersReward ||
        rewardAmount != origRewardAmount ||
        addressText != origAddress ||
        (mapReady && (selectedPoint.latitude != origLat || selectedPoint.longitude != origLon)) ||
        mainPhotoUri != null ||
        pendingAdditionalPhotos.isNotEmpty()
    )

    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = hasChanges) { showDiscardDialog = true }

    // ── Location helpers ─────────────────────────────────────────────────
    fun selectPoint(point: GeoPoint) {
        selectedPoint = point
        locationError = null
        mapView?.controller?.setCenter(point)
        mapView?.controller?.setZoom(16.0)
        mapView?.invalidate()
        scope.launch {
            val addr = reverseGeocode(context, point.latitude, point.longitude)
            if (addr.isNotBlank()) addressText = addr
        }
    }

    fun searchAddress() {
        if (addressText.isBlank()) return
        scope.launch {
            isGeocoding = true
            addressError = null
            val point = forwardGeocode(context, addressText)
            isGeocoding = false
            if (point != null) selectPoint(point)
            else addressError = "No se encontró la dirección. Intenta ser más específico."
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            tryUseCurrentLocation(context) { point, error ->
                if (point != null) selectPoint(point)
                else locationError = error ?: "No se pudo obtener tu ubicación actual."
            }
        } else {
            locationError = "Se denegó el permiso de ubicación."
        }
    }

    fun requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            tryUseCurrentLocation(context) { point, error ->
                if (point != null) selectPoint(point) else locationError = error ?: "No se pudo obtener tu ubicación."
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // ── Effects ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        reportData?.id?.let { viewModel.loadAdditionalPhotos(it) }
    }

    LaunchedEffect(reportData) {
        val r = reportData ?: return@LaunchedEffect

        description = r.description ?: ""
        hasCollar = r.hasCollar ?: false
        hasTag = r.hasTag ?: false
        searchRadiusMeters = r.searchRadiusMeters?.toString() ?: ""
        useAlternateContact = r.useAlternateContact ?: false
        contactName = r.contactName ?: ""
        contactPhone = r.contactPhone ?: ""
        contactEmail = r.contactEmail ?: ""
        offersReward = r.offersReward ?: false
        rewardAmount = r.rewardAmount?.toString() ?: ""
        addressText = r.addressText ?: ""

        val foundStatus = statusOptions.find { (_, v) -> v.equals(r.reportStatus, ignoreCase = true) }
        statusLabel = foundStatus?.first ?: r.reportStatus ?: ""
        statusValue = foundStatus?.second ?: r.reportStatus ?: ""

        val lat = r.latitude ?: defaultPoint.latitude
        val lon = r.longitude ?: defaultPoint.longitude
        val point = GeoPoint(lat, lon)
        selectedPoint = point
        mapView?.controller?.setCenter(point)
        mapView?.controller?.setZoom(15.0)
        mapView?.invalidate()

        // Load additional photos once we know the reportId
        viewModel.loadAdditionalPhotos(r.id)

        if (!formInitialized) {
            origDescription = description
            origStatus = statusValue
            origHasCollar = hasCollar
            origHasTag = hasTag
            origSearchRadius = searchRadiusMeters
            origUseAlternate = useAlternateContact
            origContactName = contactName
            origContactPhone = contactPhone
            origContactEmail = contactEmail
            origOffersReward = offersReward
            origRewardAmount = rewardAmount
            origAddress = addressText
            origLat = lat
            origLon = lon
            formInitialized = true
            mapReady = true
        }
    }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) onSaved()
    }

    // ── Discard dialog ────────────────────────────────────────────────────
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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

    // ── Photo source dialog ───────────────────────────────────────────────
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            icon = { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Foto principal") },
            text = { Text("¿Cómo deseas agregar la foto?") },
            confirmButton = {
                TextButton(onClick = { showPhotoSourceDialog = false; launchCamera() }) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false; galleryLauncher.launch("image/*") }) {
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
                title = { Text("Editar reporte", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showDiscardDialog = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading && reportData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Info de la mascota (solo lectura) ────────────────────
                val r = reportData
                if (r != null) {
                    Text("Información de la mascota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            InfoRow("Especie", translateSpecies(r.species))
                            InfoRow("Raza", r.breed ?: "-")
                            InfoRow("Color", r.color ?: "-")
                            InfoRow("Sexo", translateSex(r.sex))
                            InfoRow("Tamaño", translateSize(r.size))
                            r.approximateAge?.let { InfoRow("Edad aprox.", "$it años") }
                            r.weight?.let { InfoRow("Peso", "$it kg") }
                            InfoRow("Esterilizado/a", if (r.isNeutered == true) "Sí" else "No")
                        }
                    }
                }

                // ── Fotos ────────────────────────────────────────────────
                Text("Fotos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                // Main photo
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Foto principal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                        val reportId = reportData?.id ?: 0L
                        val apiMainPhoto: String? =
                            if (reportId > 0) PetImageUrlResolver.reportMainPictureEndpoint(reportId)
                            else null
                        val displayPhoto: Any? = mainPhotoUri ?: apiMainPhoto

                        if (displayPhoto != null) {
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(displayPhoto)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto principal del reporte",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                IconButton(
                                    onClick = { showPhotoSourceDialog = true },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                if (mainPhotoUri != null) {
                                    IconButton(
                                        onClick = { mainPhotoUri = null },
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showPhotoSourceDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar foto principal")
                            }
                        }
                    }
                }

                // Additional photos
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Fotos adicionales", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                        val reportId = reportData?.id ?: 0L

                        if (additionalPhotos.isNotEmpty()) {
                            Text(
                                "Actuales (${additionalPhotos.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(additionalPhotos) { photoName ->
                                    val url = PetImageUrlResolver.reportAdditionalPhotoUrl(reportId, photoName)
                                    Box(modifier = Modifier.size(80.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteAdditionalPhoto(reportId, photoName) },
                                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (pendingAdditionalPhotos.isNotEmpty()) {
                            HorizontalDivider()
                            Text(
                                "Nuevas (${pendingAdditionalPhotos.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                                            onClick = { pendingAdditionalPhotos = pendingAdditionalPhotos.toMutableList().also { it.removeAt(index) } },
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
                            Text(if (totalPhotos >= 5) "Límite de 5 fotos alcanzado" else "Agregar fotos adicionales")
                        }
                    }
                }

                // ── Datos del reporte ────────────────────────────────────
                Text("Datos del reporte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                // Status
                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }) {
                    OutlinedTextField(
                        value = statusLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado del reporte") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        statusOptions.forEach { (label, value) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                statusLabel = label; statusValue = value; statusExpanded = false
                            })
                        }
                    }
                }

                // hasCollar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Llevaba collar", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = hasCollar, onCheckedChange = { hasCollar = it })
                }

                // hasTag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Llevaba placa identificadora", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = hasTag, onCheckedChange = { hasTag = it })
                }

                // searchRadius
                OutlinedTextField(
                    value = searchRadiusMeters,
                    onValueChange = { searchRadiusMeters = it.filter { c -> c.isDigit() } },
                    label = { Text("Radio de búsqueda (metros)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // ── Ubicación ────────────────────────────────────────────
                Text("Ubicación del incidente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Toca en el mapa para ajustar la ubicación.", style = MaterialTheme.typography.bodyMedium)

                Card(modifier = Modifier.fillMaxWidth()) {
                    AndroidView(
                        factory = { ctx ->
                            val map = MapView(ctx).apply {
                                Configuration.getInstance().userAgentValue = ctx.packageName
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(15.0)
                                controller.setCenter(selectedPoint)
                            }
                            map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let { selectPoint(it) }; return true
                                }
                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }))
                            map.setOnTouchListener { view, event ->
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN,
                                    MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)
                                    MotionEvent.ACTION_UP,
                                    MotionEvent.ACTION_CANCEL -> {
                                        view.parent?.requestDisallowInterceptTouchEvent(false)
                                        view.performClick()
                                    }
                                }
                                false
                            }
                            mapView = map
                            map
                        },
                        update = { map ->
                            mapView = map
                            map.overlays.clear()
                            map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let { selectPoint(it) }; return true
                                }
                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }))
                            map.controller.setCenter(selectedPoint)
                            map.overlays.add(Marker(map).apply {
                                position = selectedPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Ubicación seleccionada"
                            })
                            map.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )
                }

                Row {
                    TextButton(onClick = { requestCurrentLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Usar mi ubicación actual")
                    }
                }

                locationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it; addressError = null },
                    label = { Text("Dirección de referencia") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGeocoding,
                    isError = addressError != null,
                    supportingText = addressError?.let { err -> { Text(err) } },
                    trailingIcon = {
                        if (isGeocoding) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { searchAddress() }, enabled = addressText.isNotBlank()) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar dirección")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchAddress() })
                )

                // ── Contacto alternativo ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Usar contacto alternativo", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = useAlternateContact, onCheckedChange = { useAlternateContact = it })
                }

                if (useAlternateContact) {
                    OutlinedTextField(
                        value = contactName, onValueChange = { contactName = it },
                        label = { Text("Nombre del contacto") }, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactPhone, onValueChange = { contactPhone = it },
                        label = { Text("Teléfono del contacto") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = contactEmail, onValueChange = { contactEmail = it },
                        label = { Text("Email del contacto") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }

                // ── Recompensa ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ofrecer recompensa", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = offersReward, onCheckedChange = { offersReward = it })
                }

                if (offersReward) {
                    OutlinedTextField(
                        value = rewardAmount,
                        onValueChange = { rewardAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto de la recompensa") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // ── Save button ──────────────────────────────────────────
                androidx.compose.material3.Button(
                    onClick = {
                        val reportId = reportData?.id ?: return@Button
                        viewModel.updateReport(
                            reportId = reportId,
                            request = ReportUpdateModel(
                                description = description.takeIf { it.isNotBlank() },
                                reportStatus = statusValue.takeIf { it.isNotBlank() },
                                hasCollar = hasCollar,
                                hasTag = hasTag,
                                searchRadiusMeters = searchRadiusMeters.toIntOrNull(),
                                latitude = selectedPoint.latitude,
                                longitude = selectedPoint.longitude,
                                addressText = addressText.takeIf { it.isNotBlank() },
                                useAlternateContact = useAlternateContact,
                                contactName = if (useAlternateContact) contactName.takeIf { it.isNotBlank() } else null,
                                contactPhone = if (useAlternateContact) contactPhone.takeIf { it.isNotBlank() } else null,
                                contactEmail = if (useAlternateContact) contactEmail.takeIf { it.isNotBlank() } else null,
                                offersReward = offersReward,
                                rewardAmount = if (offersReward) rewardAmount.toDoubleOrNull() else null
                            ),
                            photoUri = mainPhotoUri,
                            additionalPhotoUris = pendingAdditionalPhotos,
                            context = context
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text("Guardar cambios")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose {
            mapView?.onDetach()
            mapView = null
        }
    }
}

private fun translateSpecies(species: String?): String = when (species?.lowercase()) {
    "dog" -> "Perro"
    "cat" -> "Gato"
    else -> species ?: "-"
}

private fun translateSex(sex: String?): String = when (sex?.lowercase()) {
    "male" -> "Macho"
    "female" -> "Hembra"
    "unknown" -> "Desconocido"
    else -> sex ?: "-"
}

private fun translateSize(size: String?): String = when (size?.lowercase()) {
    "small" -> "Pequeño"
    "medium" -> "Mediano"
    "large" -> "Grande"
    else -> size ?: "-"
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun createCameraUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
    val photoFile = File(dir, "report_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
}
