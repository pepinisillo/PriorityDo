package com.prioritydo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prioritydo.data.model.Task
import com.prioritydo.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    val tasksWithoutDate: StateFlow<List<Task>> = repository.getTasksWithoutDate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasksWithDate: StateFlow<List<Task>> = repository.getUpcomingTasksWithDate(monthsAhead = 2)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val highestPriorityTask: StateFlow<Task?> = repository.getHighestPriorityTask()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addTask(
        title: String,
        priority: Int,
        date: Long?,
        time: String?,
        repeatType: String?,
        repeatInterval: Int
    ) {
        viewModelScope.launch {
            repository.insertTask(
                title = title.trim(),
                priority = priority,
                date = date,
                time = time,
                repeatType = repeatType,
                repeatInterval = repeatInterval
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun completeOrDeleteTask(task: Task) {
        viewModelScope.launch {
            repository.completeOrDeleteTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun reorderTasks(tasks: List<Task>, movedTaskId: Long? = null) {
        viewModelScope.launch {
            repository.reorderTasks(tasks, movedTaskId)
        }
    }
}
