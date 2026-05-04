@file:OptIn(ExperimentalMaterial3Api::class)

package com.petradar.mobileui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.petradar.mobileui.api.MatchViewModel
import com.petradar.mobileui.api.ReportViewModel
import com.petradar.mobileui.utils.PetImageUrlResolver
import com.petradar.mobileui.viewmodel.MyReportsViewModel
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    viewModel: MyReportsViewModel,
    userId: Long,
    onBack: () -> Unit,
    onNewReport: () -> Unit = {},
    onEditReport: (ReportViewModel) -> Unit = {},
    onDeleteReport: (Long) -> Unit = {},
    onDismissReport: (Long) -> Unit = {},
    onOpenMatchChat: (matchId: Long, otherUserId: Long, matchTitle: String, lostReportId: Long, lostPetLabel: String, strayReportId: Long) -> Unit = { _, _, _, _, _, _ -> }
) {
    val reports by viewModel.reports.observeAsState(emptyList())
    val matchesByReportId by viewModel.matchesByReportId.observeAsState(emptyMap())
    val unreadCountByMatchId by viewModel.unreadCountByMatchId.observeAsState(emptyMap())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val deleteSuccess by viewModel.deleteSuccess.observeAsState()
    val dismissSuccess by viewModel.dismissSuccess.observeAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isLoading) {
        if (!isLoading) isRefreshing = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess != null) {
            snackbarHostState.showSnackbar("Reporte eliminado correctamente")
            viewModel.clearDeleteSuccess()
        }
    }

    LaunchedEffect(dismissSuccess) {
        if (dismissSuccess != null) {
            snackbarHostState.showSnackbar("Reporte descartado")
            viewModel.clearDismissSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis reportes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewReport) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo reporte")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadReports(userId)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && reports.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                reports.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Report, contentDescription = null)
                            Spacer(Modifier.height(8.dp))
                            Text("Aún no tienes reportes")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reports, key = { it.id }) { report ->
                            ReportCard(
                                report = report,
                                matches = matchesByReportId[report.id] ?: emptyList(),
                                currentUserId = userId,
                                unreadCountByMatchId = unreadCountByMatchId,
                                onEdit = { onEditReport(report) },
                                onDelete = { onDeleteReport(report.id) },
                                onDismiss = { onDismissReport(report.id) },
                                onOpenMatchChat = onOpenMatchChat
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun translateReportType(type: String?): String = when (type?.lowercase()) {
    "lost" -> "Mascota perdida"
    "stray" -> "Animal callejero"
    "found" -> "Mascota encontrada"
    else -> type ?: "Reporte"
}

private fun translateReportStatus(status: String?): String = when (status?.lowercase()) {
    "active" -> "Activo"
    "resolved" -> "Resuelto"
    "adopted" -> "Adoptado"
    "cancelled" -> "Cancelado"
    "dismissed" -> "Descartado"
    else -> status ?: "Sin estado"
}

private fun translateReportSpecies(species: String?): String = when (species?.lowercase()) {
    "dog" -> "Perro"
    "cat" -> "Gato"
    else -> species ?: "-"
}

private fun formatReportDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "Sin fecha"
    return try {
        val instant = Instant.parse(dateStr)
        val zdt = instant.atZone(ZoneId.systemDefault())
        "%02d/%02d/%d".format(zdt.dayOfMonth, zdt.monthValue, zdt.year)
    } catch (_: Exception) {
        dateStr
    }
}

private fun lostPetLabel(lostReport: ReportViewModel): String {
    val species = translateReportSpecies(lostReport.species)
    val breed = lostReport.breed?.takeIf { it.isNotBlank() }
    return if (breed != null) "$species · $breed" else species
}

@Composable
private fun ReportCard(
    report: ReportViewModel,
    matches: List<MatchViewModel>,
    currentUserId: Long,
    unreadCountByMatchId: Map<Long, Int>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onOpenMatchChat: (matchId: Long, otherUserId: Long, matchTitle: String, lostReportId: Long, lostPetLabel: String, strayReportId: Long) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDismissDialog by remember { mutableStateOf(false) }
    var showMatchDialog by remember { mutableStateOf(false) }

    val totalUnread = remember(matches, unreadCountByMatchId) {
        matches.sumOf { unreadCountByMatchId[it.id] ?: 0 }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Eliminar reporte?") },
            text = { Text("Esta acción no se puede deshacer. ¿Deseas eliminar este reporte definitivamente?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDismissDialog) {
        AlertDialog(
            onDismissRequest = { showDismissDialog = false },
            icon = {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            title = { Text("¿Descartar reporte?") },
            text = { Text("El reporte quedará marcado como descartado. Puedes cambiarlo después editándolo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDismissDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDismissDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showMatchDialog && matches.size > 1) {
        MatchListDialog(
            matches = matches,
            currentUserId = currentUserId,
            unreadCountByMatchId = unreadCountByMatchId,
            onDismiss = { showMatchDialog = false },
            onOpenChat = { matchId, otherUserId, title, lostReportId, petLabel, strayReportId ->
                showMatchDialog = false
                onOpenMatchChat(matchId, otherUserId, title, lostReportId, petLabel, strayReportId)
            }
        )
    }

    val context = LocalContext.current
    val photoUrl: String = PetImageUrlResolver.reportMainPictureEndpoint(report.id)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Report,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto del reporte",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${translateReportType(report.reportType)} · ${translateReportStatus(report.reportStatus)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (report.reportType?.lowercase() == "lost" &&
                        report.reportStatus?.lowercase() == "active") {
                        IconButton(onClick = { showDismissDialog = true }) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Descartar reporte",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar reporte",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(text = "Especie: ${translateReportSpecies(report.species)}")
                Text(text = "Dirección: ${report.addressText ?: "No especificada"}")
                Text(text = "Fecha: ${formatReportDate(report.reportDate)}")
                report.description?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }

                if (matches.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgedBox(
                            badge = {
                                if (totalUnread > 0) {
                                    Badge { Text(totalUnread.toString()) }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (matches.size == 1) {
                                        val match = matches.first()
                                        val otherUserId = if (match.lostReport.userId == currentUserId)
                                            match.strayReport.userId
                                        else
                                            match.lostReport.userId
                                        onOpenMatchChat(
                                            match.id,
                                            otherUserId,
                                            matchChatTitle(match, currentUserId),
                                            match.lostReport.id,
                                            lostPetLabel(match.lostReport),
                                            match.strayReport.id
                                        )
                                    } else {
                                        showMatchDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.size(6.dp))
                                val label = if (matches.size == 1) "1 coincidencia"
                                            else "${matches.size} coincidencias"
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (totalUnread > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Forum,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (totalUnread == 1) "1 sin leer" else "$totalUnread sin leer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun matchChatTitle(match: MatchViewModel, currentUserId: Long): String {
    val myReport = if (match.lostReport.userId == currentUserId) match.lostReport else match.strayReport
    val otherReport = if (match.lostReport.userId == currentUserId) match.strayReport else match.lostReport
    return "${translateReportType(myReport.reportType)} · ${translateReportType(otherReport.reportType)}"
}

@Composable
private fun MatchListDialog(
    matches: List<MatchViewModel>,
    currentUserId: Long,
    unreadCountByMatchId: Map<Long, Int>,
    onDismiss: () -> Unit,
    onOpenChat: (matchId: Long, otherUserId: Long, title: String, lostReportId: Long, lostPetLabel: String, strayReportId: Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Pets, contentDescription = null)
        },
        title = { Text("Coincidencias encontradas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                matches.forEach { match ->
                    val otherReport = if (match.lostReport.userId == currentUserId)
                        match.strayReport else match.lostReport
                    val otherUserId = otherReport.userId
                    val title = matchChatTitle(match, currentUserId)
                    val status = translateMatchStatus(match.status)
                    val unreadCount = unreadCountByMatchId[match.id] ?: 0

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (unreadCount > 0) {
                                    Badge {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                            Text(
                                "Estado: $status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            match.distanceInKM?.let {
                                Text(
                                    "Distancia: ${"%.1f".format(it)} km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = {
                                    onOpenChat(
                                        match.id,
                                        otherUserId,
                                        title,
                                        match.lostReport.id,
                                        lostPetLabel(match.lostReport),
                                        match.strayReport.id
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Chatear")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

private fun translateMatchStatus(status: String?): String = when (status?.lowercase()) {
    "pending" -> "Pendiente"
    "confirmed" -> "Confirmado"
    "dismissed" -> "Descartado"
    else -> status ?: "Desconocido"
}