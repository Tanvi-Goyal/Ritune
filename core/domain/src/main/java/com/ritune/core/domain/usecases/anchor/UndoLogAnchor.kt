package com.ritune.core.domain.usecases.anchor

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.models.EventId
import javax.inject.Inject

/** Removes a just-logged event (the undo on the "logged" snackbar). */
class UndoLogAnchor @Inject constructor(
    private val anchorEventRepository: AnchorEventRepository
) {
    suspend operator fun invoke(eventId: EventId) = anchorEventRepository.remove(eventId)
}
