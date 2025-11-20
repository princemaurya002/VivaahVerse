package com.princemaurya.vivaahaverse.data.remote

import kotlin.jvm.Volatile

object AuthTokenProvider {
    @Volatile
    var token: String? = null
}

