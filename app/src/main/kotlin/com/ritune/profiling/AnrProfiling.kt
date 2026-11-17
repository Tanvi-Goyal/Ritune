package com.ritune.profiling

import android.content.Context
import android.os.Build
import android.os.ProfilingManager
import android.os.ProfilingResult
import android.os.ProfilingTrigger
import android.util.Log
import androidx.annotation.RequiresApi
import com.ritune.AppStartup
import com.ritune.platform.android.di.Dispatcher
import com.ritune.platform.android.di.RituneDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects a system profile whenever the app has an ANR and hands it to [ProfileUploadScheduler].
 * Waits for [AppStartup] so none of this competes with cold start.
 */
@Singleton
class AnrProfiling @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val startup: AppStartup,
    private val handler: AnrProfileResultHandler,
    @Dispatcher(RituneDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        scope.launch {
            startup.awaitInitialized()
            // Diagnostics must never take the app down, and some environments have no profiling service.
            runCatching { register() }.onFailure { Log.w(TAG, "ANR profiling not registered", it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun register() {
        val manager = context.getSystemService(ProfilingManager::class.java) ?: return
        // The listener is not persisted, so it is registered on every launch. The trigger is
        // persisted by the system and re-adding it is harmless.
        manager.registerForAllProfilingResults(
            Executor { scope.launch { it.run() } },
            Consumer<ProfilingResult> { handler.onResult(it.errorCode, it.resultFilePath) },
        )
        manager.addProfilingTriggers(
            listOf(
                ProfilingTrigger.Builder(ProfilingTrigger.TRIGGER_TYPE_ANR)
                    .setRateLimitingPeriodHours(RATE_LIMIT_HOURS)
                    .build(),
            ),
        )
    }

    private companion object {
        const val TAG = "AnrProfiling"
        const val RATE_LIMIT_HOURS = 24
    }
}
