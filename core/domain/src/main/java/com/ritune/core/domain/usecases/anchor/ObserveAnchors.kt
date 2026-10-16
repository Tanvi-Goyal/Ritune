package com.ritune.core.domain.usecases.anchor

import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.models.Anchor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAnchors @Inject constructor(
    private val repository: AnchorRepository
) {
    operator fun invoke(): Flow<List<Anchor>> = repository.observeAll()
}
