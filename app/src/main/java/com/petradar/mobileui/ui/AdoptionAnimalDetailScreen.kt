@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("ClickableViewAccessibility")

package com.petradar.mobileui.ui

import android.Manifest
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.petradar.mobileui.api.AdoptionAnimalViewModel
import com.petradar.mobileui.api.AdoptionRequest
import com.petradar.mobileui.utils.PetImageUrlResolver
import com.petradar.mobileui.viewmodel.AdoptionAnimalDetailViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionAnimalDetailScreen(
    viewModel: AdoptionAnimalDetailViewModel,
    currentUserId: Long,
    adoptSuccess: Boolean?,
    onAdoptAnimal: (AdoptionAnimalViewModel, AdoptionRequest) -> Unit,
    onApproveRequest: (Long) -> Unit,
    onChatWithRequester: (userId: Long, userName: String?) -> Unit,
    onChatWithOwner: () -> Unit,
    onEditAnimal: () -> Unit,
    onDeleteAnimal: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val animal by viewModel.animal.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    val additionalPhotos by viewModel.additionalPhotos.observeAsState(emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.observeAsState()
    LaunchedEffect(errorMessage) { errorMessage?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(adoptSuccess) {
        if (adoptSuccess == true) {
            snackbarHostState.showSnackbar("¡Solicitud de adopción enviada con éxito!")
            onBack()
        }
    }

    // Full-screen photo viewer state: index into the merged photos list (main = 0, additional = 1+)
    var fullscreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    var showAdoptForm by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRequestsSheet by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<AdoptionRequest?>(null) }
    val isOwner = animal != null && currentUserId > 0 && animal?.shelterId == currentUserId

    val scope = rememberCoroutineScope()

    // ── Adoption form state ─────────────────────────────────────────────────
    var adoptFullName by remember { mutableStateOf("") }
    var adoptPhone by remember { mutableStateOf("") }
    var adoptAddress by remember { mutableStateOf("") }

    // ── Map state for adoption form ─────────────────────────────────────────
    val defaultPoint = remember { GeoPoint(19.4326, -99.1332) }
    var adoptSelectedPoint by remember { mutableStateOf(defaultPoint) }
    var adoptMapView by remember { mutableStateOf<MapView?>(null) }
    var adoptIsGeocoding by remember { mutableStateOf(false) }
    var adoptLocationError by remember { mutableStateOf<String?>(null) }
    var adoptAddressError by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tryUseCurrentLocation(context) { point, error ->
                if (point != null) {
                    adoptSelectedPoint = point
                    adoptMapView?.controller?.setCenter(point)
                    adoptMapView?.controller?.setZoom(16.0)
                    adoptMapView?.invalidate()
                    scope.launch {
                        val addr = reverseGeocode(context, point.latitude, point.longitude)
                        if (addr.isNotBlank()) adoptAddress = addr
                    }
                } else adoptLocationError = error ?: "No se pudo obtener la ubicación."
            }
        } else adoptLocationError = "Se denegó el permiso de ubicación."
    }
    var adoptHousingType by remember { mutableStateOf<String?>(null) }
    var adoptHousingDropdownExpanded by remember { mutableStateOf(false) }
    var adoptHasYard by remember { mutableStateOf(false) }
    var adoptLivesWith by remember { mutableStateOf<String?>(null) }
    var adoptLivesWithDropdownExpanded by remember { mutableStateOf(false) }
    var adoptHasOtherAnimals by remember { mutableStateOf(false) }
    var adoptOtherAnimalsDesc by remember { mutableStateOf("") }
    var adoptPriorExperience by remember { mutableStateOf("") }
    var adoptWhyAdopt by remember { mutableStateOf("") }
    var adoptAcceptsVisits by remember { mutableStateOf(false) }
    var adoptSendsPhotos by remember { mutableStateOf(false) }
    var adoptComments by remember { mutableStateOf("") }
    var adoptFormError by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog && animal != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar publicación") },
            text = { Text("¿Seguro que deseas eliminar la publicación de ${animal?.name ?: "este animal"}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDeleteAnimal() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Bottom sheet: adoption requests (owner only) ──────────────────────────
    if (showRequestsSheet && animal != null) {
        val requests = animal!!.adoptionRequests.orEmpty()
        ModalBottomSheet(
            onDismissRequest = { showRequestsSheet = false; selectedRequest = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val req = selectedRequest
                if (req == null) {
                    // ── Vista A: lista resumen ──────────────────────────────
                    Text(
                        "Solicitudes de adopción (${requests.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()

                    if (requests.isEmpty()) {
                        Text(
                            "No hay solicitudes aún.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        requests.forEach { item ->
                            Card(
                                onClick = { selectedRequest = item },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                item.name ?: "Usuario #${item.userId}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    if (!item.phoneNumber.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(item.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (!item.address.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(item.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── Vista B: detalle de una solicitud ───────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { selectedRequest = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                        Text(
                            "Detalle de solicitud",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(req.name ?: "Usuario #${req.userId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            if (!req.phoneNumber.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(req.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (!req.address.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(req.address, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (!req.houseType.isNullOrBlank()) {
                                InfoRow("Vivienda", buildString {
                                    append(req.houseType)
                                    if (req.hasGarden) append(" · Con jardín")
                                })
                            }
                            if (!req.livesWith.isNullOrBlank()) {
                                InfoRow("Convive con", req.livesWith)
                            }
                            InfoRow("Otras mascotas", if (req.hasOtherPets) "Sí" else "No")
                            if (!req.otherPetsDescription.isNullOrBlank()) {
                                InfoRow("¿Cuáles?", req.otherPetsDescription)
                            }
                            val experienceText = req.experience ?: req.previousExperience
                            if (!experienceText.isNullOrBlank()) {
                                InfoRow("Experiencia", experienceText)
                            }
                            if (!req.adoptionReason.isNullOrBlank()) {
                                InfoRow("Motivo", req.adoptionReason)
                            }
                            InfoRow("Acepta visitas", if (req.acceptsFollowUpVisits) "Sí" else "No")
                            InfoRow("Enviará fotos", if (req.sendsPhotos) "Sí" else "No")
                            if (!req.additionalComments.isNullOrBlank()) {
                                InfoRow("Comentarios", req.additionalComments)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onApproveRequest(req.userId)
                            selectedRequest = null
                            showRequestsSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Aprobar solicitud")
                    }
                    OutlinedButton(
                        onClick = {
                            onChatWithRequester(req.userId, req.name)
                            selectedRequest = null
                            showRequestsSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Chatear")
                    }
                }
            }
        }
    }

    if (showAdoptForm && animal != null) {
        ModalBottomSheet(
            onDismissRequest = { showAdoptForm = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Solicitud de adopción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Animal: ${animal?.name ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()

                // ── Datos personales ─────────────────────────────────────
                Text("Datos personales", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = adoptFullName,
                    onValueChange = { adoptFullName = it; adoptFormError = null },
                    label = { Text("Nombre completo *") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = adoptPhone,
                    onValueChange = { adoptPhone = it; adoptFormError = null },
                    label = { Text("Teléfono *") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // ── Mapa de dirección ────────────────────────────────────
                Text("Dirección", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Toca el mapa para indicar tu ubicación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    AndroidView(
                        factory = { ctx ->
                            val map = MapView(ctx).apply {
                                Configuration.getInstance().userAgentValue = ctx.packageName
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(12.0)
                                controller.setCenter(adoptSelectedPoint)
                            }
                            map.overlays.add(
                                MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                        p?.let { pt ->
                                            adoptSelectedPoint = pt
                                            adoptLocationError = null
                                            map.controller.setCenter(pt)
                                            map.controller.setZoom(16.0)
                                            map.invalidate()
                                            scope.launch {
                                                val addr = reverseGeocode(ctx, pt.latitude, pt.longitude)
                                                if (addr.isNotBlank()) adoptAddress = addr
                                            }
                                        }
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                                })
                            )
                            map.setOnTouchListener { v, event ->
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN,
                                    MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                                    MotionEvent.ACTION_UP,
                                    MotionEvent.ACTION_CANCEL -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(false)
                                        v.performClick()
                                    }
                                }
                                false
                            }
                            adoptMapView = map
                            map
                        },
                        update = { map ->
                            adoptMapView = map
                            map.overlays.clear()
                            map.overlays.add(
                                MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                        p?.let { pt ->
                                            adoptSelectedPoint = pt
                                            adoptLocationError = null
                                            map.controller.setCenter(pt)
                                            map.controller.setZoom(16.0)
                                            map.invalidate()
                                            scope.launch {
                                                val addr = reverseGeocode(context, pt.latitude, pt.longitude)
                                                if (addr.isNotBlank()) adoptAddress = addr
                                            }
                                        }
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                                })
                            )
                            map.overlays.add(
                                Marker(map).apply {
                                    position = adoptSelectedPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Mi ubicación"
                                }
                            )
                            map.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
                TextButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            tryUseCurrentLocation(context) { point, error ->
                                if (point != null) {
                                    adoptSelectedPoint = point
                                    adoptMapView?.controller?.setCenter(point)
                                    adoptMapView?.controller?.setZoom(16.0)
                                    adoptMapView?.invalidate()
                                    scope.launch {
                                        val addr = reverseGeocode(context, point.latitude, point.longitude)
                                        if (addr.isNotBlank()) adoptAddress = addr
                                    }
                                } else adoptLocationError = error ?: "No se pudo obtener la ubicación."
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                ) {
                    Icon(Icons.Default.MyLocation, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Usar mi ubicación actual")
                }
                adoptLocationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(
                    value = adoptAddress,
                    onValueChange = { adoptAddress = it; adoptAddressError = null },
                    label = { Text("Dirección de referencia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = adoptAddressError != null,
                    supportingText = adoptAddressError?.let { err -> { Text(err) } },
                    trailingIcon = {
                        if (adoptIsGeocoding) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = {
                                    if (adoptAddress.isBlank()) return@IconButton
                                    scope.launch {
                                        adoptIsGeocoding = true
                                        adoptAddressError = null
                                        val pt = forwardGeocode(context, adoptAddress)
                                        adoptIsGeocoding = false
                                        if (pt != null) {
                                            adoptSelectedPoint = pt
                                            adoptMapView?.controller?.setCenter(pt)
                                            adoptMapView?.controller?.setZoom(16.0)
                                            adoptMapView?.invalidate()
                                        } else {
                                            adoptAddressError = "No se encontró la dirección."
                                        }
                                    }
                                },
                                enabled = adoptAddress.isNotBlank()
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (adoptAddress.isBlank()) return@KeyboardActions
                        scope.launch {
                            adoptIsGeocoding = true
                            adoptAddressError = null
                            val pt = forwardGeocode(context, adoptAddress)
                            adoptIsGeocoding = false
                            if (pt != null) {
                                adoptSelectedPoint = pt
                                adoptMapView?.controller?.setCenter(pt)
                                adoptMapView?.controller?.setZoom(16.0)
                                adoptMapView?.invalidate()
                            } else {
                                adoptAddressError = "No se encontró la dirección."
                            }
                        }
                    })
                )

                // ── Vivienda ─────────────────────────────────────────────
                Text("Vivienda", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                val housingOptions = listOf("Casa", "Departamento", "Otro")
                ExposedDropdownMenuBox(
                    expanded = adoptHousingDropdownExpanded,
                    onExpandedChange = { adoptHousingDropdownExpanded = !adoptHousingDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = adoptHousingType ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de vivienda") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = adoptHousingDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = adoptHousingDropdownExpanded,
                        onDismissRequest = { adoptHousingDropdownExpanded = false }
                    ) {
                        housingOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { adoptHousingType = option; adoptHousingDropdownExpanded = false }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("¿Tiene patio o jardín?")
                    Switch(checked = adoptHasYard, onCheckedChange = { adoptHasYard = it })
                }

                // ── Convivencia ──────────────────────────────────────────
                Text("Convivencia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                val livesWithOptions = listOf("Solo/a", "Familia", "Compañeros/as")
                ExposedDropdownMenuBox(
                    expanded = adoptLivesWithDropdownExpanded,
                    onExpandedChange = { adoptLivesWithDropdownExpanded = !adoptLivesWithDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = adoptLivesWith ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vive con") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = adoptLivesWithDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = adoptLivesWithDropdownExpanded,
                        onDismissRequest = { adoptLivesWithDropdownExpanded = false }
                    ) {
                        livesWithOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { adoptLivesWith = option; adoptLivesWithDropdownExpanded = false }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("¿Hay otros animales en casa?")
                    Switch(checked = adoptHasOtherAnimals, onCheckedChange = { adoptHasOtherAnimals = it })
                }
                if (adoptHasOtherAnimals) {
                    OutlinedTextField(
                        value = adoptOtherAnimalsDesc,
                        onValueChange = { adoptOtherAnimalsDesc = it },
                        label = { Text("¿Cuáles?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Experiencia y motivación ─────────────────────────────
                Text("Experiencia y motivación", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = adoptPriorExperience,
                    onValueChange = { adoptPriorExperience = it },
                    label = { Text("Experiencia previa con animales") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = adoptWhyAdopt,
                    onValueChange = { adoptWhyAdopt = it; adoptFormError = null },
                    label = { Text("¿Por qué desea adoptar este animal? *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // ── Compromiso ───────────────────────────────────────────
                Text("Compromiso de seguimiento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Acepto visitas de seguimiento")
                    Switch(checked = adoptAcceptsVisits, onCheckedChange = { adoptAcceptsVisits = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enviaré fotos periódicamente")
                    Switch(checked = adoptSendsPhotos, onCheckedChange = { adoptSendsPhotos = it })
                }

                // ── Comentarios adicionales ──────────────────────────────
                OutlinedTextField(
                    value = adoptComments,
                    onValueChange = { adoptComments = it },
                    label = { Text("Comentarios adicionales") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                adoptFormError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        when {
                            adoptFullName.isBlank() -> adoptFormError = "El nombre completo es requerido"
                            adoptPhone.isBlank() -> adoptFormError = "El teléfono es requerido"
                            adoptWhyAdopt.isBlank() -> adoptFormError = "Por favor indica por qué deseas adoptar"
                            else -> {
                                animal?.let { a ->
                                    val request = AdoptionRequest(
                                        userId = currentUserId,
                                        name = adoptFullName,
                                        phoneNumber = adoptPhone,
                                        address = adoptAddress.takeIf { it.isNotBlank() },
                                        houseType = adoptHousingType,
                                        hasGarden = adoptHasYard,
                                        livesWith = adoptLivesWith,
                                        hasOtherPets = adoptHasOtherAnimals,
                                        otherPetsDescription = adoptOtherAnimalsDesc.takeIf { adoptHasOtherAnimals && it.isNotBlank() },
                                        experience = adoptPriorExperience.takeIf { it.isNotBlank() },
                                        adoptionReason = adoptWhyAdopt.takeIf { it.isNotBlank() },
                                        acceptsFollowUpVisits = adoptAcceptsVisits,
                                        sendsPhotos = adoptSendsPhotos,
                                        additionalComments = adoptComments.takeIf { it.isNotBlank() }
                                    )
                                    onAdoptAnimal(a, request)
                                }
                                showAdoptForm = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Favorite, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar solicitud", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { showAdoptForm = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
            }
        }
    }

    // Full-screen photo overlay
    AnimatedVisibility(
        visible = fullscreenPhotoUrl != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            fullscreenPhotoUrl?.let { url ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(
                onClick = { fullscreenPhotoUrl = null },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(animal?.name ?: "Detalle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = onEditAnimal) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar publicación")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar publicación", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (animal != null && currentUserId > 0) {
                val hasSubmittedRequest = !isOwner &&
                    animal?.adoptionRequests?.any { it.userId == currentUserId } == true
                Surface(shadowElevation = 8.dp) {
                    when {
                        isOwner -> {
                            val requestCount = animal?.adoptionRequests?.size ?: 0
                            Button(
                                onClick = { showRequestsSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .navigationBarsPadding()
                            ) {
                                Icon(Icons.Default.People, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (requestCount > 0) "Ver solicitudes de adopción ($requestCount)"
                                    else "Ver solicitudes de adopción"
                                )
                            }
                        }
                        hasSubmittedRequest -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .navigationBarsPadding(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onChatWithOwner,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Chat, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Chatear con el dueño")
                                }
                                Text(
                                    text = "✓ Tu solicitud de adopción fue enviada",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = { showAdoptForm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .navigationBarsPadding()
                            ) {
                                Icon(Icons.Default.Favorite, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Adoptar")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading && animal == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val a = animal ?: return@Scaffold

        val mainPhotoUrl = PetImageUrlResolver.adoptionMainPictureEndpoint(a.id)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Hero photo ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .clickable { fullscreenPhotoUrl = mainPhotoUrl },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mainPhotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = a.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                // Tap hint
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Toca para ampliar", color = Color.White, fontSize = 11.sp)
                }
                // Status badge
                val statusLabel = when (a.status?.lowercase()) {
                    "available" -> "Disponible"
                    "adopted" -> "Adoptado"
                    "reserved" -> "Reservado"
                    else -> a.status ?: ""
                }
                val statusColor = when (a.status?.lowercase()) {
                    "available" -> MaterialTheme.colorScheme.primary
                    "adopted" -> MaterialTheme.colorScheme.tertiary
                    "reserved" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Additional photos strip ──────────────────────────────────
            if (additionalPhotos.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Más fotos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // The API returns filenames; we build the full URL the same way PetDetailScreen does.
                        itemsIndexed(additionalPhotos) { _, photoName ->
                            val photoUrl = PetImageUrlResolver.adoptionAdditionalPhotoUrl(a.id, photoName)
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { fullscreenPhotoUrl = photoUrl }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            // ── Animal info ──────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name + species
                val speciesLabel = when (a.species?.lowercase()) {
                    "dog" -> "Perro"
                    "cat" -> "Gato"
                    else -> a.species ?: "Desconocido"
                }
                Text(
                    text = a.name ?: "Sin nombre",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = speciesLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Info chips row
                val ageText = a.approximateAge?.let { age ->
                    if (age < 1.0 && age > 0.0) {
                        val months = (age * 12).toInt()
                        "$months ${if (months == 1) "mes" else "meses"}"
                    } else {
                        val years = if (age == age.toLong().toDouble()) age.toLong().toString() else age.toString()
                        "$years ${if (age == 1.0) "año" else "años"}"
                    }
                }
                val sexLabel = when (a.sex?.lowercase()) {
                    "male" -> "Macho"
                    "female" -> "Hembra"
                    else -> "Desconocido"
                }
                val sizeLabel = when (a.size?.lowercase()) {
                    "small" -> "Pequeño"
                    "medium" -> "Mediano"
                    "large" -> "Grande"
                    else -> null
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (ageText != null) InfoChip(label = ageText, icon = Icons.Default.Cake)
                    InfoChip(label = sexLabel, icon = Icons.Default.Person)
                    if (sizeLabel != null) InfoChip(label = sizeLabel, icon = Icons.Default.Straighten)
                }

                // Details card
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!a.breed.isNullOrBlank()) InfoRow("Raza", a.breed)
                        if (!a.color.isNullOrBlank()) InfoRow("Color", a.color)
                        if (a.weight != null) InfoRow("Peso", "${a.weight} kg")
                    }
                }

                // Description
                if (!a.description.isNullOrBlank()) {
                    Text("Descripción", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(a.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }

                // Personality
                if (!a.personality.isNullOrBlank()) {
                    Text("Personalidad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(a.personality, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }

                // Health & compatibility
                val healthItems = buildList {
                    if (a.isNeutered == true) add("Esterilizado/Castrado")
                    if (a.isVaccinated == true) add("Vacunado")
                    if (a.goodWithKids == true) add("Bueno con niños")
                    if (a.goodWithDogs == true) add("Bueno con perros")
                    if (a.goodWithCats == true) add("Bueno con gatos")
                }
                if (healthItems.isNotEmpty()) {
                    Text("Salud y compatibilidad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            healthItems.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(item, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // Special care
                if (a.needsSpecialCare == true && !a.specialCareDetails.isNullOrBlank()) {
                    Text("Cuidados especiales", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Text(a.specialCareDetails, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Bottom spacer so content isn't hidden behind the bottom button
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
