package com.ritune.profiling

import android.os.ProfilingResult
import javax.inject.Inject

class AnrProfileResultHandler @Inject constructor(
    private val scheduler: ProfileUploadScheduler,
) {
    fun onResult(errorCode: Int, resultFilePath: String?) {
        if (errorCode == ProfilingResult.ERROR_NONE && resultFilePath != null) {
            scheduler.schedule(resultFilePath)
        }
    }
}
