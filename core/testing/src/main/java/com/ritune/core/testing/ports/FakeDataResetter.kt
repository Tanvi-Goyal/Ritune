package com.ritune.core.testing.ports

import com.ritune.core.domain.ports.DataResetter
import kotlinx.coroutines.CompletableDeferred

class FakeDataResetter : DataResetter {

    var resetCount = 0
        private set

    var error: Throwable? = null

    var gate: CompletableDeferred<Unit>? = null

    override suspend fun resetAll() {
        resetCount++
        gate?.await()
        error?.let { throw it }
    }
}
