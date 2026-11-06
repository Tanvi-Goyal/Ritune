package com.ritune

import com.ritune.core.domain.usecases.InitializeApp
import com.ritune.core.testing.ports.FakeAppInitializer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupTest {

    private val initializer = FakeAppInitializer()

    @Test
    fun start_initializesOnce() = runTest {
        val startup = AppStartup(InitializeApp(initializer), UnconfinedTestDispatcher(testScheduler))

        startup.start()
        startup.start()
        startup.awaitInitialized()

        assertEquals(1, initializer.initializeCount)
    }

    @Test
    fun start_runsBeforeAnyoneWaits() = runTest {
        val startup = AppStartup(InitializeApp(initializer), UnconfinedTestDispatcher(testScheduler))

        startup.start()

        assertEquals(1, initializer.initializeCount)
    }

    @Test
    fun awaitInitialized_withoutStart_initializesAndWaits() = runTest {
        initializer.gate = CompletableDeferred()
        val startup = AppStartup(InitializeApp(initializer), UnconfinedTestDispatcher(testScheduler))
        var done = false

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            startup.awaitInitialized()
            done = true
        }

        assertFalse(done)
        initializer.gate!!.complete(Unit)
        assertTrue(done)
    }
}
