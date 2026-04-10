@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.petradar.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.api.AdoptionAnimalViewModel
import com.example.petradar.utils.PetImageUrlResolver
import com.example.petradar.viewmodel.AdoptionAnimalDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionAnimalDetailScreen(
    viewModel: AdoptionAnimalDetailViewModel,
    currentUserId: Long,
    adoptSuccess: Boolean?,
    onAdoptAnimal: (AdoptionAnimalViewModel) -> Unit,
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

    var showAdoptDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isOwner = animal != null && currentUserId > 0 && animal?.shelterId == currentUserId

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

    if (showAdoptDialog && animal != null) {
        AlertDialog(
            onDismissRequest = { showAdoptDialog = false },
            icon = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Confirmar adopción") },
            text = { Text("¿Deseas iniciar la adopción de ${animal?.name ?: "este animal"}?") },
            confirmButton = {
                TextButton(onClick = {
                    animal?.let(onAdoptAnimal)
                    showAdoptDialog = false
                }) { Text("Adoptar") }
            },
            dismissButton = {
                TextButton(onClick = { showAdoptDialog = false }) { Text("Cancelar") }
            }
        )
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
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
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
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { showAdoptDialog = true },
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
                        .memoryCacheKey(mainPhotoUrl)
                        .diskCacheKey(mainPhotoUrl)
                        .build(),
                    contentDescription = a.name,
                    contentScale = ContentScale.Crop,
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
                                    .memoryCacheKey(photoUrl)
                                    .diskCacheKey(photoUrl)
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
