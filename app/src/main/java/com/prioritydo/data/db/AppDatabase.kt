package com.prioritydo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prioritydo.data.model.Task

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
