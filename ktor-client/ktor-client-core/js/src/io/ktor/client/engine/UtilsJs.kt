/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine

import io.ktor.util.*
import kotlinx.coroutines.*

@InternalAPI
actual fun createClientDispatcher(threadsCount: Int): CoroutineDispatcher = Dispatchers.Unconfined
