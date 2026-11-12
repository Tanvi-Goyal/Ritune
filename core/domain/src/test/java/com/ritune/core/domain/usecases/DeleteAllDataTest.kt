package com.ritune.core.domain.usecases

import com.ritune.core.testing.ports.FakeCoachGateway
import com.ritune.core.testing.ports.FakeDataResetter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeleteAllDataTest {

    private val resetter = FakeDataResetter()
    private val gateway = FakeCoachGateway()
    private val deleteAllData = DeleteAllData(resetter, gateway)

    @Test
    fun invoke_resetsDataAndForgetsCoachUser() = runTest {
        deleteAllData()

        assertEquals(1, resetter.resetCount)
        assertEquals(1, gateway.forgotUser)
    }

    @Test
    fun invoke_resetFails_keepsCoachUser() = runTest {
        resetter.error = IOException("disk full")

        assertFailsWith<IOException> { deleteAllData() }

        assertEquals(0, gateway.forgotUser)
    }
}
