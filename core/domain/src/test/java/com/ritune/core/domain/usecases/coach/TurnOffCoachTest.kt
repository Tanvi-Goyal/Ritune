package com.ritune.core.domain.usecases.coach

import com.ritune.core.models.AppSettings
import com.ritune.core.testing.ports.FakeCoachGateway
import com.ritune.core.testing.repository.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TurnOffCoachTest {

    @Test
    fun invoke_coachOn_savesOffAndForgetsCoachUser() = runTest {
        val settings = FakeSettingsRepository(AppSettings(coachEnabled = true))
        val gateway = FakeCoachGateway()

        TurnOffCoach(settings, gateway)()

        assertFalse(settings.settings.value.coachEnabled)
        assertEquals(1, gateway.forgotUser)
    }
}
