@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.petradar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.api.AdoptionAnimalViewModel
import com.example.petradar.utils.AdoptionPhotoStore
import com.example.petradar.viewmodel.AdoptionAnimalListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Screen that displays the list of adoption animals.
 *
 * Features:
 *  - Animal list in [LazyColumn] with photo (URL or default emoji), name, species, status and breed.
 *  - Floating action button "Nuevo animal" that navigates to the creation form.
 *  - Pull-to-refresh to reload the list from the API.
 *  - Confirmation dialog before deleting an animal.
 *  - Snackbar for displaying network errors.
 *
 * @param viewModel  ViewModel with the adoption animal list and deletion logic.
 * @param onAddAnimal Callback to navigate to create a new adoption animal.
 * @param onEditAnimal Callback to navigate to edit the given adoption animal.
 * @param onBack      Callback to return to the previous screen.
 */
fun AdoptionAnimalsScreen(
    viewModel: AdoptionAnimalListViewModel,
    onAddAnimal: () -> Unit,
    onEditAnimal: (AdoptionAnimalViewModel) -> Unit,
    onBack: () -> Unit
) {
    val animals by viewModel.animals.observeAsState(emptyList())
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

    if (showDeleteDialog && animalToDelete != null) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar animal") },
            text = { Text("¿Estás seguro de que deseas eliminar a ${animalToDelete?.name ?: "este animal"}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        animalToDelete?.let { a -> viewModel.deleteAnimal(a.id) }
                        animalToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancelar") }
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
                    text = { Text("Nuevo animal") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadAnimals()
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && animals.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    animals.isEmpty() -> {
                        EmptyAdoptionMessage(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = animals, key = { animal -> animal.id }) { animal ->
                                AdoptionAnimalCard(
                                    animal = animal,
                                    onEdit = { onEditAnimal(animal) },
                                    onDelete = { animalToDelete = animal; }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

/** Centred empty-state message shown when there are no adoption animals. */
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
            text = "No hay animales en adopción",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Toca + para agregar un nuevo animal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * Card for an individual adoption animal within the list.
 *
 * Displays the avatar (API URL or default emoji), the name, species/breed,
 * status and sex of the animal.
 * Includes edit (pencil) and delete (trash) buttons.
 */
@Composable
private fun AdoptionAnimalCard(
    animal: AdoptionAnimalViewModel,
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
    val speciesEmoji = if (animal.species?.lowercase() == "cat") "🐱" else "🐶"
    val animalBreed = animal.breed?.takeIf { it.isNotBlank() } ?: "Raza no especificada"

    // Resolve photo: local store first, then API URL
    val photoUriStr = remember(animal.id) {
        AdoptionPhotoStore.get(context, animal.id) ?: animal.photoURL?.takeIf { it.isNotBlank() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (photoUriStr != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUriStr).crossfade(true).build(),
                        contentDescription = animal.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(text = speciesEmoji, fontSize = 28.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = animal.name ?: "Sin nombre",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                // Species · Breed · Age
                val ageText = animal.approximateAge?.let { age ->
                    if (age < 1.0 && age > 0.0) {
                        val months = (age * 12).toInt()
                        "$months ${if (months == 1) "mes" else "meses"}"
                    } else {
                        val years = if (age == age.toLong().toDouble()) age.toLong().toString() else age.toString()
                        "$years ${if (age == 1.0) "año" else "años"}"
                    }
                }
                val subtitle = buildString {
                    append("$speciesLabel · $animalBreed")
                    if (ageText != null) append(" · $ageText")
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}





