package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_LOCATION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_PARAMETERS
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_UPI_APPS
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_UPI_APP_NAME
import tech.nimbbl.webviewsdk.constants.PaymentConstants.LOCATION_UPI_PARAMETER_VALIDATION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.SDK_VERSION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.TAG_SAN
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UPI_PARAM_PACKAGE_NAME
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UPI_PARAM_TRANSACTION_ID
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UPI_PARAM_URL

/**
 * Manager class for handling UPI (Unified Payments Interface) operations
 */
class UPIManager(private val context: Context) {
    
    companion object {
        private const val UPI_SCHEME = "upi"
        private const val UPI_AUTHORITY = "pay"
    }

    /**
     * Create JSON object with UPI apps information for WebView
     * @return JSON object containing UPI apps data
     */
    fun createUPIAppsJSON(): JSONObject {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            action = Intent.ACTION_VIEW
            data = Uri.Builder().scheme(UPI_SCHEME).authority(UPI_AUTHORITY).build()
        }
        
        val pkgAppsList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)
        val jsonArray = JSONArray()
            // Process actual UPI apps
            for (resolveInfo in pkgAppsList) {
                val appObj = JSONObject().apply {
                    put(JSON_KEY_UPI_APP_NAME, resolveInfo.loadLabel(packageManager).toString())
                    put(UPI_PARAM_PACKAGE_NAME, resolveInfo.activityInfo.packageName)
                }
                jsonArray.put(appObj)
            }

        return JSONObject().put(JSON_KEY_UPI_APPS, jsonArray)
    }
    
    /**
     * Validate UPI parameters received from JavaScript
     * @param parameters JSON string containing UPI parameters
     * @return true if parameters are valid
     */
    fun validateUPIParameters(parameters: String,orderId:String,token:String,subMerchantId: String): Boolean {
        return try {
            val jsonObject = JSONObject(parameters)
            val requiredFields = listOf(UPI_PARAM_URL, UPI_PARAM_PACKAGE_NAME, UPI_PARAM_TRANSACTION_ID)

            requiredFields.all { field ->
                jsonObject.has(field) && !jsonObject.getString(field).isNullOrEmpty()
            }
        } catch (e: Exception) {
            SDKUtils.logError(TAG_SAN, "Invalid UPI parameters: ${e.message}", e)
            // Log exception event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EventConstants.EXCEPTION_THROWN,
                orderId,
                token,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    JSON_KEY_ERROR to (e.message ?: UNKNOWN_ERROR),
                    JSON_KEY_LOCATION to LOCATION_UPI_PARAMETER_VALIDATION,
                    JSON_KEY_PARAMETERS to parameters
                ),
                sdkVersion = SDK_VERSION
            )
            false
        }
    }
} 