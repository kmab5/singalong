package com.kmab.singalong.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.kmab.singalong.model.Recording
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.UUID

/** Only the user-selected document tree is read. No broad storage permission or persisted grant. */
class AudioFolderSource(private val context: Context) {
    private data class Document(val id: String, val name: String, val type: String)
    fun files(tree: Uri): Flow<AudioCandidate> = flow<AudioCandidate> {
        val pending = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        pending.add(DocumentsContract.getTreeDocumentId(tree))
        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val folder = pending.removeFirst()
            if (!visited.add(folder)) continue
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folder)
            // Close each directory cursor before yielding; a network wait must not retain it.
            val documents = try { context.contentResolver.query(children, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
                buildList<Document> {
                    while (cursor.moveToNext()) {
                        currentCoroutineContext().ensureActive()
                        add(Document(cursor.getString(0), cursor.getString(1).orEmpty(), cursor.getString(2).orEmpty()))
                    }
                }
            } ?: throw IOException("Document provider returned no cursor")
            } catch (unsupported: IllegalArgumentException) {
                throw IOException("Document provider could not list this folder", unsupported)
            } catch (unsupported: UnsupportedOperationException) {
                throw IOException("Document provider does not support folder listing", unsupported)
            }
            for (document in documents) {
                currentCoroutineContext().ensureActive()
                if (document.type == DocumentsContract.Document.MIME_TYPE_DIR) pending.add(document.id)
                else if (isAudio(document.name, document.type) && visited.add(document.id)) {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, document.id)
                    emit(AudioCandidate(document.name, metadata(uri, document.name)))
                }
            }
        }
    }.flowOn(Dispatchers.IO).buffer(0)

    private fun metadata(uri: Uri, name: String): Recording? {
        if (name.isBlank()) return null
        val id = UUID.nameUUIDFromBytes(uri.toString().toByteArray(Charsets.UTF_8)).toString()
        return try {
            MediaMetadataRetriever().use { retriever ->
                // Some document providers return a non-seekable stream or an unsupported codec.
                retriever.setDataSource(context, uri)
                fun tag(key: Int) = retriever.extractMetadata(key)?.trim()?.takeUnless { it.isBlank() || it.equals("<unknown>", true) }
                Recording(id, tag(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name,
                    listOfNotNull(tag(MediaMetadataRetriever.METADATA_KEY_ARTIST)),
                    tag(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    tag(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it > 0 }, fileName = name)
            }
        } catch (denied: SecurityException) { throw denied }
        catch (_: IOException) { Recording(id, name, emptyList(), null, null, fileName = name) }
        catch (_: RuntimeException) {
            // Scoped to Android's native metadata API: use filename hints for corrupt/unsupported tags.
            Recording(id, name, emptyList(), null, null, fileName = name)
        }
    }

    companion object {
        fun isAudio(name: String, mime: String) = mime.startsWith("audio/") || name.substringAfterLast('.', "").lowercase() in
            setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "aiff", "aif", "alac")
    }
}
