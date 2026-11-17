package com.ritune.profiling

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

fun interface ProfileUploadScheduler {
    fun schedule(profilePath: String)
}

class WorkManagerProfileUploadScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ProfileUploadScheduler {

    override fun schedule(profilePath: String) {
        // Profiles are several MB, so wait for Wi-Fi, charging and idle.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()
        val request = OneTimeWorkRequestBuilder<TraceUploadWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(TraceUploadWorker.KEY_PROFILE_PATH, profilePath).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
