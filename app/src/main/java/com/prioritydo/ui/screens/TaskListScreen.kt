package com.prioritydo.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prioritydo.data.model.Task
import com.prioritydo.service.FloatingBubbleService
import com.prioritydo.ui.components.CategoryTabs
import com.prioritydo.ui.components.DraggableList
import com.prioritydo.ui.theme.priorityColor
import com.prioritydo.util.OverlayPermissionHelper
import com.prioritydo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val tasksWithoutDate by viewModel.tasksWithoutDate.collectAsState()
    val tasksWithDate by viewModel.tasksWithDate.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val highestPriorityTask by viewModel.highestPriorityTask.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Diálogo de ayuda para permisos de superposición
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permiso de Burbuja Flotante", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Para mostrar la burbuja sobre otras apps, Android necesita el permiso 'Mostrar sobre otras aplicaciones'.\n\n" +
                    "⚠️ Si ya lo activaste en los Ajustes (muy común en Xiaomi/HyperOS, Samsung o Huawei), pulsa 'Ya lo activé, iniciar burbuja' para activarla directamente."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        startBubbleService(context)
                    }
                ) {
                    Text("Ya lo activé, iniciar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPermissionDialog = false
                        OverlayPermissionHelper.openOverlaySettings(context)
                    }
                ) {
                    Text("Abrir Ajustes")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PriorityDo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    // Botón para activar/desactivar la burbuja flotante tipo Messenger
                    IconButton(
                        onClick = {
                            toggleFloatingBubble(
                                context = context,
                                onShowPermissionDialog = { showPermissionDialog = true }
                            )
                        }
                    ) {
                        val bubbleColor = highestPriorityTask?.let { priorityColor(it.priority) }
                            ?: MaterialTheme.colorScheme.primary
                        Icon(
                            imageVector = Icons.Default.BubbleChart,
                            contentDescription = "Activar burbuja flotante",
                            tint = bubbleColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Botón circular con "+" (Agregar) para crear una tarea nueva
            FloatingActionButton(
                onClick = onNavigateToCreate,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar tarea",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pestañas superiores con animación y sincronizadas con el swipe horizontal
            CategoryTabs(
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            // Deslizamiento rápido (swipe) entre categorías: 0. Todas, 1. Sin fecha, 2. Con fecha
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        // 1. Vista combinada ("Todas"): todas juntas por prioridad, reacomodables con arrastre
                        DraggableList(
                            tasks = allTasks,
                            onReorder = { reordered, movedTaskId ->
                                viewModel.reorderTasks(reordered, movedTaskId)
                            },
                            onDeleteOrComplete = { task ->
                                viewModel.completeOrDeleteTask(task)
                            },
                            onTaskClick = { task ->
                                onNavigateToEdit(task.id)
                            },
                            emptyMessage = "No hay tareas pendientes"
                        )
                    }
                    1 -> {
                        // 2. Sin fecha: tareas sin fecha asignada, reacomodar prioridad con arrastre
                        DraggableList(
                            tasks = tasksWithoutDate,
                            onReorder = { reordered, movedTaskId ->
                                viewModel.reorderTasks(reordered, movedTaskId)
                            },
                            onDeleteOrComplete = { task ->
                                viewModel.completeOrDeleteTask(task)
                            },
                            onTaskClick = { task ->
                                onNavigateToEdit(task.id)
                            },
                            emptyMessage = "No hay tareas sin fecha pendientes"
                        )
                    }
                    2 -> {
                        // 3. Con fecha: ventana de 1 o 2 meses próxima
                        DraggableList(
                            tasks = tasksWithDate,
                            onReorder = { reordered, movedTaskId ->
                                viewModel.reorderTasks(reordered, movedTaskId)
                            },
                            onDeleteOrComplete = { task ->
                                viewModel.completeOrDeleteTask(task)
                            },
                            onTaskClick = { task ->
                                onNavigateToEdit(task.id)
                            },
                            emptyMessage = "No hay tareas próximas con fecha"
                        )
                    }
                }
            }
        }
    }
}

private fun toggleFloatingBubble(
    context: Context,
    onShowPermissionDialog: () -> Unit
) {
    if (FloatingBubbleService.isRunning) {
        val serviceIntent = Intent(context, FloatingBubbleService::class.java)
        context.stopService(serviceIntent)
        Toast.makeText(context, "Burbuja flotante desactivada", Toast.LENGTH_SHORT).show()
        return
    }

    if (!OverlayPermissionHelper.canDrawOverlays(context)) {
        onShowPermissionDialog()
    } else {
        startBubbleService(context)
    }
}

private fun startBubbleService(context: Context) {
    try {
        val serviceIntent = Intent(context, FloatingBubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        Toast.makeText(context, "Burbuja flotante activada", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al iniciar burbuja: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
