package com.prioritydo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.prioritydo.data.model.Task

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableList(
    tasks: List<Task>,
    onReorder: (List<Task>, Long?) -> Unit,
    onDeleteOrComplete: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    emptyMessage: String = "No hay tareas pendientes",
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 16.sp
            )
        }
        return
    }

    var localList by remember(tasks) { mutableStateOf(tasks) }
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(localList) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { item ->
                                offset.y.toInt() in item.offset..(item.offset + item.size)
                            }
                            ?.let { item ->
                                draggingItemIndex = item.index
                                draggingItemId = localList.getOrNull(item.index)?.id
                                dragOffsetY = 0f
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y

                        val currentIndex = draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                        val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == currentIndex } ?: return@detectDragGesturesAfterLongPress

                        val currentItemHeight = currentItemInfo.size.toFloat()

                        if (dragOffsetY > currentItemHeight * 0.7f && currentIndex < localList.size - 1) {
                            val targetIndex = currentIndex + 1
                            val mutable = localList.toMutableList()
                            val item = mutable.removeAt(currentIndex)
                            mutable.add(targetIndex, item)
                            localList = mutable
                            draggingItemIndex = targetIndex
                            dragOffsetY -= currentItemHeight
                        } else if (dragOffsetY < -currentItemHeight * 0.7f && currentIndex > 0) {
                            val targetIndex = currentIndex - 1
                            val mutable = localList.toMutableList()
                            val item = mutable.removeAt(currentIndex)
                            mutable.add(targetIndex, item)
                            localList = mutable
                            draggingItemIndex = targetIndex
                            dragOffsetY += currentItemHeight
                        }
                    },
                    onDragEnd = {
                        val finalIndex = draggingItemIndex
                        draggingItemIndex = null
                        val movedTaskId = draggingItemId
                        draggingItemId = null
                        dragOffsetY = 0f
                        if (finalIndex != null) {
                            onReorder(localList, movedTaskId)
                        }
                    },
                    onDragCancel = {
                        draggingItemIndex = null
                        draggingItemId = null
                        dragOffsetY = 0f
                    }
                )
            },
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {
        itemsIndexed(
            items = localList,
            key = { _, item -> item.id }
        ) { index, task ->
            val isDragging = index == draggingItemIndex
            val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "scale")
            val elevation = if (isDragging) 8.dp else 0.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .zIndex(if (isDragging) 10f else 1f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                    }
                    .scale(scale)
                    .shadow(elevation)
            ) {
                TaskItem(
                    task = task,
                    onDeleteRequested = onDeleteOrComplete,
                    onClick = onTaskClick
                )
            }
        }
    }
}
