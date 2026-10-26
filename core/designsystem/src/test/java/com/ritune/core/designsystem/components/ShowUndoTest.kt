package com.ritune.core.designsystem.components

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ShowUndoTest {

    private val hostState = SnackbarHostState()

    @Test
    fun showUndo_staysVisibleForTenSeconds() = runTest {
        backgroundScope.async { hostState.showUndo("Wake logged", "Undo") }

        advanceTimeBy(9.seconds)
        runCurrent()

        assertNotNull(hostState.currentSnackbarData)
    }

    @Test
    fun showUndo_afterTimeout_dismissesAndReturnsFalse() = runTest {
        val undone = async { hostState.showUndo("Wake logged", "Undo") }

        advanceTimeBy(10.seconds)
        runCurrent()

        assertFalse(undone.await())
        assertNull(hostState.currentSnackbarData)
    }

    @Test
    fun showUndo_actionTapped_returnsTrue() = runTest {
        val undone = async { hostState.showUndo("Wake logged", "Undo") }
        runCurrent()

        hostState.currentSnackbarData!!.performAction()

        assertTrue(undone.await())
    }

    @Test
    fun showUndo_customTimeout_isRespected() = runTest {
        val undone = async { hostState.showUndo("Wake logged", "Undo", timeoutMs = 30_000) }

        advanceTimeBy(29.seconds)
        runCurrent()
        assertNotNull(hostState.currentSnackbarData)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(false, undone.await())
    }
}
