package com.ritune.core.testing.ports

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachSetup
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.ModelEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Answers each request with the next scripted reply, in order. */
class FakeCoachGateway : CoachGateway {

    private val replies = ArrayDeque<List<ModelEvent>>()

    var failure: CoachException? = null
    val sent = mutableListOf<CoachInput>()
    val closed = mutableListOf<ConversationId>()

    fun reply(vararg events: ModelEvent) {
        replies += events.toList()
    }

    override fun send(
        conversation: ConversationId,
        setup: CoachSetup,
        input: CoachInput,
    ): Flow<ModelEvent> = flow {
        sent += input
        failure?.let { throw it }
        val reply = checkNotNull(replies.removeFirstOrNull()) { "No reply scripted for $input" }
        reply.forEach { emit(it) }
    }

    override fun close(conversation: ConversationId) {
        closed += conversation
    }
}
