package com.ritune.profiling

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

class TraceUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun uploader(): ProfileUploader
    }

    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_PROFILE_PATH) ?: return Result.failure()
        val file = File(path)
        // The system also deletes old profiles, so the file can be gone by the time we run.
        if (!file.exists()) return Result.success()

        val uploader = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).uploader()
        return if (uploader.upload(file)) {
            file.delete()
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val KEY_PROFILE_PATH = "PROFILE_PATH"
    }
}
