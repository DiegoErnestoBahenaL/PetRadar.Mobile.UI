@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.petradar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.api.AdoptionAnimalViewModel
import com.example.petradar.utils.PetImageUrlResolver
import com.example.petradar.viewmodel.AdoptionAnimalListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionAnimalsScreen(
    viewModel: AdoptionAnimalListViewModel,
    currentUserId: Long,
    onAddAnimal: () -> Unit,
    onAnimalClick: (AdoptionAnimalViewModel) -> Unit,
    onAdoptAnimal: (AdoptionAnimalViewModel) -> Unit,
    onEditAnimal: (AdoptionAnimalViewModel) -> Unit,
    onDeleteAnimal: (AdoptionAnimalViewModel) -> Unit,
    onBack: () -> Unit
) {
    val animals by viewModel.animals.observeAsState(emptyList())

    // Show all Available animals + all the user's own (regardless of status), deduplicated
    val displayAnimals = remember(animals, currentUserId) {
        animals.filter {
            it.status.equals("Available", ignoreCase = true) || it.shelterId == currentUserId
        }.distinctBy { it.id }
    }

    val isLoadingState by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingState
    val errorMessage by viewModel.errorMessage.observeAsState()
    val adoptSuccess by viewModel.adoptSuccess.observeAsState(null)

    var animalToAdopt by remember { mutableStateOf<AdoptionAnimalViewModel?>(null) }
    var showAdoptDialog by remember { mutableStateOf(false) }
    var animalToDelete by remember { mutableStateOf<AdoptionAnimalViewModel?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) { if (!isLoading) isRefreshing = false }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(adoptSuccess) {
        if (adoptSuccess == true) {
            snackbarHostState.showSnackbar("¡Solicitud de adopción enviada con éxito!")
        }
    }

    // Adopt confirmation dialog
    if (showAdoptDialog && animalToAdopt != null) {
        AlertDialog(
            onDismissRequest = { showAdoptDialog = false; animalToAdopt = null },
            icon = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Confirmar adopción") },
            text = { Text("¿Deseas iniciar la adopción de ${animalToAdopt?.name ?: "este animal"}?") },
            confirmButton = {
                TextButton(onClick = {
                    animalToAdopt?.let(onAdoptAnimal)
                    showAdoptDialog = false
                    animalToAdopt = null
                }) { Text("Adoptar") }
            },
            dismissButton = {
                TextButton(onClick = { showAdoptDialog = false; animalToAdopt = null }) { Text("Cancelar") }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && animalToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; animalToDelete = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar publicación") },
            text = { Text("¿Seguro que deseas eliminar la publicación de ${animalToDelete?.name ?: "este animal"}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        animalToDelete?.let(onDeleteAnimal)
                        showDeleteDialog = false
                        animalToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; animalToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adopciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                ExtendedFloatingActionButton(
                    onClick = onAddAnimal,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Subir animal") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; viewModel.loadAnimals() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && displayAnimals.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    displayAnimals.isEmpty() -> {
                        EmptyAdoptionMessage(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = displayAnimals, key = { it.id }) { animal ->
                                val isOwner = animal.shelterId == currentUserId
                                AdoptionAnimalGridCard(
                                    animal = animal,
                                    isOwner = isOwner,
                                    canAdopt = currentUserId > 0 && !isOwner,
                                    onClick = { onAnimalClick(animal) },
                                    onAdopt = { animalToAdopt = animal; showAdoptDialog = true },
                                    onEdit = { onEditAnimal(animal) },
                                    onDelete = { animalToDelete = animal; showDeleteDialog = true }
                                )
                            }
                            item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAdoptionMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No hay animales disponibles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Vuelve a intentar más tarde",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun AdoptionAnimalGridCard(
    animal: AdoptionAnimalViewModel,
    isOwner: Boolean,
    canAdopt: Boolean,
    onClick: () -> Unit,
    onAdopt: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val speciesLabel = when (animal.species?.lowercase()) {
        "dog" -> "Perro"
        "cat" -> "Gato"
        else -> animal.species ?: "Desconocido"
    }
    val statusLabel = when (animal.status?.lowercase()) {
        "available" -> "Disponible"
        "adopted" -> "Adoptado"
        "reserved" -> "Reservado"
        else -> animal.status ?: "Sin estado"
    }
    val statusColor = when (animal.status?.lowercase()) {
        "available" -> MaterialTheme.colorScheme.primary
        "adopted" -> MaterialTheme.colorScheme.tertiary
        "reserved" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    val animalBreed = animal.breed?.takeIf { it.isNotBlank() } ?: "Sin raza"
    val ageText = animal.approximateAge?.let { age ->
        if (age < 1.0 && age > 0.0) {
            val months = (age * 12).toInt()
            "$months ${if (months == 1) "mes" else "meses"}"
        } else {
            val years = if (age == age.toLong().toDouble()) age.toLong().toString() else age.toString()
            "$years ${if (age == 1.0) "año" else "años"}"
        }
    }

    val photoUrl = PetImageUrlResolver.adoptionMainPictureEndpoint(animal.id)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column {
            // Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .memoryCacheKey(photoUrl)
                        .diskCacheKey(photoUrl)
                        .build(),
                    contentDescription = animal.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                // Status badge
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // "Mi publicación" badge for owner
                if (isOwner) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Mía",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Info
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = animal.name ?: "Sin nombre",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = "$speciesLabel · $animalBreed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
                if (ageText != null) {
                    Text(
                        text = ageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Action buttons
            if (isOwner) {
                // Owner: edit + delete (icon-only)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Other users: adopt button
                Button(
                    onClick = onAdopt,
                    enabled = canAdopt && animal.status.equals("Available", ignoreCase = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp)
                ) {
                    Text("Adoptar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
