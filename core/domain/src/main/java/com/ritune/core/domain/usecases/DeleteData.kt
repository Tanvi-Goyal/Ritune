package com.ritune.core.domain.usecases

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.domain.ports.DataResetter
import javax.inject.Inject

class DeleteAllData @Inject constructor(
    private val resetter: DataResetter,
    private val coach: CoachGateway,
) {
    suspend operator fun invoke() {
        resetter.resetAll()
        coach.forgetUser()
    }
}
