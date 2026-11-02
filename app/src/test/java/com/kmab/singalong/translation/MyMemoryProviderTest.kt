package com.kmab.singalong.translation

import com.kmab.singalong.editor.LyricsEditor
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MyMemoryProviderTest {
    private class Cache(var allowance: Int = 5000) : SegmentCache {
        val lines = mutableMapOf<String, String>()
        var reserved = 0
        override suspend fun get(language: String, text: String) = lines["$language:$text"]
        override suspend fun put(language: String, text: String, translation: String) { lines["$language:$text"] = translation }
        override suspend fun reserve(characters: Int): Boolean {
            if (reserved + characters > allowance) return false
            reserved += characters; return true
        }
    }
    @Test fun publishesBeforeSlowSegmentsFinishAndLimitsEachBatchToTwoRequests() = runBlocking {
        MockWebServer().use { server ->
            val secondStarted = CountDownLatch(1)
            val releaseSecond = CountDownLatch(1)
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val text = request.requestUrl!!.queryParameter("q")
                    if (text == "İki") {
                        secondStarted.countDown()
                        check(releaseSecond.await(5, TimeUnit.SECONDS))
                    }
                    return MockResponse().setBody("""{"responseStatus":200,"responseData":{"translatedText":"Meaning $text"}}""")
                }
            }
            val provider = MyMemoryProvider(LanguageDetector { "tr" }, Cache(), baseUrl = server.url("/get").toString())
            val original = LyricsEditor.import("r", "Bir\nİki\nÜç")
            val first = CompletableDeferred<TranslationRecord>()
            val result = async { provider.translateProgressively(original, true) { first.complete(it) } }
            try {
                val partial = withTimeout(5000) { first.await() }
                assertEquals(1, partial.lines.size)
                assertTrue(withContext(Dispatchers.IO) { secondStarted.await(5, TimeUnit.SECONDS) })
                assertEquals(2, server.requestCount)
                assertFalse(result.isCompleted)
            } finally { releaseSecond.countDown() }
            val complete = withTimeout(5000) { result.await() } as TranslationResult.Available
            assertEquals(3, complete.record.lines.size)
            assertEquals(3, server.requestCount)
        }
    }
    @Test fun cachedSegmentsAfterMissingLinesAppearOfflineWithoutQuotaOrNetworkUse() = runBlocking {
        MockWebServer().use { server ->
            val cache = Cache().apply { lines["tr:İki"] = "Two" }
            val original = LyricsEditor.import("r", "Bir\nİki\nİki")
            val received = mutableListOf<TranslationRecord>()
            val provider = MyMemoryProvider(LanguageDetector { "tr" }, cache, baseUrl = server.url("/get").toString())
            assertEquals(TranslationResult.Unavailable, provider.translateProgressively(original, false) { received += it })
            assertEquals(original.lines.drop(1).map { it.id }.toSet(), received.single().lines.keys)
            assertEquals(0, server.requestCount)
            assertEquals(0, cache.reserved)
        }
    }
    @Test fun repeatedLinesReuseTranslationAndKeepOriginalLineIds() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"responseStatus":200,"responseData":{"translatedText":"Synthetic meaning"}}"""))
            val cache = Cache()
            val provider = MyMemoryProvider(LanguageDetector { "tr" }, cache, baseUrl = server.url("/get").toString())
            val original = LyricsEditor.import("r", "Merhaba dünya\nMerhaba dünya")
            val result = provider.translate(original) as TranslationResult.Available
            assertEquals(original.lines.map { it.id }.toSet(), result.record.lines.keys)
            assertEquals(1, server.requestCount)
            val request = server.takeRequest()
            assertEquals("tr|en", request.requestUrl?.queryParameter("langpair"))
            assertEquals("Merhaba dünya", request.requestUrl?.queryParameter("q"))
            assertEquals(original.lines.first().text.length, cache.reserved)
        }
    }
    @Test fun englishUnknownAndOversizedSegmentsNeverReachTheNetwork() = runBlocking {
        MockWebServer().use { server ->
            val cache = Cache()
            val original = LyricsEditor.import("r", "Synthetic original")
            for (language in listOf("en", "und")) {
                assertEquals(TranslationResult.Unsupported, MyMemoryProvider(LanguageDetector { language }, cache, baseUrl = server.url("/get").toString()).translate(original))
            }
            assertEquals(TranslationResult.Unsupported, MyMemoryProvider(LanguageDetector { "ja" }, cache, baseUrl = server.url("/get").toString())
                .translate(LyricsEditor.import("r", "界".repeat(200))))
            assertEquals(0, server.requestCount)
            assertEquals(0, cache.reserved)
        }
    }
    @Test fun quotaAndNetworkPolicyStopBeforeSendingAndFailedResponsesAreNotSavedAsWords() = runBlocking {
        MockWebServer().use { server ->
            val original = LyricsEditor.import("r", "Merhaba dünya")
            val denied = MyMemoryProvider(LanguageDetector { "tr" }, Cache(), baseUrl = server.url("/get").toString(), permitsNetwork = { false })
            assertEquals(TranslationResult.Unavailable, denied.translate(original))
            val quota = MyMemoryProvider(LanguageDetector { "tr" }, Cache(1), baseUrl = server.url("/get").toString())
            assertEquals(TranslationResult.Unavailable, quota.translate(original))
            assertEquals(0, server.requestCount)
            server.enqueue(MockResponse().setBody("""{"responseStatus":403,"responseDetails":"Quota exceeded"}"""))
            val cache = Cache()
            val failed = MyMemoryProvider(LanguageDetector { "tr" }, cache, baseUrl = server.url("/get").toString())
            assertEquals(TranslationResult.Failed, failed.translate(original))
            assertTrue(cache.lines.isEmpty())
        }
    }
}
