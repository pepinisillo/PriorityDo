package com.prioritydo.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager

object OverlayPermissionHelper {

    private const val TAG = "OverlayHelper"

    /**
     * Comprueba si la aplicación tiene permiso para mostrar sobre otras aplicaciones.
     * Utiliza una combinación de 3 métodos para compatibilidad con Xiaomi (MIUI/HyperOS),
     * Samsung (One UI) y otros fabricantes con modificaciones del sistema:
     * 1. Settings.canDrawOverlays(context)
     * 2. AppOpsManager check
     * 3. Prueba en tiempo real intentando agregar una vista invisible a WindowManager
     */
    fun canDrawOverlays(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        // 1. Verificación oficial de Android
        if (Settings.canDrawOverlays(context)) {
            Log.d(TAG, "canDrawOverlays: verificado mediante Settings.canDrawOverlays")
            return true
        }

        // 2. Verificación mediante AppOpsManager
        try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            if (appOpsManager != null) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOpsManager.unsafeCheckOpNoThrow(
                        "android:system_alert_window",
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOpsManager.checkOpNoThrow(
                        AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        Process.myUid(),
                        context.packageName
                    )
                }
                if (mode == AppOpsManager.MODE_ALLOWED) {
                    Log.d(TAG, "canDrawOverlays: verificado mediante AppOpsManager MODE_ALLOWED")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error comprobando AppOpsManager", e)
        }

        // 3. Verificación práctica directa (añadir vista invisible de prueba)
        // Soluciona el error común en Xiaomi/Samsung donde Settings.canDrawOverlays retorna false
        // aún después de haber activado el interruptor en ajustes.
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager != null) {
                val testView = View(context)
                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                val params = WindowManager.LayoutParams(
                    0, 0,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT
                )
                windowManager.addView(testView, params)
                windowManager.removeView(testView)
                Log.d(TAG, "canDrawOverlays: verificado con éxito mediante prueba en WindowManager")
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "canDrawOverlays: prueba en WindowManager falló (sin permiso todavía)")
        }

        return false
    }

    /**
     * Abre la pantalla de ajustes de superposición de forma segura.
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Log.e(TAG, "No se pudo abrir los ajustes de superposición", e2)
                }
            }
        }
    }
}
