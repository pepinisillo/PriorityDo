package com.prioritydo

import com.prioritydo.data.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTest {

    @Test
    fun testTaskDefaults() {
        val task = Task(
            title = "Hacer compras",
            priority = Task.PRIORITY_RED
        )

        assertEquals("Hacer compras", task.title)
        assertEquals(Task.PRIORITY_RED, task.priority)
        assertEquals(0, task.sortOrder)
        assertFalse(task.hasDate)
        assertFalse(task.isCompleted)
    }

    @Test
    fun testTaskWithDate() {
        val now = System.currentTimeMillis()
        val task = Task(
            title = "Reunión de equipo",
            priority = Task.PRIORITY_YELLOW,
            date = now,
            time = "10:30",
            repeatType = Task.REPEAT_WEEKLY
        )

        assertTrue(task.hasDate)
        assertEquals("10:30", task.time)
        assertEquals(Task.REPEAT_WEEKLY, task.repeatType)
    }

    @Test
    fun testSixPriorityLevels() {
        assertEquals(0, Task.PRIORITY_RED)
        assertEquals(1, Task.PRIORITY_ORANGE)
        assertEquals(2, Task.PRIORITY_YELLOW)
        assertEquals(3, Task.PRIORITY_GREEN)
        assertEquals(4, Task.PRIORITY_BLUE)
        assertEquals(5, Task.PRIORITY_PURPLE)
    }
}
