@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.petradar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.ui.theme.PetTeal40
import com.example.petradar.utils.PetPhotoStore
import com.example.petradar.viewmodel.PetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsScreen(
    viewModel: PetViewModel,
    userId: Long,
    onAddPet: () -> Unit,
    onEditPet: (UserPetViewModel) -> Unit,
    onBack: () -> Unit
) {
    val pets by viewModel.pets.observeAsState(emptyList())
    val isLoadingState by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingState ?: false
    val errorMessage by viewModel.errorMessage.observeAsState()

    var petToDelete by remember { mutableStateOf<UserPetViewModel?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Stop the refresh indicator once loading finishes
    LaunchedEffect(isLoading) { if (!isLoading) isRefreshing = false }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showDeleteDialog && petToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar mascota") },
            text = { Text("¿Estás seguro de que deseas eliminar a ${petToDelete?.name ?: "esta mascota"}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        petToDelete?.let { p -> viewModel.deletePet(p.id, p.userId) }
                        showDeleteDialog = false
                        petToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Mascotas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                ExtendedFloatingActionButton(
                    onClick = onAddPet,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva mascota") },
                    containerColor = PetTeal40,
                    contentColor = Color.White
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
                viewModel.loadPets(userId)
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && pets.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PetTeal40)
                    }
                    pets.isEmpty() -> {
                        EmptyPetsMessage(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = pets, key = { pet -> pet.id }) { pet ->
                                PetCard(
                                    pet = pet,
                                    onEdit = { onEditPet(pet) },
                                    onDelete = { petToDelete = pet; showDeleteDialog = true }
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

@Composable
private fun EmptyPetsMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = PetTeal40.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No tienes mascotas registradas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Toca + para agregar tu primera mascota",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun PetCard(
    pet: UserPetViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val speciesLabel = when (pet.species?.lowercase()) {
        "dog" -> "Perro"
        "cat" -> "Gato"
        else -> pet.species ?: "Desconocido"
    }
    val sexLabel = when (pet.sex?.lowercase()) {
        "male" -> "Macho"
        "female" -> "Hembra"
        else -> "No especificado"
    }
    val speciesEmoji = if (pet.species?.lowercase() == "cat") "🐱" else "🐶"
    val petBreed = pet.breed?.takeIf { it.isNotBlank() } ?: "Raza no especificada"

    // Resolve photo: local store first, then API URL
    val photoUriStr = remember(pet.id) {
        PetPhotoStore.get(context, pet.id) ?: pet.photoURL?.takeIf { it.isNotBlank() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(PetTeal40.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (photoUriStr != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUriStr).crossfade(true).build(),
                        contentDescription = pet.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(text = speciesEmoji, fontSize = 28.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pet.name ?: "Sin nombre", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "$speciesLabel · $petBreed", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(text = sexLabel, fontSize = 12.sp, color = PetTeal40)
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PetTeal40)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
