package com.ritune.core.domain.ports

import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachSetup
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.ModelEvent
import kotlinx.coroutines.flow.Flow

/**
 * The only way the domain talks to an AI model. The gateway keeps each conversation's earlier
 * turns, because the model itself remembers nothing between requests.
 * Failures are thrown as [com.ritune.core.models.coach.CoachException].
 */
interface CoachGateway {
    fun send(
        conversation: ConversationId,
        setup: CoachSetup,
        input: CoachInput
    ): Flow<ModelEvent>

    fun close(conversation: ConversationId)

    suspend fun forgetUser()
}
