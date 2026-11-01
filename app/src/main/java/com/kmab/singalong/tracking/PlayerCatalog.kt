package com.kmab.singalong.tracking

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlayerApp(val packageName: String, val label: String)

class PlayerCatalog(private val context: Context) {
    suspend fun installed(): List<PlayerApp> = withContext(Dispatchers.IO) {
        context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { PlayerApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }
            .filterNot { it.packageName == context.packageName }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
    fun label(packageName: String): String = try {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) { packageName }
}
