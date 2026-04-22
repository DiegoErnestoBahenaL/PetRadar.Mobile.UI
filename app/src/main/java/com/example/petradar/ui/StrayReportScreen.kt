@file:Suppress("ClickableViewAccessibility")

package com.example.petradar.ui

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.viewmodel.StrayReportViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val MAX_ADDITIONAL_PHOTOS = 5

data class StrayReportFormData(
    val latitude: Double,
    val longitude: Double,
    val addressText: String?,
    val hasCollar: Boolean,
    val hasTag: Boolean,
    val incidentDateIso: String,
    val size: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrayReportScreen(
    viewModel: StrayReportViewModel,
    initialPhotoUri: String? = null,
    onBack: () -> Unit,
    onSubmit: (StrayReportFormData, String?, List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val saveSuccess by viewModel.saveSuccess.observeAsState(false)

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Photo state ────────────────────────────────────────────────────────────
    var mainPhotoUri by remember { mutableStateOf<Uri?>(initialPhotoUri?.toUri()) }
    var cameraMainUri by remember { mutableStateOf<Uri?>(null) }
    var showMainPhotoDialog by remember { mutableStateOf(false) }

    var additionalPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var cameraAdditionalUri by remember { mutableStateOf<Uri?>(null) }
    var showAdditionalPhotoDialog by remember { mutableStateOf(false) }

    // ── Location state ─────────────────────────────────────────────────────────
    val defaultPoint = remember { GeoPoint(19.4326, -99.1332) }
    var selectedPoint by remember { mutableStateOf(defaultPoint) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var addressText by remember { mutableStateOf("") }
    var isResolvingAddress by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var locationReady by remember { mutableStateOf(false) }

    // ── Form toggles ───────────────────────────────────────────────────────────
    var hasCollar by remember { mutableStateOf(false) }
    var hasTag by remember { mutableStateOf(false) }
    var selectedSize by remember { mutableStateOf<String?>(null) }
    var sizeDropdownExpanded by remember { mutableStateOf(false) }

    // ── Photo launchers ────────────────────────────────────────────────────────
    val mainGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) mainPhotoUri = uri }

    val mainCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success && cameraMainUri != null) mainPhotoUri = cameraMainUri }

    val mainCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                .let { File(it, "stray_main_${System.currentTimeMillis()}.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraMainUri = uri
            mainCameraLauncher.launch(uri)
        }
    }

    fun launchMainCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val file = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                .let { File(it, "stray_main_${System.currentTimeMillis()}.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraMainUri = uri
            mainCameraLauncher.launch(uri)
        } else {
            mainCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val additionalGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val allowed = (MAX_ADDITIONAL_PHOTOS - additionalPhotoUris.size).coerceAtLeast(0)
            additionalPhotoUris = additionalPhotoUris + uris.take(allowed)
        }
    }

    val additionalCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraAdditionalUri != null && additionalPhotoUris.size < MAX_ADDITIONAL_PHOTOS) {
            additionalPhotoUris = additionalPhotoUris + cameraAdditionalUri!!
        }
    }

    val additionalCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                .let { File(it, "stray_add_${System.currentTimeMillis()}.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraAdditionalUri = uri
            additionalCameraLauncher.launch(uri)
        }
    }

    fun launchAdditionalCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val file = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                .let { File(it, "stray_add_${System.currentTimeMillis()}.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraAdditionalUri = uri
            additionalCameraLauncher.launch(uri)
        } else {
            additionalCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── Location helpers ───────────────────────────────────────────────────────
    fun selectPoint(point: GeoPoint) {
        selectedPoint = point
        locationError = null
        locationReady = true
        mapView?.controller?.setCenter(point)
        mapView?.controller?.setZoom(16.0)
        mapView?.invalidate()
        scope.launch {
            isResolvingAddress = true
            val addr = strayReverseGeocode(context, point.latitude, point.longitude)
            isResolvingAddress = false
            if (addr.isNotBlank()) addressText = addr
        }
    }

    // ── Location permission launcher ───────────────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            strayTryCurrentLocation(context) { point, error ->
                if (point != null) selectPoint(point)
                else locationError = error ?: "No se pudo obtener la ubicación."
            }
        } else {
            locationError = "Se denegó el permiso de ubicación. El reporte usará la ubicación predeterminada."
            selectPoint(defaultPoint)
        }
    }

    // Auto-detect location on first composition
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            strayTryCurrentLocation(context) { point, _ ->
                if (point != null) selectPoint(point) else selectPoint(defaultPoint)
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) onBack()
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showMainPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showMainPhotoDialog = false },
            icon = { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Foto principal") },
            text = { Text("¿Cómo deseas agregar la foto?") },
            confirmButton = {
                TextButton(onClick = { showMainPhotoDialog = false; launchMainCamera() }) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMainPhotoDialog = false; mainGalleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Galería")
                }
            }
        )
    }

    if (showAdditionalPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showAdditionalPhotoDialog = false },
            icon = { Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Fotos adicionales") },
            text = { Text("¿Cómo deseas agregar las fotos?") },
            confirmButton = {
                TextButton(onClick = { showAdditionalPhotoDialog = false; launchAdditionalCamera() }) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdditionalPhotoDialog = false; additionalGalleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Galería")
                }
            }
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar animal en calle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Foto principal ─────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Foto principal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (mainPhotoUri != null) {
                        Box {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(mainPhotoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto principal",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            IconButton(
                                onClick = { mainPhotoUri = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        OutlinedButton(
                            onClick = { showMainPhotoDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cambiar foto principal")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showMainPhotoDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar foto principal")
                        }
                    }
                }
            }

            // ── Fotos adicionales ──────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Fotos adicionales",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (additionalPhotoUris.isNotEmpty()) {
                            Text(
                                "(${additionalPhotoUris.size}/$MAX_ADDITIONAL_PHOTOS)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (additionalPhotoUris.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(additionalPhotoUris) { index, uri ->
                                Box(modifier = Modifier.size(88.dp)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    IconButton(
                                        onClick = {
                                            additionalPhotoUris = additionalPhotoUris
                                                .toMutableList()
                                                .also { it.removeAt(index) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Cancel,
                                            contentDescription = "Eliminar foto",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    if (additionalPhotoUris.size < MAX_ADDITIONAL_PHOTOS) {
                        OutlinedButton(
                            onClick = { showAdditionalPhotoDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar fotos")
                        }
                    }
                }
            }

            // ── Ubicación ──────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Ubicación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = "Se utilizará la ubicación actual del dispositivo para este reporte. " +
                                    "Toca el mapa para ajustar el punto si es necesario.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Address display
                    when {
                        isResolvingAddress -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "Obteniendo ubicación...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        addressText.isNotBlank() -> {
                            Text(
                                text = addressText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    locationError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ── Mapa interactivo ───────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        val map = MapView(ctx).apply {
                            Configuration.getInstance().userAgentValue = ctx.packageName
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(14.0)
                            controller.setCenter(selectedPoint)
                        }
                        map.overlays.add(
                            MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let { selectPoint(it) }
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint?) = false
                            })
                        )
                        map.setOnTouchListener { view, event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN,
                                MotionEvent.ACTION_MOVE ->
                                    view.parent?.requestDisallowInterceptTouchEvent(true)
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
                        map.overlays.add(
                            MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let { selectPoint(it) }
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint?) = false
                            })
                        )
                        map.controller.setCenter(selectedPoint)
                        map.overlays.add(
                            Marker(map).apply {
                                position = selectedPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Ubicación del animal"
                            }
                        )
                        map.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            // ── Collar & Placa ─────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Características observadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Llevaba collar", modifier = Modifier.weight(1f))
                        Switch(
                            checked = hasCollar,
                            onCheckedChange = { hasCollar = it },
                            enabled = !isLoading
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Llevaba placa", modifier = Modifier.weight(1f))
                        Switch(
                            checked = hasTag,
                            onCheckedChange = { hasTag = it },
                            enabled = !isLoading
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val sizeOptions = listOf(
                        null to "Desconocido",
                        "Small" to "Pequeño",
                        "Medium" to "Mediano",
                        "Large" to "Grande"
                    )
                    ExposedDropdownMenuBox(
                        expanded = sizeDropdownExpanded,
                        onExpandedChange = { sizeDropdownExpanded = !sizeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = sizeOptions.find { it.first == selectedSize }?.second ?: "Desconocido",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tamaño aproximado") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            enabled = !isLoading
                        )
                        ExposedDropdownMenu(
                            expanded = sizeDropdownExpanded,
                            onDismissRequest = { sizeDropdownExpanded = false }
                        ) {
                            sizeOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedSize = value
                                        sizeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Submit ─────────────────────────────────────────────────────────
            Button(
                onClick = {
                    val form = StrayReportFormData(
                        latitude = selectedPoint.latitude,
                        longitude = selectedPoint.longitude,
                        addressText = addressText.trim().ifEmpty { null },
                        hasCollar = hasCollar,
                        hasTag = hasTag,
                        incidentDateIso = LocalDateTime.now()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        size = selectedSize
                    )
                    val localMain = mainPhotoUri?.let { uri ->
                        val scheme = uri.scheme?.lowercase() ?: ""
                        if (scheme == "content" || scheme == "file") uri.toString() else null
                    }
                    val localAdditional = additionalPhotoUris.map { it.toString() }
                    onSubmit(form, localMain, localAdditional)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Report, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Publicar reporte")
                }
            }

            TextButton(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Cancelar")
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

// ── Private geocoding helpers ──────────────────────────────────────────────────

private suspend fun strayReverseGeocode(context: Context, lat: Double, lon: Double): String =
    withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val deferred = CompletableDeferred<String>()
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    deferred.complete(addresses.firstOrNull()?.getAddressLine(0) ?: "")
                }
                deferred.await()
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()?.getAddressLine(0) ?: ""
            }
        }.getOrDefault("")
    }

private fun strayTryCurrentLocation(
    context: Context,
    callback: (point: GeoPoint?, errorMessage: String?) -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        callback(null, "Se requiere el permiso de ubicación.")
        return
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        @Suppress("MissingPermission")
        runCatching {
            locationManager.getCurrentLocation(
                LocationManager.GPS_PROVIDER,
                null,
                context.mainExecutor
            ) { location ->
                if (location != null) {
                    callback(location.strayToGeoPoint(), null)
                } else {
                    callback(strayGetLastKnown(context, locationManager), null)
                }
            }
        }.getOrElse {
            callback(strayGetLastKnown(context, locationManager), null)
        }
        return
    }

    callback(strayGetLastKnown(context, locationManager), null)
}

@Suppress("MissingPermission")
private fun strayGetLastKnown(context: Context, locationManager: LocationManager): GeoPoint? {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) return null

    if (locationManager.allProviders.isEmpty()) return null

    return locationManager
        .getProviders(true)
        .filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }
        ?.strayToGeoPoint()
}

private fun Location.strayToGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
