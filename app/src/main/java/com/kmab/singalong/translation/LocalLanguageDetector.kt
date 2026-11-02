package com.kmab.singalong.translation

import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Detection stays on-device. Only the separately enabled translation service receives lyric text. */
class LocalLanguageDetector : LanguageDetector {
    override suspend fun identify(text: String): String? = suspendCancellableCoroutine { continuation ->
        val detector = LanguageIdentification.getClient()
        continuation.invokeOnCancellation { detector.close() }
        detector.identifyLanguage(text)
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
            .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
            .addOnCompleteListener { detector.close() }
    }
}
