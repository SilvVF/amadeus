package io.silv.data.download

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.silv.common.model.NetworkConnectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * This worker is used to manage the downloader. The system can decide to stop the worker, in
 * which case the downloader is also stopped. It's also stopped while there's no network available.
 */
class DownloadWorker(
    applicationContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(applicationContext, workerParams), KoinComponent {

    private val downloadManager by inject<DownloadManager>()
    private val connectivityManager by inject<NetworkConnectivity>()

    private val isOnline = connectivityManager.online.stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.Eagerly, true)

    override suspend fun doWork(): Result {

        var active = checkConnectivity() && downloadManager.downloaderStart()

        Log.d("DownloadWorker", "starting work active $active")

        if (!active) {
            return Result.failure()
        }

        // Keep the worker running when needed
        while (active) {
            delay(100)
            active = !isStopped && downloadManager.isRunning && checkConnectivity()
        }

        Log.d("DownloadWorker", "ending work not active")

        return Result.success()
    }

    private fun checkConnectivity(): Boolean {
        return (isOnline.value).also { online ->
            if (!online) {
                downloadManager.downloaderStop("no network connection")
            }
        }

    }

    companion object {
        private const val TAG = "Downloader"

        fun start(context: Context) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(TAG)
        }

        fun isRunning(context: Context): Boolean {
            return runBlocking {
                WorkManager.getInstance(context)
                    .getWorkInfosByTagFlow(TAG)
                    .first()
                    .let { list -> list.count { it.state == WorkInfo.State.RUNNING } == 1 }
            }
        }

        fun isRunningFlow(context: Context): Flow<Boolean> {
            return WorkManager.getInstance(context)
                .getWorkInfosByTagFlow(TAG)
                .map { list -> list.count { it.state == WorkInfo.State.RUNNING } == 1 }
        }
    }
}