package com.kmab.singalong.backup

import java.io.IOException

enum class SettingsRestoreStatus { NOT_SELECTED, RESTORED, WRITE_FAILED }
data class BackupRestoreResult(val added: Int, val settings: SettingsRestoreStatus)

/** Validate both selected stores first; report a settings I/O failure after a committed library merge. */
suspend fun restoreBackup(
    document: BackupDocument,
    includeSettings: Boolean,
    validateSettings: suspend (Map<String, String>) -> Unit,
    restoreLibrary: suspend (BackupDocument) -> Int,
    applySettings: suspend (Map<String, String>) -> Unit,
): BackupRestoreResult {
    BackupCodec.validate(document)
    if (includeSettings) validateSettings(document.preferences)
    val added = restoreLibrary(document)
    val settings = if (!includeSettings) SettingsRestoreStatus.NOT_SELECTED else try {
        applySettings(document.preferences)
        SettingsRestoreStatus.RESTORED
    } catch (_: IOException) { SettingsRestoreStatus.WRITE_FAILED }
    return BackupRestoreResult(added, settings)
}
