package com.prioritydo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val priority: Int = PRIORITY_PURPLE,    // 0 a 5 (6 niveles de prioridad)
    val sortOrder: Int = 0,                 // orden en la lista
    val date: Long? = null,                 // timestamp epoch millis o null para sin fecha
    val time: String? = null,               // formato "HH:mm"
    val repeatType: String? = null,         // DAILY, WEEKLY, MONTHLY, YEARLY, o null
    val repeatInterval: Int = 1,            // Cada X días/semanas/meses
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val hasDate: Boolean get() = date != null

    companion object {
        const val PRIORITY_RED = 0      // 1. Rojo: Urgente / Máxima
        const val PRIORITY_ORANGE = 1   // 2. Naranja: Muy Alta
        const val PRIORITY_YELLOW = 2   // 3. Amarillo: Alta
        const val PRIORITY_GREEN = 3    // 4. Verde: Media
        const val PRIORITY_BLUE = 4     // 5. Azul: Baja
        const val PRIORITY_PURPLE = 5   // 6. Morado: Mínima

        const val REPEAT_NONE = "NONE"
        const val REPEAT_DAILY = "DAILY"
        const val REPEAT_WEEKLY = "WEEKLY"
        const val REPEAT_MONTHLY = "MONTHLY"
        const val REPEAT_YEARLY = "YEARLY"
    }
}
