package com.ritune.core.testing.ports

import com.ritune.core.domain.ports.DataResetter

class FakeDataResetter : DataResetter {

    var resetCount = 0
        private set

    override suspend fun resetAll() {
        resetCount++
    }
}
