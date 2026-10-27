package com.ritune.core.testing.ports

import com.ritune.core.domain.initialization.AppInitializer
import kotlinx.coroutines.CompletableDeferred

class FakeAppInitializer : AppInitializer {

    var initializeCount = 0
        private set

    var gate: CompletableDeferred<Unit>? = null

    override suspend fun initialize() {
        initializeCount++
        gate?.await()
    }
}
