package com.ritune.core.domain.usecases

import com.ritune.core.domain.initialization.AppInitializer
import javax.inject.Inject

class InitializeApp @Inject constructor(
    private val appInitializer: AppInitializer,
) {
    suspend operator fun invoke() = appInitializer.initialize()
}
