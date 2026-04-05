package com.example.petradar.ui

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.viewmodel.LostPetReportViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LostReportFormData(
    val latitude: Double,
    val longitude: Double,
    val addressText: String?,
    val description: String?,
    val incidentDateIso: String,
    val hasCollar: Boolean,
    val hasTag: Boolean,
    val searchRadiusMeters: Int,
    val useAlternateContact: Boolean,
    val contactName: String?,
    val contactPhone: String?,
    val contactEmail: String?,
    val offersReward: Boolean,
    val rewardAmount: Double?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostPetReportScreen(
    viewModel: LostPetReportViewModel,
    onBack: () -> Unit,
    onSubmit: (LostReportFormData, UserPetViewModel?) -> Unit
) {
    val context = LocalContext.current
    val pet by viewModel.pet.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val saveSuccess by viewModel.saveSuccess.observeAsState(false)

    val snackbarHostState = remember { SnackbarHostState() }

    val defaultPoint = remember { GeoPoint(19.4326, -99.1332) }
    var selectedPoint by remember { mutableStateOf(defaultPoint) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var addressText by remember { mutableStateOf("") }
    var detailsText by remember { mutableStateOf("") }
    var hasCollar by remember { mutableStateOf(false) }
    var hasTag by remember { mutableStateOf(false) }
    var searchRadiusText by remember { mutableStateOf("3000") }
    var useAlternateContact by remember { mutableStateOf(false) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var offersReward by remember { mutableStateOf(false) }
    var rewardAmountText by remember { mutableStateOf("") }
    var locationError by remember { mutableStateOf<String?>(null) }

    fun selectPoint(point: GeoPoint) {
        selectedPoint = point
        locationError = null
        mapView?.controller?.setCenter(point)
        mapView?.controller?.setZoom(16.0)
        mapView?.invalidate()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tryUseCurrentLocation(context) { point, error ->
                if (point != null) {
                    selectPoint(point)
                } else {
                    locationError = error ?: "No se pudo obtener tu ubicación actual."
                }
            }
        } else {
            locationError = "Se denegó el permiso de ubicación."
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            tryUseCurrentLocation(context) { point, _ ->
                point?.let { selectPoint(it) }
            }
        } else {
            selectPoint(defaultPoint)
        }
    }

    fun requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            tryUseCurrentLocation(context) { point, error ->
                if (point != null) {
                    selectPoint(point)
                } else {
                    locationError = error ?: "No se pudo obtener tu ubicación actual."
                }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar mascota perdida", fontWeight = FontWeight.Bold) },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mascota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Nombre: ${pet?.name ?: "-"}")
                    Text("Especie: ${pet?.species ?: "-"}")
                    Text("Raza: ${pet?.breed ?: "-"}")
                }
            }

            Text("Ubicación donde se perdió", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Text(
                text = "Toca en el mapa para elegir el lugar donde posiblemente se perdió.",
                style = MaterialTheme.typography.bodyMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        val map = MapView(ctx).apply {
                            Configuration.getInstance().userAgentValue = ctx.packageName
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(12.0)
                            controller.setCenter(selectedPoint)
                        }

                        map.overlays.add(
                            MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let { selectPoint(it) }
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            })
                        )

                        map.setOnTouchListener { view, event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN,
                                MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
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

                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            })
                        )

                        map.controller.setCenter(selectedPoint)
                        map.overlays.add(
                            Marker(map).apply {
                                position = selectedPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Ubicación seleccionada"
                            }
                        )

                        map.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { requestCurrentLocation() }) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(Modifier.height(2.dp))
                    Text("Usar mi ubicación actual")
                }
            }

            locationError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = addressText,
                onValueChange = { addressText = it },
                label = { Text("Dirección de referencia") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = detailsText,
                onValueChange = { detailsText = it },
                label = { Text("Detalles del incidente") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isLoading
            )

            OutlinedTextField(
                value = searchRadiusText,
                onValueChange = { searchRadiusText = it },
                label = { Text("Radio de búsqueda (metros)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Llevaba collar", modifier = Modifier.weight(1f))
                Switch(checked = hasCollar, onCheckedChange = { hasCollar = it }, enabled = !isLoading)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Llevaba placa", modifier = Modifier.weight(1f))
                Switch(checked = hasTag, onCheckedChange = { hasTag = it }, enabled = !isLoading)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Usar contacto alternativo", modifier = Modifier.weight(1f))
                Switch(
                    checked = useAlternateContact,
                    onCheckedChange = { useAlternateContact = it },
                    enabled = !isLoading
                )
            }

            if (useAlternateContact) {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Nombre del contacto") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Teléfono de contacto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Correo de contacto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ofrecer recompensa", modifier = Modifier.weight(1f))
                Switch(checked = offersReward, onCheckedChange = { offersReward = it }, enabled = !isLoading)
            }

            if (offersReward) {
                OutlinedTextField(
                    value = rewardAmountText,
                    onValueChange = { rewardAmountText = it },
                    label = { Text("Monto de la recompensa") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val latitude = selectedPoint.latitude
                    val longitude = selectedPoint.longitude

                    val form = LostReportFormData(
                        latitude = latitude,
                        longitude = longitude,
                        addressText = addressText.trim().ifEmpty { null },
                        description = detailsText.trim().ifEmpty { null },
                        incidentDateIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        hasCollar = hasCollar,
                        hasTag = hasTag,
                        searchRadiusMeters = searchRadiusText.toIntOrNull() ?: 3000,
                        useAlternateContact = useAlternateContact,
                        contactName = contactName.trim().ifEmpty { null },
                        contactPhone = contactPhone.trim().ifEmpty { null },
                        contactEmail = contactEmail.trim().ifEmpty { null },
                        offersReward = offersReward,
                        rewardAmount = rewardAmountText.toDoubleOrNull()
                    )
                    onSubmit(form, pet)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Report, contentDescription = null)
                    Spacer(Modifier.height(2.dp))
                    Text("Publicar reporte")
                }
            }

            TextButton(onClick = onBack, enabled = !isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
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

private fun tryUseCurrentLocation(context: Context, callback: (point: GeoPoint?, errorMessage: String?) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
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
                    callback(location.toGeoPoint(), null)
                } else {
                    callback(getLastKnownGeoPoint(context, locationManager), "No se pudo obtener tu ubicación actual.")
                }
            }
        }.getOrElse {
            callback(getLastKnownGeoPoint(context, locationManager), "No se pudo obtener tu ubicación actual.")
        }
        return
    }

    callback(getLastKnownGeoPoint(context, locationManager), "No se pudo obtener tu ubicación actual.")
}

@Suppress("MissingPermission")
private fun getLastKnownGeoPoint(context: Context, locationManager: LocationManager): GeoPoint? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        return null
    }

    if (locationManager.allProviders.isEmpty()) return null

    val bestLocation = locationManager
        .getProviders(true)
        .filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }

    return bestLocation?.toGeoPoint()
}

private fun Location.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)











