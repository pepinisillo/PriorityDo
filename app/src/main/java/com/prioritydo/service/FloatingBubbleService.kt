package com.prioritydo.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.prioritydo.MainActivity
import com.prioritydo.data.model.Task
import com.prioritydo.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class FloatingBubbleService : Service() {

    @Inject
    lateinit var repository: TaskRepository

    private var windowManager: WindowManager? = null

    // Vista de la burbuja
    private var bubbleView: View? = null
    private var bubbleCircle: ImageView? = null
    private var isBubbleAdded = false

    // Panel flotante de pendientes (1/3 de pantalla en el centro)
    private var panelView: FloatingPanelView? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var isPanelShowing = false
    private var panelShownAtElapsed = 0L
    private var suppressOpenUntil = 0L
    private var cachedTasks: List<Task> = emptyList()
    private var panelWasShowingOnTouchDown = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var taskCollectionJob: Job? = null
    private var allTasksJob: Job? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var isLongClick = false
    private val longPressRunnable = Runnable {
        if (SystemClock.elapsedRealtime() < suppressOpenUntil) return@Runnable
        isLongClick = true
        openQuickAddInPanel()
    }

    companion object {
        private const val TAG = "FloatingBubbleService"
        var isRunning = false
            private set
        private const val CHANNEL_ID = "priority_do_bubble_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: iniciando servicio de burbuja")
        startForegroundServiceWithNotification()
        setupFloatingBubble()
        observeTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand invocado")
        startForegroundServiceWithNotification()
        if (!isBubbleAdded || bubbleView == null) {
            setupFloatingBubble()
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "PriorityDo Burbuja",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Burbuja flotante de tareas pendientes"
                    setShowBadge(false)
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PriorityDo activa")
                .setContentText("Burbuja flotante de pendientes disponible")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Fallo al iniciar foreground con tipo especial, intentando fallback", e)
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, 0)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando servicio en primer plano", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingBubble() {
        if (isBubbleAdded) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 60
                y = 300
            }

            val bubbleSize = (60 * resources.displayMetrics.density).toInt()

            val root = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            }

            bubbleCircle = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
                setImageResource(android.R.drawable.ic_menu_agenda)
                setColorFilter(AndroidColor.WHITE)
                val padding = (14 * resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
                elevation = 12f * resources.displayMetrics.density
                isClickable = false
                isFocusable = false
                updateBubbleColor(Task.PRIORITY_BLUE)
            }

            root.addView(bubbleCircle)
            bubbleView = root

            val density = resources.displayMetrics.density
            val clickThreshold = 18f * density
            val dragThreshold = 8f * density

            root.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isLongClick = false
                        panelWasShowingOnTouchDown = isPanelShowing
                        handler.removeCallbacks(longPressRunnable)

                        if (!panelWasShowingOnTouchDown &&
                            SystemClock.elapsedRealtime() >= suppressOpenUntil
                        ) {
                            handler.postDelayed(longPressRunnable, 500)
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        if (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold) {
                            handler.removeCallbacks(longPressRunnable)
                            params.x = initialX + deltaX.toInt()
                            params.y = initialY + deltaY.toInt()
                            try {
                                windowManager?.updateViewLayout(bubbleView, params)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error actualizando posición de la burbuja", e)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        handler.removeCallbacks(longPressRunnable)
                        val deltaX = abs(event.rawX - initialTouchX)
                        val deltaY = abs(event.rawY - initialTouchY)
                        val blocked = SystemClock.elapsedRealtime() < suppressOpenUntil

                        if (!isLongClick && deltaX < clickThreshold && deltaY < clickThreshold) {
                            if (panelWasShowingOnTouchDown) {
                                Log.d(TAG, "Tap en burbuja: cerrar panel")
                                hideFloatingPanel()
                                suppressOpenUntil = SystemClock.elapsedRealtime() + 250L
                            } else if (!blocked) {
                                Log.d(TAG, "Tap en burbuja: abrir panel")
                                showFloatingPanel(focusInput = false)
                            }
                        }
                        panelWasShowingOnTouchDown = false
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable)
                        panelWasShowingOnTouchDown = false
                        false
                    }
                    else -> false
                }
            }

            windowManager?.addView(bubbleView, params)
            isBubbleAdded = true
            isRunning = true
            Log.d(TAG, "Burbuja agregada exitosamente a la pantalla")
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar la vista de la burbuja a WindowManager", e)
            Toast.makeText(
                this,
                "No se pudo mostrar la burbuja: verifica el permiso 'Mostrar sobre otras aplicaciones'",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingPanel(focusInput: Boolean = false) {
        if (isPanelShowing) return
        if (SystemClock.elapsedRealtime() < suppressOpenUntil) return

        try {
            if (panelView != null) {
                try {
                    windowManager?.removeView(panelView)
                } catch (_: Exception) {}
                panelView = null
                panelParams = null
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val displayMetrics = resources.displayMetrics
            val panelWidth = (displayMetrics.widthPixels * 0.92f).toInt()
            val panelHeight = (displayMetrics.heightPixels * 0.46f).toInt()

            val panelFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                (if (focusInput) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

            val params = WindowManager.LayoutParams(
                panelWidth,
                panelHeight,
                layoutFlag,
                panelFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            panelView = FloatingPanelView(
                context = this,
                onCloseClick = { hideFloatingPanel() },
                onOpenFullAppClick = {
                    hideFloatingPanel()
                    openMainApp()
                },
                onCompleteTaskClick = { task ->
                    serviceScope.launch {
                        repository.completeOrDeleteTask(task)
                    }
                },
                onMoveTaskUpClick = { task ->
                    serviceScope.launch {
                        repository.moveTaskUp(task.id, cachedTasks)
                    }
                },
                onMoveTaskDownClick = { task ->
                    serviceScope.launch {
                        repository.moveTaskDown(task.id, cachedTasks)
                    }
                },
                onMoveTaskByClick = { task, offset ->
                    serviceScope.launch {
                        repository.moveTaskBy(task.id, cachedTasks, offset)
                    }
                },
                onAddTaskSubmit = { title, priority, date, time ->
                    serviceScope.launch {
                        repository.insertTask(
                            title = title,
                            priority = priority,
                            date = date,
                            time = time
                        )
                    }
                },
                onInputFocusRequested = {
                    makePanelFocusableForInput()
                }
            ).apply {
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        if (SystemClock.elapsedRealtime() - panelShownAtElapsed < 400L) {
                            return@setOnTouchListener true
                        }
                        hideFloatingPanel()
                        // Evita que el mismo toque (p. ej. en la burbuja) vuelva a abrir
                        suppressOpenUntil = SystemClock.elapsedRealtime() + 500L
                        true
                    } else false
                }
            }

            panelView?.setTasks(cachedTasks)
            windowManager?.addView(panelView, params)
            panelParams = params
            isPanelShowing = true
            panelShownAtElapsed = SystemClock.elapsedRealtime()
            Log.d(TAG, "Panel flotante mostrado")

            if (focusInput) {
                panelView?.focusInput()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando panel flotante", e)
            Toast.makeText(this, "No se pudo abrir el panel flotante", Toast.LENGTH_SHORT).show()
            isPanelShowing = false
            panelView = null
            panelParams = null
        }
    }

    private fun hideFloatingPanel() {
        if (!isPanelShowing && panelView == null) return
        try {
            panelView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error ocultando panel flotante", e)
        }
        panelView = null
        panelParams = null
        isPanelShowing = false
        Log.d(TAG, "Panel flotante oculto")
    }

    private fun makePanelFocusableForInput() {
        val view = panelView ?: return
        val params = panelParams ?: return
        if (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE == 0) return

        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error habilitando foco del panel", e)
        }
    }

    private fun openQuickAddInPanel() {
        if (!isPanelShowing) {
            showFloatingPanel(focusInput = true)
        } else {
            panelView?.focusInput()
        }
    }

    private fun updateBubbleColor(priority: Int) {
        val colorInt = when (priority) {
            Task.PRIORITY_RED -> AndroidColor.parseColor("#E53935")
            Task.PRIORITY_ORANGE -> AndroidColor.parseColor("#FB8C00")
            Task.PRIORITY_YELLOW -> AndroidColor.parseColor("#FBC02D")
            Task.PRIORITY_GREEN -> AndroidColor.parseColor("#43A047")
            Task.PRIORITY_BLUE -> AndroidColor.parseColor("#1E88E5")
            Task.PRIORITY_PURPLE -> AndroidColor.parseColor("#8E24AA")
            else -> AndroidColor.parseColor("#8E24AA")
        }

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorInt)
            setStroke((3 * resources.displayMetrics.density).toInt(), AndroidColor.WHITE)
        }

        bubbleCircle?.background = drawable
    }

    private fun observeTasks() {
        // Observar tarea de máxima prioridad para el color de la burbuja
        taskCollectionJob = serviceScope.launch {
            repository.getHighestPriorityTask().collectLatest { highestTask ->
                val priority = highestTask?.priority ?: Task.PRIORITY_BLUE
                updateBubbleColor(priority)
            }
        }

        // Observar todas las tareas para alimentar el panel flotante
        allTasksJob = serviceScope.launch {
            repository.getAllTasks().collectLatest { tasks ->
                cachedTasks = tasks
                if (isPanelShowing) {
                    panelView?.setTasks(tasks)
                }
            }
        }
    }

    private fun openMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: deteniendo servicio de burbuja")
        isRunning = false
        taskCollectionJob?.cancel()
        allTasksJob?.cancel()
        hideFloatingPanel()
        if (bubbleView != null && isBubbleAdded) {
            try {
                windowManager?.removeView(bubbleView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removiendo vista de burbuja", e)
            }
            bubbleView = null
            isBubbleAdded = false
        }
        super.onDestroy()
    }
}
