package com.prioritydo.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prioritydo.data.model.Task
import com.prioritydo.ui.components.PriorityColorBar
import com.prioritydo.ui.theme.priorityColor
import com.prioritydo.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: Long?,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allTasks by viewModel.allTasks.collectAsState()
    val existingTask = remember(taskId, allTasks) {
        if (taskId != null && taskId != -1L) allTasks.firstOrNull { it.id == taskId } else null
    }

    var title by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(Task.PRIORITY_BLUE) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var repeatType by remember { mutableStateOf<String?>(null) }
    var repeatInterval by remember { mutableIntStateOf(1) }

    LaunchedEffect(existingTask) {
        existingTask?.let {
            title = it.title
            priority = it.priority
            selectedDateMillis = it.date
            selectedTime = it.time
            repeatType = it.repeatType
            repeatInterval = it.repeatInterval
        }
    }

    val isEditing = existingTask != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Editar tarea" else "Nueva tarea",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                if (isEditing && existingTask != null) {
                                    viewModel.updateTask(
                                        existingTask.copy(
                                            title = title.trim(),
                                            priority = priority,
                                            date = selectedDateMillis,
                                            time = selectedTime,
                                            repeatType = repeatType,
                                            repeatInterval = repeatInterval
                                        )
                                    )
                                } else {
                                    viewModel.addTask(
                                        title = title,
                                        priority = priority,
                                        date = selectedDateMillis,
                                        time = selectedTime,
                                        repeatType = repeatType,
                                        repeatInterval = repeatInterval
                                    )
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(
                            text = "Guardar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Campo de texto largo en la parte superior para escribir la tarea
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Escribe tu tarea pendiente...") },
                placeholder = { Text("Ej: Comprar boletos de avión") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            // 2. Fila con selector de prioridad (izquierda) y hora 00:00 (derecha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Debajo a la izquierda: Selector de prioridad por color
                Box(modifier = Modifier.weight(1.5f)) {
                    PriorityColorBar(
                        selectedPriority = priority,
                        onPrioritySelected = { priority = it }
                    )
                }

                // Debajo a la derecha: Campo de hora (formato 00:00)
                Column(
                    modifier = Modifier.weight(0.8f)
                ) {
                    Text(
                        text = "Hora (00:00)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .clickable {
                                val calendar = Calendar.getInstance()
                                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                                val currentMin = calendar.get(Calendar.MINUTE)

                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                                    },
                                    currentHour,
                                    currentMin,
                                    true
                                ).show()
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedTime ?: "00:00",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (selectedTime != null) {
                                IconButton(
                                    onClick = { selectedTime = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Borrar hora",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 3. Opciones de Fecha
            Column {
                Text(
                    text = "Fecha programada",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .clickable {
                            val calendar = Calendar.getInstance()
                            if (selectedDateMillis != null) {
                                calendar.timeInMillis = selectedDateMillis!!
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val sel = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedDateMillis = sel.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateText = if (selectedDateMillis != null) {
                                val fmt = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
                                fmt.format(Date(selectedDateMillis!!))
                            } else {
                                "Sin fecha (categoría: Sin fecha)"
                            }
                            Text(
                                text = dateText,
                                fontSize = 14.sp,
                                fontWeight = if (selectedDateMillis != null) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        if (selectedDateMillis != null) {
                            IconButton(
                                onClick = { selectedDateMillis = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Quitar fecha",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Opciones de Repetición
            Column {
                Text(
                    text = "Repetición",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val repeatOptions = listOf(
                    null to "No repetir",
                    Task.REPEAT_DAILY to "Diaria",
                    Task.REPEAT_WEEKLY to "Semanal",
                    Task.REPEAT_MONTHLY to "Mensual",
                    Task.REPEAT_YEARLY to "Anual"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeatOptions.forEach { (type, label) ->
                        val isSelected = repeatType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { repeatType = type },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón grande de guardar
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        if (isEditing && existingTask != null) {
                            viewModel.updateTask(
                                existingTask.copy(
                                    title = title.trim(),
                                    priority = priority,
                                    date = selectedDateMillis,
                                    time = selectedTime,
                                    repeatType = repeatType,
                                    repeatInterval = repeatInterval
                                )
                            )
                        } else {
                            viewModel.addTask(
                                title = title,
                                priority = priority,
                                date = selectedDateMillis,
                                time = selectedTime,
                                repeatType = repeatType,
                                repeatInterval = repeatInterval
                            )
                        }
                        onNavigateBack()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isEditing) "Actualizar tarea" else "Crear tarea",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
