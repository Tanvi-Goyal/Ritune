package com.ritune.evals

import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.ritune.ailab.installDebugAppCheck
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
        val secret = args.getString(SECRET_ARG)
        assumeTrue("No App Check debug token, so the evals are skipped", !secret.isNullOrBlank())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = checkNotNull(FirebaseApp.initializeApp(context))
        // The debug provider reads its token from this file. Firebase's debug-testing library
        // does the same from test arguments, but it breaks the debug app outside of tests.
        context.getSharedPreferences(DEBUG_STORE + app.persistenceKey, Context.MODE_PRIVATE)
            .edit().putString(DEBUG_SECRET_KEY, secret).commit()
        installDebugAppCheck()
        val gateway = FirebaseCoachGateway(context, Dispatchers.IO, AndroidLogger(), AppCheckSetup {})
        val only = args.getString(ONLY_ARG)?.split(",")?.map(String::trim)?.toSet()
        val scenarios = CoachEvalScenarios.all.filter { only == null || it.id in only }

        val report = runBlocking {
            CoachEvalRunner(gateway).run(scenarios) { log(it.transcript()) }
        }
        log(report.toMarkdown())
    }

    // Logcat cuts long entries, so log line by line.
    private fun log(text: String) = text.lines().forEach { Log.i(TAG, it) }

    private companion object {
        const val TAG = "CoachEval"
        const val SECRET_ARG = "firebaseAppCheckDebugSecret"
        const val ONLY_ARG = "evalScenarios"
        const val DEBUG_STORE = "com.google.firebase.appcheck.debug.store."
        const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
    }
}
