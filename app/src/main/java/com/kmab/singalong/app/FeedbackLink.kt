package com.kmab.singalong.app

import okhttp3.HttpUrl.Companion.toHttpUrl

object FeedbackLink {
    fun url(version: String, type: String = "other"): String = "https://kmab5.github.io/kmab-feedback/".toHttpUrl().newBuilder()
        .addQueryParameter("product", "singalong")
        .addQueryParameter("source", "android")
        .addQueryParameter("version", version)
        .addQueryParameter("type", type)
        .addQueryParameter("ctx_screen", "about")
        .build().toString()
}
