package com.prioritydo.ui.theme

import androidx.compose.ui.graphics.Color

// 6 Colores de Prioridad principales (Vibrantes y fuertes para contrastar con texto blanco)
// 1. Rojo: Urgente / Máxima
val PriorityRed = Color(0xFFD32F2F)
// 2. Naranja: Muy Alta
val PriorityOrange = Color(0xFFF57C00)
// 3. Amarillo / Ámbar: Alta
val PriorityYellow = Color(0xFFF57F17)
// 4. Verde: Media
val PriorityGreen = Color(0xFF2E7D32)
// 5. Azul: Baja
val PriorityBlue = Color(0xFF1565C0)
// 6. Morado: Mínima
val PriorityPurple = Color(0xFF7B1FA2)

// Fondos completos para las notas (tonos fuertes y sólidos para que el texto blanco resalte al 100%)
val PriorityRedNoteBg = Color(0xFFD32F2F)
val PriorityOrangeNoteBg = Color(0xFFE65100)
val PriorityYellowNoteBg = Color(0xFFF57F17)
val PriorityGreenNoteBg = Color(0xFF2E7D32)
val PriorityBlueNoteBg = Color(0xFF1565C0)
val PriorityPurpleNoteBg = Color(0xFF7B1FA2)

// Textos de alto contraste sobre las tarjetas (Blanco puro)
val PriorityNoteTextColor = Color(0xFFFFFFFF)

// Standard theme colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

fun priorityColor(priority: Int): Color = when (priority) {
    0 -> PriorityRed
    1 -> PriorityOrange
    2 -> PriorityYellow
    3 -> PriorityGreen
    4 -> PriorityBlue
    5 -> PriorityPurple
    else -> PriorityPurple
}

fun priorityNoteBackground(priority: Int): Color = when (priority) {
    0 -> PriorityRedNoteBg
    1 -> PriorityOrangeNoteBg
    2 -> PriorityYellowNoteBg
    3 -> PriorityGreenNoteBg
    4 -> PriorityBlueNoteBg
    5 -> PriorityPurpleNoteBg
    else -> PriorityPurpleNoteBg
}

fun priorityName(priority: Int): String = when (priority) {
    0 -> "1. Rojo (Máxima)"
    1 -> "2. Naranja (Muy Alta)"
    2 -> "3. Amarillo (Alta)"
    3 -> "4. Verde (Media)"
    4 -> "5. Azul (Baja)"
    5 -> "6. Morado (Mínima)"
    else -> "Mínima"
}
