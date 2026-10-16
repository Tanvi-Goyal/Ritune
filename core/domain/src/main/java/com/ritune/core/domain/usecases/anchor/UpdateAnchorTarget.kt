package com.ritune.core.domain.usecases.anchor

import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.models.AnchorId
import kotlinx.datetime.LocalTime
import javax.inject.Inject

/** Change an anchor's target time (A-6). Pass null to clear it. */
class UpdateAnchorTarget @Inject constructor(
    private val anchorRepository: AnchorRepository
) {
    suspend operator fun invoke(
        anchorId: AnchorId,
        target: LocalTime?
    ) = anchorRepository.updateTarget(anchorId, target)
}