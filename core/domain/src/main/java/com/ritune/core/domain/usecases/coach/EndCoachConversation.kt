package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.models.coach.ConversationId
import javax.inject.Inject

/** Forgets a conversation's earlier turns, so they're no longer sent with each request. */
class EndCoachConversation @Inject constructor(private val gateway: CoachGateway) {
    operator fun invoke(conversation: ConversationId) = gateway.close(conversation)
}
