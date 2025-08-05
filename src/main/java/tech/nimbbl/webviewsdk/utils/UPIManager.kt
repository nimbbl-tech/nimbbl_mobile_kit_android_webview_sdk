package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.data.models.payment.UpiAppsVo
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.R
import tech.nimbbl.webviewsdk.constants.PaymentConstants.is_debug_enabled
import tech.nimbbl.webviewsdk.constants.PaymentConstants.SDK_VERSION
import android.app.Activity
import kotlin.collections.listOf
import kotlin.collections.mapOf
import kotlin.text.isNullOrEmpty
import kotlin.collections.mutableListOf

/**
 * Manager class for handling UPI (Unified Payments Interface) operations
 */
class UPIManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UPIManager"
        private const val UPI_SCHEME = "upi"
        private const val UPI_AUTHORITY = "pay"
    }
    
    /**
     * Create UPI intent with validation
     * @param url UPI URL
     * @param packageName Package name of the UPI app
     * @param transactionId Transaction ID
     * @return Intent if valid, null otherwise
     */
    fun createUPIIntent(url: String, packageName: String, transactionId: String): Intent? {
        return try {
            // Validate URL
            if (url.isNullOrEmpty()) {
                Log.e(TAG, "UPI URL is null or empty")
                return null
            }
            
            // Validate package name
            if (packageName.isNullOrEmpty()) {
                Log.e(TAG, "Package name is null or empty")
                return null
            }
            
            // Create intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage(packageName)
            }
            
            // Check if intent can be resolved
            if (intent.resolveActivity(context.packageManager) != null) {
                intent
            } else {
                Log.w(TAG, "No app found to handle UPI intent for package: $packageName")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating UPI intent: ${e.message}", e)
            null
        }
    }
    
    /**
     * Launch UPI intent with proper error handling
     * @param intent UPI intent to launch
     * @param activity Activity to launch from
     * @return true if launched successfully, false otherwise
     */
    fun launchUPIIntent(intent: Intent, activity: Activity): Boolean {
        return try {
            // Validate intent
            if (intent.data == null) {
                Log.e(TAG, "UPI intent has no data URI")
                return false
            }
            
            // Launch intent
            activity.startActivity(intent)
            Log.d(TAG, "UPI intent launched successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching UPI intent: ${e.message}", e)
            false
        }
    }
    
    /**
     * Check if UPI app is installed
     * @param packageName Package name to check
     * @return true if app is installed, false otherwise
     */
    private fun isUPIAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            Log.d(TAG, "UPI app not installed: $packageName")
            false
        }
    }
    
    /**
     * Get list of installed UPI apps
     * @return List of package names for installed UPI apps
     */
    fun getInstalledUPIApps(): List<String> {
        val upiApps = mutableListOf<String>()
        val commonUPIApps = listOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.phonepe.app", // PhonePe
            "net.one97.paytm", // Paytm
            "in.amazonpay", // Amazon Pay
            "com.mobikwik_new", // Mobikwik
            "com.freecharge", // FreeCharge
            "com.olacabs.customer", // Ola Money
            "com.airtel.airtelmoney", // Airtel Money
            "com.jio.myjio", // Jio Money
            "com.icici.bank.imobile", // iMobile
            "com.hdfcbank.hdfcpay", // HDFC Pay
            "com.sbi.SBIFreedomPlus", // SBI Yono
            "com.axis.bank.axismobile", // Axis Mobile
            "com.kotak.mobilebanking", // Kotak Mobile Banking
            "com.unionbankofindia.mobilebanking", // Union Bank Mobile Banking
            "com.icici.bank.imobile", // ICICI iMobile
            "com.hdfcbank.hdfcpay", // HDFC Pay
            "com.sbi.SBIFreedomPlus", // SBI Yono
            "com.axis.bank.axismobile", // Axis Mobile
            "com.kotak.mobilebanking", // Kotak Mobile Banking
            "com.unionbankofindia.mobilebanking" // Union Bank Mobile Banking
        )
        
        for (packageName in commonUPIApps) {
            if (isUPIAppInstalled(packageName)) {
                upiApps.add(packageName)
            }
        }
        
        return upiApps
    }
    
    /**
     * Create JSON object with UPI apps information for WebView
     * @return JSON object containing UPI apps data
     */
    fun createUPIAppsJSON(orderID: String, orderToken: String, subMerchantId: String): JSONObject {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            action = Intent.ACTION_VIEW
            data = Uri.Builder().scheme(UPI_SCHEME).authority(UPI_AUTHORITY).build()
        }
        
        val pkgAppsList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)
        val jsonArray = JSONArray()
        
        // If no UPI apps found, add dummy Google Pay and PhonePe
        if (pkgAppsList.isEmpty()) {
            if (is_debug_enabled) {
                Log.d(TAG, "No UPI apps found in createUPIAppsJSON, adding dummy Google Pay and PhonePe")
            }

        /*    // Add dummy Google Pay
            val googlePayObj = JSONObject().apply {
                put("upi_app_name", "Google Pay")
                put("package_name", "com.google.android.apps.nbu.paisa.user")
                    put("appiconbase64", "")

            }
            jsonArray.put(googlePayObj)

            // Add dummy PhonePe
            val phonePeObj = JSONObject().apply {
                put("upi_app_name", "PhonePe")
                put("package_name", "com.phonepe.app")

                // Use default UPI icon for dummy apps
                    put("appiconbase64","")

            }
            jsonArray.put(phonePeObj)*/
        } else {
            // Process actual UPI apps
            for (resolveInfo in pkgAppsList) {
                val appObj = JSONObject().apply {
                    put("upi_app_name", resolveInfo.loadLabel(packageManager).toString())
                    put("package_name", resolveInfo.activityInfo.packageName)
                    
                    // Get app icon
                    val icon: Drawable = try {
                        packageManager.getApplicationIcon(resolveInfo.activityInfo.packageName)
                    } catch (e: Exception) {
                        if (is_debug_enabled) {
                            Log.w(TAG, "Error loading app icon for ${resolveInfo.activityInfo.packageName}: ${e.message}")
                        }
                        // Log exception event
                        EventLoggingUtils.safeLogEvent(
                            context,
                            EventConstants.EXCEPTION_THROWN,
                            orderID,
                            orderToken,
                            subMerchantId,
                            mapOf<String, Any>(
                                "error" to (e.message ?: "Unknown error"),
                                "location" to "UPI app icon loading",
                                "package_name" to resolveInfo.activityInfo.packageName
                            ),
                            SDK_VERSION
                        )
                        // Fallback to default UPI icon
                        context.resources.getDrawable(R.drawable.upi, null)
                    }
                }
                jsonArray.put(appObj)
            }
        }
        
        return JSONObject().put("UPIApps", jsonArray)
    }
    
    /**
     * Validate UPI parameters received from JavaScript
     * @param parameters JSON string containing UPI parameters
     * @return true if parameters are valid
     */
    fun validateUPIParameters(parameters: String,orderId:String,token:String,subMerchantId: String): Boolean {
        return try {
            val jsonObject = JSONObject(parameters)
            val requiredFields = listOf("url", "package_name", "transaction_id")
            
            requiredFields.all { field ->
                jsonObject.has(field) && !jsonObject.getString(field).isNullOrEmpty()
            }
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e(TAG, "Invalid UPI parameters: ${e.message}", e)
            }
            // Log exception event
            EventLoggingUtils.safeLogEvent(
                context,
                EventConstants.EXCEPTION_THROWN,
                orderId,
                token,
                subMerchantId,
                mapOf<String, Any>(
                    "error" to (e.message ?: "Unknown error"),
                    "location" to "UPI parameter validation",
                    "parameters" to parameters
                ),
                SDK_VERSION
            )
            false
        }
    }
} 