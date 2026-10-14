package com.prioritydo.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prioritydo.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE date IS NULL AND isCompleted = 0 ORDER BY priority ASC, sortOrder ASC, createdAt ASC")
    fun getTasksWithoutDate(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date IS NOT NULL AND isCompleted = 0 AND date BETWEEN :rangeStart AND :rangeEnd ORDER BY priority ASC, sortOrder ASC, createdAt ASC")
    fun getTasksWithDate(rangeStart: Long, rangeEnd: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date IS NOT NULL AND isCompleted = 0 ORDER BY priority ASC, sortOrder ASC, createdAt ASC")
    fun getAllTasksWithDate(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority ASC, sortOrder ASC, createdAt ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority ASC, sortOrder ASC, createdAt ASC")
    suspend fun getAllTasksSnapshot(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority ASC, sortOrder ASC, createdAt ASC LIMIT 1")
    fun getHighestPriorityTask(): Flow<Task?>

    @Query("SELECT MAX(sortOrder) FROM tasks WHERE priority = :priority")
    suspend fun getMaxSortOrderForPriority(priority: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun updateAll(tasks: List<Task>)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)
}
