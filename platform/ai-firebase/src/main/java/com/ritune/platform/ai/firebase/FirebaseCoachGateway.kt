package com.ritune.platform.ai.firebase

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.auth.auth
import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachSetup
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.ModelEvent
import com.ritune.platform.android.Logger
import com.ritune.platform.android.di.Dispatcher
import com.ritune.platform.android.di.RituneDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Talks to Gemini through Firebase AI Logic. The SDK attaches the App Check token. */
@Singleton
class FirebaseCoachGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(RituneDispatchers.IO) private val io: CoroutineDispatcher,
    private val logger: Logger,
    private val appCheckSetup: AppCheckSetup,
) : CoachGateway {

    // Firebase starts on the first coach request, not at app start, so cold start doesn't pay
    // for it. The app manifest removes Firebase's own start-up provider.
    private val app by lazy {
        checkNotNull(FirebaseApp.initializeApp(context)) { "google-services.json is missing" }
            .also { appCheckSetup.install() }
    }

    private val ai by lazy { Firebase.ai(app, GenerativeBackend.googleAI()) }

    // Each Chat resends its conversation's earlier turns, including the tool calls and results.
    private val chats = ConcurrentHashMap<ConversationId, Chat>()

    override fun send(
        conversation: ConversationId,
        setup: CoachSetup,
        input: CoachInput,
    ): Flow<ModelEvent> = flow {
        signIn()
        val chat = chats.getOrPut(conversation) { model(setup).startChat() }
        var usage = CoachUsage()
        chat.sendMessageStream(input.toContent()).collect { response ->
            response.text?.takeIf { it.isNotEmpty() }?.let { emit(ModelEvent.Text(it)) }
            response.functionCalls.forEach { emit(ModelEvent.ToolCallRequest(it.toToolCall())) }
            response.usageMetadata?.let { usage = it.toCoachUsage() }
        }
        emit(ModelEvent.Finished(usage))
    }.catch { e ->
        if (e is CancellationException) throw e
        logger.error(TAG, "Gemini request failed", e)
        throw CoachException(e.toCoachError(), e)
    }.flowOn(io) // Starting Firebase reads its config from disk.

    override fun close(conversation: ConversationId) {
        chats.remove(conversation)
    }

    private suspend fun signIn() {
        val auth = Firebase.auth(app)
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    private fun model(setup: CoachSetup) =
        ai.generativeModel(
            modelName = MODEL,
            tools = listOf(Tool.functionDeclarations(setup.tools.map { it.toDeclaration() })),
            systemInstruction = content { text(setup.instructions) },
            requestOptions = RequestOptions(timeoutInMillis = TIMEOUT_MILLIS),
        )

    private companion object {
        const val TAG = "CoachGateway"

        // Fastest and steadiest in the Step 1 lab runs; revisit once tools are measured.
        const val MODEL = "gemini-3.5-flash-lite"
        const val TIMEOUT_MILLIS = 60_000L
    }
}
