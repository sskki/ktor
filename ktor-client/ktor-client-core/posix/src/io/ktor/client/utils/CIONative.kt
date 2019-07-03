/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.utils

import io.ktor.client.*
import io.ktor.util.coroutines.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * Run request blocking in [HttpClient] dispatcher.
 */
actual fun <T> HttpClient.runBlocking(block: suspend CoroutineScope.() -> T): T =
    runBlockingWithHelp(block)
