package com.ritune

import com.ritune.core.domain.usecases.InitializeApp
import com.ritune.core.testing.ports.FakeAppInitializer
import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.feature.today.ui.navigation.TodayRoute
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val initializer = FakeAppInitializer()

    @Test
    fun startDestination_staysUnsetUntilInitializationFinishes() {
        initializer.gate = CompletableDeferred()

        val viewModel = MainViewModel(InitializeApp(initializer))

        assertNull(viewModel.startDestination.value)
        initializer.gate!!.complete(Unit)
        assertEquals(TodayRoute, viewModel.startDestination.value)
    }

    @Test
    fun init_initializesTheAppOnce() {
        MainViewModel(InitializeApp(initializer))

        assertEquals(1, initializer.initializeCount)
    }
}
