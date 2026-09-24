package com.example.gymapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.ZoneId
import java.time.ZonedDateTime

object HealthConnectManager {

    private const val TAG = "HealthConnectDebug"

    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    )

    fun getSdkStatus(context: Context): Int {
        val status = HealthConnectClient.getSdkStatus(context)
        Log.d(TAG, "SDK Status: $status")
        return status
    }

    fun isAvailable(context: Context) =
        getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun getClient(context: Context): HealthConnectClient? {
        if (!isAvailable(context)) return null
        return try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.e(TAG, "getOrCreate failed: ${e.message}")
            null
        }
    }

    suspend fun hasAllPermissions(context: Context): Boolean {
        val client = getClient(context) ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            Log.d(TAG, "Granted: $granted | Required: $PERMISSIONS")
            granted.containsAll(PERMISSIONS)
        } catch (e: Exception) {
            Log.e(TAG, "hasAllPermissions failed: ${e.message}")
            false
        }
    }

    /**
     * Returns the RAW step count from the phone sensor for TODAY (midnight → now).
     * This is the absolute sensor reading — NOT adjusted for any user baseline.
     * HomeFragment is responsible for subtracting the user's baseline offset.
     */
    suspend fun getRawTodaySteps(context: Context): Pair<Int, Int> {
        val client = getClient(context) ?: return Pair(0, 0)
        return try {
            val now        = ZonedDateTime.now()
            val startOfDay = now.toLocalDate().atStartOfDay(ZoneId.systemDefault())
            val timeRange  = TimeRangeFilter.between(startOfDay.toInstant(), now.toInstant())
            fetchStepsInRange(client, timeRange)
        } catch (e: Exception) {
            Log.e(TAG, "getRawTodaySteps failed: ${e.message}")
            Pair(0, 0)
        }
    }

    /**
     * Returns the RAW step count for YESTERDAY (midnight → midnight).
     * HomeFragment handles baseline subtraction separately for yesterday.
     */
    suspend fun getRawYesterdaySteps(context: Context): Pair<Int, Int> {
        val client = getClient(context) ?: return Pair(0, 0)
        return try {
            val now          = ZonedDateTime.now()
            val startOfToday = now.toLocalDate().atStartOfDay(ZoneId.systemDefault())
            val startOfYest  = startOfToday.minusDays(1)
            val timeRange    = TimeRangeFilter.between(
                startOfYest.toInstant(),
                startOfToday.toInstant()
            )
            fetchStepsInRange(client, timeRange)
        } catch (e: Exception) {
            Log.e(TAG, "getRawYesterdaySteps failed: ${e.message}")
            Pair(0, 0)
        }
    }

    private suspend fun fetchStepsInRange(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter
    ): Pair<Int, Int> {
        val response = client.aggregate(
            AggregateRequest(
                metrics         = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = timeRange
            )
        )
        val steps         = response[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
        val activeMinutes = steps / 100
        Log.d(TAG, "Steps in range: $steps")
        return Pair(steps, activeMinutes)
    }

    fun openHealthConnectApp(context: Context) {
        try {
            val intent = context.packageManager
                .getLaunchIntentForPackage("com.google.android.apps.healthdata")
            if (intent != null) context.startActivity(intent)
            else openPlayStore(context)
        } catch (e: Exception) { openPlayStore(context) }
    }

    fun openPlayStore(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                ).setPackage("com.android.vending")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open Play Store: ${e.message}")
        }
    }
}