package com.ritune

import androidx.tracing.traceAsync
import com.ritune.core.domain.usecases.InitializeApp
import com.ritune.platform.android.di.Dispatcher
import com.ritune.platform.android.di.RituneDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens and seeds the database once per process, started from Application.onCreate. The splash
 * waits for it, and MainViewModel is only created about 70 ms later, after the activity's own
 * setup, so starting here gives the read that head start.
 */
@Singleton
class AppStartup @Inject constructor(
    private val initializeApp: InitializeApp,
    @Dispatcher(RituneDispatchers.Default) dispatcher: CoroutineDispatcher,
) {
    private val initialized: Deferred<Unit> =
        CoroutineScope(SupervisorJob() + dispatcher).async(start = CoroutineStart.LAZY) {
            traceAsync("InitializeApp", cookie = 0) { initializeApp() }
        }

    fun start() {
        initialized.start()
    }

    suspend fun awaitInitialized() {
        initialized.await()
    }
}
