@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.petradar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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

    val myRequestsAnimals = remember(animals, currentUserId) {
        animals.filter { animal ->
            animal.adoptionRequests?.any { it.userId == currentUserId } == true
        }.distinctBy { it.id }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val activeAnimals = if (selectedTab == 0) displayAnimals else myRequestsAnimals

    val isLoadingState by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingState
    val errorMessage by viewModel.errorMessage.observeAsState()

    var animalToDelete by remember { mutableStateOf<AdoptionAnimalViewModel?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) { if (!isLoading) isRefreshing = false }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
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
            Column {
                TopAppBar(
                    title = { Text("Adopciones", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Todos") },
                        icon = { Icon(Icons.Default.Pets, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Mis solicitudes") },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
                    )
                }
            }
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
                    isLoading && activeAnimals.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    activeAnimals.isEmpty() -> {
                        if (selectedTab == 1) {
                            EmptyRequestsMessage(modifier = Modifier.align(Alignment.Center))
                        } else {
                            EmptyAdoptionMessage(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = activeAnimals, key = { it.id }) { animal ->
                                val isOwner = animal.shelterId == currentUserId
                                val hasSentRequest = !isOwner &&
                                    animal.adoptionRequests?.any { it.userId == currentUserId } == true
                                AdoptionAnimalGridCard(
                                    animal = animal,
                                    isOwner = isOwner,
                                    hasSentRequest = hasSentRequest,
                                    onClick = { onAnimalClick(animal) },
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
private fun EmptyRequestsMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sin solicitudes enviadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Aquí verás los animales a los que hayas solicitado adoptar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
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
    hasSentRequest: Boolean,
    onClick: () -> Unit,
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
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = animal.name,
                    contentScale = ContentScale.Fit,
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
                // Badges y botones de acción en la esquina superior izquierda
                when {
                    isOwner -> {
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
                    hasSentRequest -> {
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Solicitado",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                if (isOwner) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = onEdit,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = onDelete,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
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
        }
    }
}
