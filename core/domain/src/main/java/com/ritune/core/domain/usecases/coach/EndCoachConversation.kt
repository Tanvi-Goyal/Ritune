package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.models.coach.ConversationId
import javax.inject.Inject

class EndCoachConversation @Inject constructor(private val gateway: CoachGateway) {
    operator fun invoke(conversation: ConversationId) = gateway.close(conversation)
}
