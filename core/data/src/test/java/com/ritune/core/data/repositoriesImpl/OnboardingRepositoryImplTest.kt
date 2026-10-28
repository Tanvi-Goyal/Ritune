package com.ritune.core.data.repositoriesImpl

import app.cash.turbine.test
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingRepositoryImplTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun TestScope.newRepository() = OnboardingRepositoryImpl(
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tmpFolder.root, "onboarding.preferences_pb") },
        ),
    )

    @Test
    fun isCompleted_newStore_isFalse() = runTest {
        newRepository().isCompleted.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun setCompleted_updatesIsCompleted() = runTest {
        val repository = newRepository()

        repository.isCompleted.test {
            assertFalse(awaitItem())

            repository.setCompleted(true)
            assertTrue(awaitItem())

            repository.setCompleted(false)
            assertFalse(awaitItem())
        }
    }
}
