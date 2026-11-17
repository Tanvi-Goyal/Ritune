package com.ritune.profiling

import java.io.File
import javax.inject.Inject

/** Sends one profile to wherever Ritune collects them. Returns true once the backend has it. */
fun interface ProfileUploader {
    suspend fun upload(profile: File): Boolean
}

/** Placeholder until a destination exists. Reports failure so the profile is kept, not deleted. */
class UnconfiguredProfileUploader @Inject constructor() : ProfileUploader {
    override suspend fun upload(profile: File): Boolean = false
}
