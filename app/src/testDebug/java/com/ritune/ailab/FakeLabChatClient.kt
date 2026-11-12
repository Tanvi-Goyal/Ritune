package com.ritune.ailab

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Replies with [chunks], waiting [delayMillis] of virtual time before each one.
 * Lives beside the lab tests rather than in :core:testing because the lab is debug-only.
 */
class FakeLabChatClient : LabChatClient {

    var chunks: List<LabChunk> = emptyList()
    var delayMillis: Long = 0
    var failure: Exception? = null
    var neverFinishes: Boolean = false

    val requests = mutableListOf<LabRequest>()
    var historyCleared = 0
        private set

    override fun stream(request: LabRequest): Flow<LabChunk> = flow {
        requests += request
        failure?.let { throw it }
        chunks.forEach {
            delay(delayMillis)
            emit(it)
        }
        if (neverFinishes) awaitCancellation()
    }

    override fun clearHistory() {
        historyCleared++
    }
}
