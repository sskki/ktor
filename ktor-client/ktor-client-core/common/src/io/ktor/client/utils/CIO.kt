/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.utils

import io.ktor.client.*
import io.ktor.util.*
import kotlinx.coroutines.*

/**
 * Maximum number of buffers to be allocated in the [HttpClientDefaultPool].
 */
const val DEFAULT_HTTP_POOL_SIZE: Int = 1000

/**
 * Size of each buffer in the [HttpClientDefaultPool].
 */
const val DEFAULT_HTTP_BUFFER_SIZE: Int = 4096

/**
 * Run request blocking in [HttpClient] dispatcher.
 */
@KtorExperimentalAPI
expect fun <T> HttpClient.runBlocking(block: suspend CoroutineScope.() -> T): T
