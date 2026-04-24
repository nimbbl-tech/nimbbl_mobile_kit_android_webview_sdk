package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_LOCATION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_UPI_APPS
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_UPI_APP_NAME
import tech.nimbbl.webviewsdk.constants.PaymentConstants.LOCATION_UPI_PARAMETER_VALIDATION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.SDK_VERSION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UPI_PARAM_PACKAGE_NAME
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UPI_PARAM_TRANSACTION_ID
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UPI_PARAM_URL

class UPIManager(private val context: Context) {

    companion object {
        private const val TAG = "UPIManager"
        private const val UPI_SCHEME = "upi"
        private const val UPI_AUTHORITY = "pay"
    }

    /**
     * Query installed UPI apps on a background thread and deliver the result on the main thread.
     * Falls back to an empty list on any failure so the caller always receives a valid JSONObject.
     */
    fun createUPIAppsJSON(onResult: (JSONObject) -> Unit) {
        // The caller's isDestroyed() guard in the callback prevents acting on a destroyed WebView.
        // Package-manager query itself is lightweight and always completes quickly.
        Thread({
            val result = try {
                buildUPIAppsJSON()
            } catch (e: Exception) {
                SDKUtils.logError(TAG, "Error querying UPI apps: ${e.message}", e)
                JSONObject().put(JSON_KEY_UPI_APPS, JSONArray())
            }
            Handler(Looper.getMainLooper()).post { onResult(result) }
        }, "nimbbl-upi-query").start()
    }

    private fun buildUPIAppsJSON(): JSONObject {
        val packageManager = context.packageManager
        val upiIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.Builder().scheme(UPI_SCHEME).authority(UPI_AUTHORITY).build()
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            // CATEGORY_BROWSABLE intentionally omitted — it matches web browsers, not UPI apps
        }

        val pkgAppsList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(upiIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(upiIntent, 0)
        }

        val jsonArray = JSONArray()
        for (resolveInfo in pkgAppsList) {
            jsonArray.put(JSONObject().apply {
                put(JSON_KEY_UPI_APP_NAME, resolveInfo.loadLabel(packageManager).toString())
                put(UPI_PARAM_PACKAGE_NAME, resolveInfo.activityInfo.packageName)
            })
        }

        return JSONObject().put(JSON_KEY_UPI_APPS, jsonArray)
    }

    /**
     * Validate UPI parameters received from JavaScript.
     * @return true if all required fields are present and non-empty
     */
    fun validateUPIParameters(parameters: String, orderId: String, token: String, subMerchantId: String): Boolean {
        return try {
            val jsonObject = JSONObject(parameters)
            val requiredFields = listOf(UPI_PARAM_URL, UPI_PARAM_PACKAGE_NAME, UPI_PARAM_TRANSACTION_ID)

            requiredFields.all { field ->
                // getString never returns null — isNotEmpty() is sufficient
                jsonObject.has(field) && jsonObject.getString(field).isNotEmpty()
            }
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Invalid UPI parameters: ${e.message}", e)
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EventConstants.EXCEPTION_THROWN,
                orderId,
                token,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    JSON_KEY_ERROR to (e.message ?: UNKNOWN_ERROR),
                    JSON_KEY_LOCATION to LOCATION_UPI_PARAMETER_VALIDATION
                    // Raw parameters omitted — the string may contain sensitive payment tokens
                ),
                sdkVersion = SDK_VERSION
            )
            false
        }
    }
}
