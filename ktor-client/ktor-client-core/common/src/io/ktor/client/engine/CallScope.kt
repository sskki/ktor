/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine

import kotlinx.coroutines.*
import kotlinx.io.core.*
import kotlin.coroutines.*

/**
 * Base jvm implementation for [HttpClientEngine]
 */
@Suppress("KDocMissingDocumentation")
abstract class CallScope(name: String) : CoroutineScope, Closeable {
    private val clientContext = SupervisorJob()

    abstract val dispatcher: CoroutineDispatcher

    override val coroutineContext: CoroutineContext by lazy {
        dispatcher + clientContext + CoroutineName("$name-context")
    }

    /**
     * Create [CoroutineContext] to execute call.
     */
    @UseExperimental(InternalCoroutinesApi::class)
    protected suspend fun createCallContext(): CoroutineContext {
        val callJob = Job(clientContext)
        val callContext = coroutineContext + callJob

        val parentCoroutineJob = currentContext()[Job]
        val onParentCancelCleanupHandle = parentCoroutineJob?.invokeOnCompletion(
            onCancelling = true
        ) { cause ->
            if (cause != null) callContext.cancel()
        }

        callJob.invokeOnCompletion {
            onParentCancelCleanupHandle?.dispose()
        }

        return callContext
    }

    override fun close() {
        clientContext.complete()

        clientContext.invokeOnCompletion {
            val current = dispatcher
            if (current is Closeable) {
                current.close()
            }
        }
    }
}

private suspend inline fun currentContext() = coroutineContext
