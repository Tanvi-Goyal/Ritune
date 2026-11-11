package com.ritune.evals

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.debug.testing.DebugAppCheckTestHelper
import com.ritune.core.testing.evals.CoachEvalRunner
import com.ritune.core.testing.evals.CoachEvalScenarios
import com.ritune.core.testing.evals.transcript
import com.ritune.platform.ai.firebase.AppCheckSetup
import com.ritune.platform.ai.firebase.FirebaseCoachGateway
import com.ritune.platform.android.AndroidLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Scores the coach on the eval set with the real model. It costs quota, so it runs only by
 * hand, when a registered App Check debug token is passed in. CI never passes one, so there
 * it is skipped. See docs/coach-evals.md.
 */
class CoachEvalTest {
    private val args = InstrumentationRegistry.getArguments()

    @Test
    fun scoreTheCoach() {
        assumeTrue(
            "No App Check debug token, so the evals are skipped",
            !args.getString(SECRET_ARG).isNullOrBlank(),
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        FirebaseApp.initializeApp(context)
        // The helper installs the debug provider with the token, so the gateway installs nothing.
        val gateway = FirebaseCoachGateway(context, Dispatchers.IO, AndroidLogger(), AppCheckSetup {})
        val only = args.getString(ONLY_ARG)?.split(",")?.map(String::trim)?.toSet()
        val scenarios = CoachEvalScenarios.all.filter { only == null || it.id in only }

        DebugAppCheckTestHelper.fromInstrumentationArgs().withDebugProvider<RuntimeException> {
            val report = runBlocking {
                CoachEvalRunner(gateway).run(scenarios) { log(it.transcript()) }
            }
            log(report.toMarkdown())
        }
    }

    // Logcat cuts long entries, so log line by line.
    private fun log(text: String) = text.lines().forEach { Log.i(TAG, it) }

    private companion object {
        const val TAG = "CoachEval"
        const val SECRET_ARG = "firebaseAppCheckDebugSecret"
        const val ONLY_ARG = "evalScenarios"
    }
}
