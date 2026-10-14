package com.prioritydo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prioritydo.data.model.Task
import com.prioritydo.ui.theme.priorityNoteBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    onDeleteRequested: (Task) -> Unit,
    onClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val noteBg = priorityNoteBackground(task.priority)

    // Toda la nota coloreada de fondo, sin X.
    // Click corto: abrir/editar
    // Click largo (mantener presionado): solicitar confirmación de eliminación
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onClick(task) },
                onLongClick = { onDeleteRequested(task) }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = noteBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenido principal de la tarea (Título y metadatos)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Título en letra blanca resaltada
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadatos (Fecha, Hora, Repetición) en chips blancos translúcidos
                if (task.date != null || task.time != null || (task.repeatType != null && task.repeatType != Task.REPEAT_NONE)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (task.date != null) {
                            val formatter = SimpleDateFormat("dd MMM", Locale("es", "ES"))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = formatter.format(Date(task.date)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (!task.time.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = task.time,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (!task.repeatType.isNullOrBlank() && task.repeatType != Task.REPEAT_NONE) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = when (task.repeatType) {
                                            Task.REPEAT_DAILY -> "Diaria"
                                            Task.REPEAT_WEEKLY -> "Semanal"
                                            Task.REPEAT_MONTHLY -> "Mensual"
                                            Task.REPEAT_YEARLY -> "Anual"
                                            else -> task.repeatType
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Franja punteada en el borde derecho (zona de agarre para arrastrar)
            DragHandleDots(
                modifier = Modifier
                    .padding(start = 8.dp, end = 2.dp)
                    .width(18.dp)
            )
        }
    }
}

@Composable
fun DragHandleDots(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(vertical = 1.8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(3.8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f))
                )
                Box(
                    modifier = Modifier
                        .size(3.8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f))
                )
            }
        }
    }
}
