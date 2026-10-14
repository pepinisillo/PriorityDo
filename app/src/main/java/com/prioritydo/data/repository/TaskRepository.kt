package com.prioritydo.data.repository

import com.prioritydo.data.db.TaskDao
import com.prioritydo.data.model.Task
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getTasksWithoutDate(): Flow<List<Task>> = taskDao.getTasksWithoutDate()

    fun getUpcomingTasksWithDate(monthsAhead: Int = 2): Flow<List<Task>> {
        val calendar = getStartOfDay()
        val startOfToday = calendar.timeInMillis

        calendar.add(Calendar.MONTH, monthsAhead)
        val endOfWindow = calendar.timeInMillis

        return taskDao.getTasksWithDate(startOfToday, endOfWindow)
    }

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getHighestPriorityTask(): Flow<Task?> = taskDao.getHighestPriorityTask()

    fun getTaskById(id: Long): Flow<Task?> = taskDao.getTaskById(id)

    suspend fun insertTask(
        title: String,
        priority: Int,
        date: Long? = null,
        time: String? = null,
        repeatType: String? = null,
        repeatInterval: Int = 1
    ): Long {
        val maxOrder = taskDao.getMaxSortOrderForPriority(priority) ?: 0
        val newTask = Task(
            title = title,
            priority = priority,
            sortOrder = maxOrder + 1,
            date = date,
            time = time,
            repeatType = if (repeatType == Task.REPEAT_NONE) null else repeatType,
            repeatInterval = repeatInterval,
            createdAt = System.currentTimeMillis()
        )
        return taskDao.insert(newTask)
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task)
    }

    suspend fun completeOrDeleteTask(task: Task) {
        if (!task.repeatType.isNullOrBlank() && task.repeatType != Task.REPEAT_NONE && task.date != null) {
            val nextDate = calculateNextOccurrence(task.date, task.repeatType, task.repeatInterval)
            val updatedTask = task.copy(date = nextDate)
            taskDao.update(updatedTask)
        } else {
            taskDao.delete(task)
        }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
    }

    suspend fun reorderTasks(tasks: List<Task>, movedTaskId: Long? = null) {
        val adaptedTasks = adaptPrioritiesOnReorder(tasks, movedTaskId)
        taskDao.updateAll(adaptedTasks)
    }

    suspend fun moveTaskUp(taskId: Long, currentList: List<Task>) {
        moveTaskBy(taskId, currentList, -1)
    }

    suspend fun moveTaskDown(taskId: Long, currentList: List<Task>) {
        moveTaskBy(taskId, currentList, 1)
    }

    suspend fun moveTaskBy(taskId: Long, currentList: List<Task>, offset: Int) {
        val index = currentList.indexOfFirst { it.id == taskId }
        if (index < 0 || offset == 0) return

        val targetIndex = (index + offset).coerceIn(0, currentList.lastIndex)
        if (targetIndex == index) return

        val mutable = currentList.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(targetIndex, item)
        reorderTasks(mutable, taskId)
    }

    /**
     * Lógica de mutación de color según posición:
     * Cuando se arrastra o mueve una tarea entre otras, adopta el color/prioridad de su nueva posición:
     * - Si se pone entre dos del mismo color -> toma ese color.
     * - Si se sube debajo de un color superior -> adopta el color superior.
     * - Si se sube hasta el tope -> adopta la prioridad del tope.
     */
    private fun adaptPrioritiesOnReorder(tasks: List<Task>, movedTaskId: Long?): List<Task> {
        if (tasks.isEmpty()) return emptyList()

        val result = tasks.toMutableList()
        val movedIndex = movedTaskId?.let { id -> result.indexOfFirst { it.id == id } } ?: -1
        if (movedIndex >= 0) {
            val newPriority = priorityForPosition(result, movedIndex)
            result[movedIndex] = result[movedIndex].copy(priority = newPriority)
        }

        return result.mapIndexed { index, task ->
            task.copy(sortOrder = index)
        }
    }

    private fun priorityForPosition(tasks: List<Task>, index: Int): Int {
        val movedPriority = tasks[index].priority
        val previousPriority = tasks.getOrNull(index - 1)?.priority
        val nextPriority = tasks.getOrNull(index + 1)?.priority

        val resolvedPriority = when {
            previousPriority == null && nextPriority == null -> movedPriority
            previousPriority == null && nextPriority == movedPriority -> movedPriority - 1
            previousPriority == null -> nextPriority!!
            nextPriority == null && previousPriority == movedPriority -> movedPriority + 1
            nextPriority == null -> previousPriority
            previousPriority == nextPriority -> previousPriority
            previousPriority == movedPriority -> nextPriority
            nextPriority == movedPriority -> previousPriority
            else -> movedPriority
        }

        return resolvedPriority.coerceIn(Task.PRIORITY_RED, Task.PRIORITY_PURPLE)
    }

    fun calculateNextOccurrence(currentDate: Long, repeatType: String, interval: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDate
        }
        val count = if (interval <= 0) 1 else interval
        when (repeatType) {
            Task.REPEAT_DAILY -> calendar.add(Calendar.DAY_OF_YEAR, count)
            Task.REPEAT_WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, count)
            Task.REPEAT_MONTHLY -> calendar.add(Calendar.MONTH, count)
            Task.REPEAT_YEARLY -> calendar.add(Calendar.YEAR, count)
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}

private fun getStartOfDay(): Calendar {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
