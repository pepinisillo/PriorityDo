package com.prioritydo.service

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.prioritydo.data.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class FloatingPanelView(
    context: Context,
    private val onCloseClick: () -> Unit,
    private val onOpenFullAppClick: () -> Unit,
    private val onCompleteTaskClick: (Task) -> Unit,
    private val onMoveTaskUpClick: (Task) -> Unit,
    private val onMoveTaskDownClick: (Task) -> Unit,
    private val onMoveTaskByClick: (Task, Int) -> Unit,
    private val onAddTaskSubmit: (title: String, priority: Int, date: Long?, time: String?) -> Unit,
    private val onInputFocusRequested: () -> Unit = {}
) : FrameLayout(context) {

    // 0: Todas, 1: Sin fecha, 2: Con fecha
    private var currentTab = 0
    private var selectedPriority = Task.PRIORITY_RED
    private var pendingDateMillis: Long? = null
    private var pendingTime: String? = null
    private var currentTasks: List<Task> = emptyList()

    private val tabTextViews = mutableListOf<TextView>()
    private val tabIndicatorViews = mutableListOf<View>()
    private val priorityDots = mutableListOf<View>()
    private lateinit var tasksContainer: LinearLayout
    private lateinit var emptyTextView: TextView
    private lateinit var taskInput: EditText
    private lateinit var calendarBtn: ImageButton
    private lateinit var timeBtn: ImageButton
    private lateinit var gestureDetector: GestureDetector

    companion object {
        // 6 Colores fuertes y vibrantes para contrastar con texto blanco
        val PRIORITY_NOTE_BG = listOf(
            "#D32F2F", // 0: Rojo fuerte
            "#E65100", // 1: Naranja fuerte
            "#F57F17", // 2: Amarillo / Ámbar fuerte
            "#2E7D32", // 3: Verde fuerte
            "#1565C0", // 4: Azul fuerte
            "#7B1FA2"  // 5: Morado fuerte
        )
    }

    init {
        setupGestureDetector()
        setupView()
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 50f
            private val SWIPE_VELOCITY_THRESHOLD = 50f

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                // Deslizamiento horizontal entre categorías (swipe)
                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > SWIPE_THRESHOLD &&
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    if (diffX < 0) {
                        // Swipe izquierda -> siguiente pestaña (Todas -> Sin fecha -> Con fecha)
                        if (currentTab < 2) {
                            switchTab(currentTab + 1)
                            return true
                        }
                    } else {
                        // Swipe derecha -> pestaña anterior (Con fecha -> Sin fecha -> Todas)
                        if (currentTab > 0) {
                            switchTab(currentTab - 1)
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    private fun switchTab(newTab: Int) {
        currentTab = newTab
        updateTabsStyle()
        renderTasks()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView() {
        // MODO OSCURO: Fondo oscuro profundo con bordes suaves y sombra
        val cardBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18f).toFloat()
            setColor(Color.parseColor("#1E1E1E"))
            setStroke(dp(1.2f), Color.parseColor("#333333"))
        }
        background = cardBackground
        elevation = dp(18f).toFloat()
        isClickable = true
        isFocusable = true
        setPadding(dp(10f), dp(8f), dp(10f), dp(8f))

        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        // 1. HEADER: Pestañas de categorías con subrayado indicador (Todas | Sin fecha | Con fecha)
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38f)
            )
        }

        // Contenedor de Pestañas con estilo de subrayado
        val tabsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        val tabTitles = listOf("Todas", "Sin fecha", "Con fecha")
        tabTitles.forEachIndexed { index, title ->
            val singleTabCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setPadding(dp(2f), 0, dp(2f), 0)
                isClickable = true
                isFocusable = true
                setOnClickListener { switchTab(index) }
            }

            val text = TextView(context).apply {
                this.text = title
                textSize = 12.5f
                includeFontPadding = false
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                setPadding(dp(1f), 0, dp(1f), 0)
            }
            tabTextViews.add(text)
            singleTabCol.addView(text)

            // Línea indicadora inferior subrayada
            val indicator = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dp(34f),
                    dp(2.5f)
                ).apply { bottomMargin = dp(2f) }
            }
            tabIndicatorViews.add(indicator)
            singleTabCol.addView(indicator)

            tabsLayout.addView(singleTabCol)
        }
        headerLayout.addView(tabsLayout)

        // Botón abrir app completa
        val openAppBtn = ImageButton(context, null, android.R.attr.borderlessButtonStyle).apply {
            setImageResource(android.R.drawable.ic_menu_crop)
            setColorFilter(Color.parseColor("#B0BEC5"))
            setPadding(dp(5f), dp(5f), dp(5f), dp(5f))
            layoutParams = LinearLayout.LayoutParams(dp(34f), dp(34f)).apply {
                topMargin = dp(1f)
            }
            setOnClickListener { onOpenFullAppClick() }
        }
        headerLayout.addView(openAppBtn)

        // Botón Cerrar (X)
        val closeBtn = ImageButton(context, null, android.R.attr.borderlessButtonStyle).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#EEEEEE"))
            setPadding(dp(5f), dp(5f), dp(5f), dp(5f))
            layoutParams = LinearLayout.LayoutParams(dp(34f), dp(34f)).apply {
                topMargin = dp(1f)
            }
            setOnClickListener { onCloseClick() }
        }
        headerLayout.addView(closeBtn)

        mainLayout.addView(headerLayout)
        updateTabsStyle()

        // Línea divisoria oscura
        val divider = View(context).apply {
            setBackgroundColor(Color.parseColor("#2C2C2C"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)).apply {
                bottomMargin = dp(3f)
            }
        }
        mainLayout.addView(divider)

        // 2. CONTENIDO CENTRAL: ScrollView con soporte de swipe entre categorías
        val scrollView = object : ScrollView(context) {
            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(ev)
                return super.onInterceptTouchEvent(ev)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isVerticalScrollBarEnabled = true
            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
        }

        tasksContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        emptyTextView = TextView(context).apply {
            text = "No hay pendientes en esta categoría"
            textSize = 13f
            setTextColor(Color.parseColor("#757575"))
            gravity = Gravity.CENTER
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            visibility = View.GONE
        }
        tasksContainer.addView(emptyTextView)
        scrollView.addView(tasksContainer)
        mainLayout.addView(scrollView)

        // 3. BARRA INFERIOR (MODO OSCURO): Selector de 6 colores + Input + Botón Calendario + Botón (+)
        val bottomLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6f)
            }
        }

        // Selector rápido de 6 colores de prioridad
        val priorityContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2f), 0, dp(4f), 0)
        }

        PRIORITY_NOTE_BG.forEachIndexed { prio, hex ->
            val dot = View(context).apply {
                val size = dp(14f)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(3f)
                }
                setOnClickListener {
                    selectedPriority = prio
                    updatePriorityDots()
                }
            }
            priorityDots.add(dot)
            priorityContainer.addView(dot)
        }
        bottomLayout.addView(priorityContainer)
        updatePriorityDots()

        // Campo de texto rápido en modo oscuro
        taskInput = EditText(context).apply {
            hint = "Escribir pendiente..."
            textSize = 13.5f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#757575"))
            setPadding(dp(10f), dp(7f), dp(10f), dp(7f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8f).toFloat()
                setColor(Color.parseColor("#2A2A2A"))
            }
            imeOptions = EditorInfo.IME_ACTION_DONE
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitTask()
                    true
                } else false
            }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onInputFocusRequested()
                }
                false
            }
        }
        bottomLayout.addView(taskInput)

        // Botón Icono de Calendario (NO EMOJI) al lado del botón agregar
        calendarBtn = ImageButton(context).apply {
            val size = dp(34f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = dp(5f)
            }
            setImageResource(android.R.drawable.ic_menu_today)
            setColorFilter(Color.WHITE)
            setPadding(dp(7f), dp(7f), dp(7f), dp(7f))
            setOnClickListener {
                val cal = Calendar.getInstance()
                if (pendingDateMillis != null) {
                    cal.timeInMillis = pendingDateMillis!!
                }
                val dialog = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val picked = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        pendingDateMillis = picked.timeInMillis
                        updateCalendarBtnState()
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                } else {
                    @Suppress("DEPRECATION")
                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
                }
                dialog.show()
            }
            setOnLongClickListener {
                if (pendingDateMillis != null) {
                    pendingDateMillis = null
                    updateCalendarBtnState()
                    Toast.makeText(context, "Fecha removida", Toast.LENGTH_SHORT).show()
                    true
                } else false
            }
        }
        updateCalendarBtnState()
        bottomLayout.addView(calendarBtn)

        timeBtn = ImageButton(context).apply {
            val size = dp(34f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = dp(5f)
            }
            setImageResource(android.R.drawable.ic_menu_recent_history)
            setColorFilter(Color.WHITE)
            setPadding(dp(7f), dp(7f), dp(7f), dp(7f))
            setOnClickListener {
                val cal = Calendar.getInstance()
                val existingParts = pendingTime?.split(":")
                val hour = existingParts?.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
                val minute = existingParts?.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
                val dialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, selectedMinute ->
                        pendingTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, selectedMinute)
                        updateTimeBtnState()
                    },
                    hour,
                    minute,
                    true
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                } else {
                    @Suppress("DEPRECATION")
                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
                }
                dialog.show()
            }
            setOnLongClickListener {
                if (pendingTime != null) {
                    pendingTime = null
                    updateTimeBtnState()
                    Toast.makeText(context, "Hora removida", Toast.LENGTH_SHORT).show()
                    true
                } else false
            }
        }
        updateTimeBtnState()
        bottomLayout.addView(timeBtn)

        // Botón circular (+) para agregar
        val addBtn = ImageButton(context).apply {
            val size = dp(34f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = dp(5f)
            }
            setImageResource(android.R.drawable.ic_input_add)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#7C4DFF"))
            }
            setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
            setOnClickListener { submitTask() }
        }
        bottomLayout.addView(addBtn)

        mainLayout.addView(bottomLayout)
        addView(mainLayout)
    }

    private fun updateCalendarBtnState() {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (pendingDateMillis != null) {
                setColor(Color.parseColor("#00897B")) // Verde azulado cuando hay fecha asignada
                setStroke(dp(1.5f), Color.WHITE)
            } else {
                setColor(Color.parseColor("#37474F")) // Gris oscuro neutro
            }
        }
        calendarBtn.background = bg
    }

    private fun updateTimeBtnState() {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (pendingTime != null) {
                setColor(Color.parseColor("#5E35B1"))
                setStroke(dp(1.5f), Color.WHITE)
            } else {
                setColor(Color.parseColor("#37474F"))
            }
        }
        timeBtn.background = bg
    }

    private fun updateTabsStyle() {
        tabTextViews.forEachIndexed { index, textView ->
            val isSelected = index == currentTab
            val indicator = tabIndicatorViews[index]

            if (isSelected) {
                textView.setTextColor(Color.WHITE)
                textView.setTypeface(null, Typeface.BOLD)
                indicator.setBackgroundColor(Color.parseColor("#BB86FC")) // Subrayado activo
                indicator.visibility = View.VISIBLE
            } else {
                textView.setTextColor(Color.parseColor("#9E9E9E"))
                textView.setTypeface(null, Typeface.NORMAL)
                indicator.setBackgroundColor(Color.TRANSPARENT)
                indicator.visibility = View.INVISIBLE
            }
        }
    }

    private fun updatePriorityDots() {
        priorityDots.forEachIndexed { index, dot ->
            val isSelected = index == selectedPriority
            val color = Color.parseColor(PRIORITY_NOTE_BG[index])
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                if (isSelected) {
                    setStroke(dp(2.5f), Color.WHITE)
                }
            }
            dot.background = drawable
        }
    }

    private fun submitTask() {
        val text = taskInput.text.toString().trim()
        if (text.isNotEmpty()) {
            val finalDate = pendingDateMillis ?: if (currentTab == 2) System.currentTimeMillis() else null
            onAddTaskSubmit(text, selectedPriority, finalDate, pendingTime)
            taskInput.setText("")
            pendingDateMillis = null
            pendingTime = null
            updateCalendarBtnState()
            updateTimeBtnState()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(taskInput.windowToken, 0)
        }
    }

    fun focusInput() {
        onInputFocusRequested()
        taskInput.requestFocus()
        taskInput.postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(taskInput, InputMethodManager.SHOW_IMPLICIT)
        }, 150)
    }

    fun setTasks(tasks: List<Task>) {
        currentTasks = tasks
        renderTasks()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun renderTasks() {
        // Filtrar según la pestaña activa: 0: Todas, 1: Sin fecha, 2: Con fecha
        val filteredTasks = when (currentTab) {
            0 -> currentTasks
            1 -> currentTasks.filter { !it.hasDate }
            2 -> currentTasks.filter { it.hasDate }
            else -> currentTasks
        }

        tasksContainer.removeAllViews()

        if (filteredTasks.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
            tasksContainer.addView(emptyTextView)
            return
        }
        emptyTextView.visibility = View.GONE

        val dateFormat = SimpleDateFormat("dd MMM", Locale("es", "ES"))

        filteredTasks.forEachIndexed { index, task ->
            val prioIndex = task.priority.coerceIn(0, 5)
            val noteBgHex = PRIORITY_NOTE_BG[prioIndex]

            // Toda la nota coloreada de fondo con color fuerte, SIN franja a la izquierda
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(2f)
                    bottomMargin = dp(2f)
                }
                setPadding(dp(10f), dp(7f), dp(6f), dp(7f))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(8f).toFloat()
                    setColor(Color.parseColor(noteBgHex))
                }
            }

            // Fila con título y metadatos para ahorrar altura en la burbuja
            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleView = TextView(context).apply {
                text = task.title
                textSize = 13.5f
                setTextColor(Color.WHITE)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(titleView)

            // Chips pequeños de fecha y hora si existen (Iconos nativos vectoriales, SIN EMOJIS)
            if (task.date != null || !task.time.isNullOrBlank()) {
                val metaLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = dp(6f)
                    }
                }
                if (task.date != null) {
                    val dateChip = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dp(4f).toFloat()
                            setColor(Color.parseColor("#38FFFFFF")) // 22% blanco translúcido
                        }
                        setPadding(dp(5f), dp(1.5f), dp(6f), dp(1.5f))
                    }
                    val calIcon = ImageView(context).apply {
                        setImageResource(android.R.drawable.ic_menu_today)
                        setColorFilter(Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(dp(11f), dp(11f)).apply {
                            marginEnd = dp(3f)
                        }
                    }
                    val dateText = TextView(context).apply {
                        text = dateFormat.format(Date(task.date))
                        textSize = 10f
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                    }
                    dateChip.addView(calIcon)
                    dateChip.addView(dateText)
                    metaLayout.addView(dateChip)
                }
                if (!task.time.isNullOrBlank()) {
                    val timeChip = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dp(4f).toFloat()
                            setColor(Color.parseColor("#38FFFFFF"))
                        }
                        setPadding(dp(5f), dp(1.5f), dp(6f), dp(1.5f))
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginStart = dp(6f)
                        }
                        layoutParams = lp
                    }
                    val clockIcon = ImageView(context).apply {
                        setImageResource(android.R.drawable.ic_menu_recent_history)
                        setColorFilter(Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(dp(11f), dp(11f)).apply {
                            marginEnd = dp(3f)
                        }
                    }
                    val timeText = TextView(context).apply {
                        text = task.time
                        textSize = 10f
                        setTextColor(Color.WHITE)
                    }
                    timeChip.addView(clockIcon)
                    timeChip.addView(timeText)
                    metaLayout.addView(timeChip)
                }
                textCol.addView(metaLayout)
            }
            row.addView(textCol)

            // Botón X para completar o eliminar la tarea (Blanco)
            val deleteBtn = ImageButton(context, null, android.R.attr.borderlessButtonStyle).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.WHITE)
                setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
                layoutParams = LinearLayout.LayoutParams(dp(26f), dp(26f))
                setOnClickListener {
                    showCompleteOrDeleteConfirmation(task)
                }
            }
            row.addView(deleteBtn)

            // Zona de agarre con los puntitos (Drag Handle Dots ⋮⋮⋮)
            val dragHandle = createDragHandleDots(
                onDragUp = { onMoveTaskUpClick(task) },
                onDragDown = { onMoveTaskDownClick(task) },
                onDragBy = { offset -> onMoveTaskByClick(task, offset) }
            )
            row.addView(dragHandle)

            tasksContainer.addView(row)
        }
    }

    /**
     * Crea la zona de agarre con puntitos (:::), con soporte táctil para arrastrar hacia arriba o abajo
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createDragHandleDots(
        onDragUp: () -> Unit,
        onDragDown: () -> Unit,
        onDragBy: (Int) -> Unit
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(22f), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginStart = dp(4f)
            }
            setPadding(dp(2f), dp(4f), dp(2f), dp(4f))

            repeat(3) {
                val dotRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(0, dp(1.2f), 0, dp(1.2f))
                }
                repeat(2) {
                    val dot = View(context).apply {
                        val dotSize = dp(3.2f)
                        layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                            marginStart = dp(1.2f)
                            marginEnd = dp(1.2f)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.WHITE)
                            alpha = 190 // 75% blanco
                        }
                    }
                    dotRow.addView(dot)
                }
                addView(dotRow)
            }

            // Gesto de arrastre en los puntitos para subir o bajar la tarea
            var startY = 0f
            var startIndex = -1
            var currentSteps = 0
            var draggedRow: View? = null
            var isDraggingStarted = false
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.rawY
                        draggedRow = parent as? View
                        startIndex = draggedRow?.let { tasksContainer.indexOfChild(it) } ?: -1
                        currentSteps = 0
                        isDraggingStarted = false
                        // NO bloquear el scroll del ScrollView todavía
                        draggedRow?.elevation = dp(10f).toFloat()
                        draggedRow?.animate()
                            ?.alpha(0.96f)
                            ?.setDuration(90L)
                            ?.start()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val row = draggedRow
                        if (row != null && startIndex >= 0) {
                            val deltaY = event.rawY - startY
                            // Solo bloquear scroll del ScrollView cuando hay movimiento real de arrastre
                            if (!isDraggingStarted && Math.abs(deltaY) > dp(8f)) {
                                isDraggingStarted = true
                                parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            if (isDraggingStarted) {
                                val rowStep = dragStepSize(row)
                                val maxUp = -startIndex
                                val maxDown = tasksContainer.childCount - 1 - startIndex
                                val steps = (deltaY / rowStep)
                                    .roundToInt()
                                    .coerceIn(maxUp, maxDown)

                                row.translationY = deltaY.coerceIn(
                                    maxUp * rowStep,
                                    maxDown * rowStep
                                )

                                if (steps != currentSteps) {
                                    currentSteps = steps
                                    previewDragPositions(row, startIndex, startIndex + steps, rowStep)
                                }
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val row = draggedRow
                        val steps = currentSteps
                        val rowStep = row?.let { dragStepSize(it) } ?: dp(30f).toFloat()
                        val targetTranslation = steps * rowStep

                        if (row != null && steps != 0) {
                            row.animate()
                                .translationY(targetTranslation)
                                .alpha(0.98f)
                                .setDuration(120L)
                                .setInterpolator(DecelerateInterpolator())
                                .withEndAction {
                                    when {
                                        steps <= -2 || steps >= 2 -> onDragBy(steps)
                                        steps < 0 -> onDragUp()
                                        steps > 0 -> onDragDown()
                                    }
                                    tasksContainer.postDelayed({ resetDragAnimations() }, 180L)
                                }
                                .start()
                        } else {
                            resetDragAnimations()
                        }
                        parent?.requestDisallowInterceptTouchEvent(false)
                        draggedRow = null
                        startIndex = -1
                        currentSteps = 0
                        isDraggingStarted = false
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        parent?.requestDisallowInterceptTouchEvent(false)
                        resetDragAnimations()
                        draggedRow = null
                        startIndex = -1
                        currentSteps = 0
                        isDraggingStarted = false
                        false
                    }
                    else -> true
                }
            }
        }
    }

    private fun dragStepSize(row: View): Float {
        val margins = (row.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            it.topMargin + it.bottomMargin
        } ?: 0
        return max(dp(30f).toFloat(), (row.height + margins).toFloat())
    }

    private fun previewDragPositions(draggedRow: View, startIndex: Int, targetIndex: Int, rowStep: Float) {
        for (i in 0 until tasksContainer.childCount) {
            val child = tasksContainer.getChildAt(i)
            if (child == draggedRow || child == emptyTextView) continue

            val shift = when {
                targetIndex < startIndex && i in targetIndex until startIndex -> rowStep
                targetIndex > startIndex && i in (startIndex + 1)..targetIndex -> -rowStep
                else -> 0f
            }

            child.animate()
                .translationY(shift)
                .setDuration(120L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun resetDragAnimations() {
        for (i in 0 until tasksContainer.childCount) {
            tasksContainer.getChildAt(i).animate()
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(140L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            tasksContainer.getChildAt(i).elevation = 0f
        }
    }

    private fun showCompleteOrDeleteConfirmation(task: Task) {
        val message = if (!task.repeatType.isNullOrBlank() &&
            task.repeatType != Task.REPEAT_NONE &&
            task.date != null
        ) {
            "Esta tarea se repetira en su siguiente fecha."
        } else {
            "Esta tarea se eliminara de tus pendientes."
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Confirmar")
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                onCompleteTaskClick(task)
            }
            .create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }
        dialog.show()
    }
}
