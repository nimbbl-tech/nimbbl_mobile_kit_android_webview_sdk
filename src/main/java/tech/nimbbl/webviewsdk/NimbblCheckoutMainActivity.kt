package tech.nimbbl.webviewsdk

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import tech.nimbbl.coreapisdk.NimbblCoreApiSDK
import tech.nimbbl.coreapisdk.models.UpiAppsVo
import tech.nimbbl.coreapisdk.utils.PayloadKeys
import tech.nimbbl.webviewsdk.util.LogUtil.Companion.debugLog
import tech.nimbbl.webviewsdk.util.RestApiUtils
import tech.nimbbl.webviewsdk.util.encodeToBase64
import tech.nimbbl.webviewsdk.util.isNetConnected
import tech.nimbbl.webviewsdk.util.parseJwtToken
import java.net.URLDecoder


class NimbblCheckoutMainActivity : AppCompatActivity() {

    companion object {
        private const val RESPONSE_QUERY_PARAM = "response"
        private const val ERROR_SWW_DOT_DOT = "Something went wrong ..."
        private const val TAG = "NimbblCheckoutMainActivity"
        private const val EXTRA_TAG_OPTIONS = "options"
    }

    private val optionsList = arrayOf(
        "Unable to find my payment method",
        "Unable to complete the payment",
        "Don't want to make a purchase right now",
        "Don't understand how to proceed",
        "Others"
    )
    private lateinit var webView: WebView
    private lateinit var options: NimbblCheckoutOptions
    private var cancelOption = "Others"
    private lateinit var transactionID: String
    private var btnBackGroundColor = "#000000"
    private var btnTextColor = "#ffffff"
    private var orderID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nimbbl_checkout_main)
        webView = findViewById(R.id.webview)
        if (intent.hasExtra(EXTRA_TAG_OPTIONS)) {
            options = intent.getParcelableExtra(EXTRA_TAG_OPTIONS)!!
            if (!isNetConnected(this)) {
                onFailedPayment(getString(R.string.no_internet))
            } else {
                orderID = parseJwtToken(options.orderToken!!)
                Log.d("orderID-->",orderID)
                updateOrder(options.orderToken!!, orderID)
            }
        } else {
            onFailedPayment(getString(R.string.input_sent_invalid))
        }
    }

    private fun updateOrder(token: String, orderId: String) {
        try {
            CoroutineScope(Dispatchers.Main).launch {
                val response = NimbblCoreApiSDK.getInstance()
                    ?.updateOrder(token, orderId, "callback_mobile", "android")
                if (response != null && response.isSuccessful) {
                    checkOutDetails()
                } else {
                    if (response != null) {
                        if (response.errorBody() != null) {
                            try {
                                val jObjError = JSONObject(response.errorBody()!!.string())
                                onFailedPayment(
                                    jObjError.getJSONObject("error")
                                        .getString("nimbbl_merchant_message")
                                )

                            } catch (e: Exception) {
                                onFailedPayment(getString(R.string.error_in_update_order))
                            }

                        }
                    }
                }
            }
        } catch (e: Exception) {
            onFailedPayment(getString(R.string.error_in_update_order))
        }
        NimbblCheckoutSDK.instance?.setMyActivityWeakReference(this)

    }

    private fun checkOutDetails() {
        try {
            CoroutineScope(Dispatchers.Main).launch {
                val response = NimbblCoreApiSDK.getInstance()
                    ?.getCheckoutDetails(options.orderToken!!)
                if (response != null && response.isSuccessful) {
                    btnBackGroundColor = response.body()!!.checkout_background_color
                    btnTextColor =
                        if (response.body()!!.checkout_text_color == "white") "#ffffff" else "#000000"
                    setupWebView()
                } else {
                    if (response != null) {
                        if (response.errorBody() != null) {
                            try {
                                val jObjError = JSONObject(response.errorBody()!!.string())
                                onFailedPayment(
                                    jObjError.getJSONObject("error")
                                        .getString("nimbbl_merchant_message")
                                )

                            } catch (e: Exception) {
                                onFailedPayment(getString(R.string.error_in_update_order))
                            }

                        }
                    }
                }
            }
        } catch (e: Exception) {
            onFailedPayment(getString(R.string.error_in_update_order))
        }
        NimbblCheckoutSDK.instance?.setMyActivityWeakReference(this)

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        //val url = RestApiUtils.WEB_VIEW_VIEW_URL
        var url = String.format(RestApiUtils.WEB_VIEW_VIEW_URL, orderID, options.orderToken)
        if (options.paymentModeCode?.isNotEmpty() == true) {
            url = if (url.indexOf("?") > 0) {
                "$url&payment_mode=${options.paymentModeCode}"
            } else {
                "$url?payment_mode=${options.paymentModeCode}"
            }
        }

        if (options.bankCode?.isNotEmpty() == true) {
            url = if (url.indexOf("?") > 0) {
                "$url&bank_code=${options.bankCode}"
            } else {
                "$url?bank_code=${options.bankCode}"
            }
        }
        if (options.walletCode?.isNotEmpty() == true) {
            url = if (url.indexOf("?") > 0) {
                "$url&wallet_code=${options.walletCode}"
            } else {
                "$url?wallet_code=${options.walletCode}"
            }
        }
        if (options.paymentFlow?.isNotEmpty() == true) {
            url = if (url.indexOf("?") > 0) {
                "$url&payment_flow=${options.paymentFlow}"
            } else {
                "$url?payment_flow=${options.paymentFlow}"
            }
        }
        webView.loadUrl(url)
        webView.apply {
            settings.apply {
                domStorageEnabled = true
                javaScriptEnabled = true
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
            }
        }
        webView.addJavascriptInterface(INimmbleWebViewInterface(this), "NimbblSDK")

        webView.webViewClient = webViewClient

        webView.webChromeClient = object : WebChromeClient() {

            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean,
                isUserGesture: Boolean, resultMsg: Message?
            ): Boolean {
                val newWebView = WebView(view!!.context)
                val transport = resultMsg!!.obj as WebViewTransport
                val webSettings = newWebView.settings
                webSettings.javaScriptEnabled = true
                webSettings.domStorageEnabled = true
                val dialog = Dialog(view.context).apply {
                    setContentView(newWebView)
                    show()
                    // remember that dialog.show should be called before doing window.setlayout.
                    window?.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }

                newWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView) {
                        dialog.dismiss()
                    }

                }
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    )
                            : Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        debugLog(TAG, "onPageFinished2 " + (url ?: " emptyUrl"))
                        super.onPageFinished(view, url)
                    }
                }
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }
    }

    inner class INimmbleWebViewInterface
    /**
     * Instantiate the interface and set the context
     */
    internal constructor(private val mContext: Context) {


        @JavascriptInterface
        fun openUpiIntent(parameters: String) {
            // Log.d("SAN","openUpiIntent")
            Log.d("SAN", "openUpiIntent-->$parameters")
            runOnUiThread {
                try {
                    val responseObj = JSONObject(parameters)
                    val url = responseObj.getString("url")
                    //url ="upi://pay?pa=shopnimbbltech.payu@indus&pn=Bigital Technologies Pvt ltd&tr=15236808982&tid=26052202&am=1.00&cu=INR&tn=UPI Transaction for 26052201"
                    // var tidIndex = url.indexOf("tid=")
                    //println("tidIndex-->$tidIndex")
                    // var result = url.chunked(tidIndex)
                    // println("result-->$result")

                    //var secondString  = result[1].substringAfter("&")
                    // println("secondString-->$secondString")
                    // var finalResult = result[0] + secondString
                    //println("finalresult-->$finalResult")
                    val packageName = responseObj.getString("package_name")
                    transactionID = responseObj.getString("transaction_id")
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    intent.setPackage(packageName)
                    resultLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@NimbblCheckoutMainActivity,
                        "Unable to launch UPI intent app.",
                        Toast.LENGTH_SHORT
                    ).show()
                    insertJSToWebview()
                }

            }

        }


    }

    private fun insertJSToWebview() {
        CoroutineScope(Dispatchers.Main).launch {
            val paymentEnquiryResponse = NimbblCoreApiSDK.getInstance()
                ?.getTransactionEnquiry(
                    options.orderToken!!,
                    orderID,
                    orderID,
                    transactionID
                )
            /*  val paymentEnquiryResponse = NimbblCoreApiSDK.getInstance()
                  ?.getTransactionEnquiry(
                      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ1cm46bmltYmJsIiwiaWF0IjoxNzE2OTg1NTE1LCJleHAiOjE3MTY5ODY3MTUsInR5cGUiOiJvcmRlciIsInN1Yl9tZXJjaGFudF9pZCI6Miwib3JkZXJfaWQiOiJvX2xQTThWTno1WURRT1dQRHoifQ.yqWIUFB1u_lzRyEcwR8x213IbdHC0zmOZBg26xDohAs",
                      "o_lPM8VNz5YDQOWPDz",
                      "dev0.6169222828798078",
                      transactionID
                  )*/
            val jsonObject = JSONObject()
            jsonObject.put(PayloadKeys.key_upi_intent_back_response, "close")

            if (paymentEnquiryResponse != null && paymentEnquiryResponse.isSuccessful) {
                jsonObject.put(
                    PayloadKeys.key_transaction_enquiry_api_response,
                    paymentEnquiryResponse.body()!!.order.status
                )
                webView.evaluateJavascript("window.postMessage('$jsonObject', '*');", null)
            } else {
                jsonObject.put(PayloadKeys.key_transaction_enquiry_api_response, "")
                webView.evaluateJavascript("window.postMessage('$jsonObject', '*');", null)

            }
        }


    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("SAN", "resultCode-->" + result.resultCode + "data-->" + result.data)
            insertJSToWebview()
        }

    private fun sendUpiIntents() {
        val packageManager = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_DEFAULT)
        mainIntent.addCategory(Intent.CATEGORY_BROWSABLE)
        mainIntent.action = Intent.ACTION_VIEW
        val uri1: Uri = Uri.Builder().scheme("upi").authority("pay").build()
        mainIntent.data = uri1
        val pkgAppsList: List<*> = packageManager.queryIntentActivities(mainIntent, 0)
        Log.d("SAN", "${pkgAppsList.size}")
        val appListCol = mutableListOf<UpiAppsVo>()
        val jsonArray = JSONArray()
        for (i in pkgAppsList.indices) {
            val resolveInfo = pkgAppsList[i] as ResolveInfo
            val apssObj = JSONObject()
            apssObj.put("upi_app_name", resolveInfo.loadLabel(packageManager).toString())
            apssObj.put("package_name", resolveInfo.activityInfo.packageName)
            val icon: Drawable =
                getPackageManager().getApplicationIcon(resolveInfo.activityInfo.packageName)
            val bitmap = getBitmapFromDrawable(icon)
            if (bitmap != null) {
                // apssObj.put("appiconbase64", "")
                apssObj.put("appiconbase64", encodeToBase64(bitmap, Bitmap.CompressFormat.PNG, 100))
            } else {
                val icon = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.upi
                )
                // apssObj.put("appiconbase64","")
                apssObj.put("appiconbase64", encodeToBase64(icon, Bitmap.CompressFormat.PNG, 100))
            }
            jsonArray.put(apssObj)
            appListCol.add(
                UpiAppsVo(
                    resolveInfo.activityInfo.packageName,
                    "",
                    resolveInfo.loadLabel(packageManager).toString()
                )
            )
        }
        val mainobj = JSONObject().put("UPIApps", jsonArray)

        /*       var json = "{\n" +
                       "  \"UPIApps\": [\n" +
                       "    {\n" +
                       "      \"upi_app_name\": \"gpay\",\n" +
                       "      \"package_name\": \"com.google.android.apps.nbu.paisa.user\"\n" +
                       "    },\n" +
                       "    {\n" +
                       "      \"upi_app_name\": \"phonepay\",\n" +
                       "      \"package_name\": \"com.phonepe.app\"\n" +
                       "    }\n" +
                       "  ]\n" +
                       "}"*/

        Log.d("SAN", "mainobj--->$mainobj")
        // Log.d("SAN", "json--->$json")
        webView.evaluateJavascript(
            "window.nimbbl_web.UPIIntentAvailable(JSON.stringify(${mainobj}))",
            null
        )

        /*    webView.evaluateJavascript(
                "javascript: window.nimbbl_web.UPIIntentAvailable(JSON.stringify(${mainobj}))"
            ) { s ->
                Log.d("SAN-evaluateJavascript", s!!) // Prints: "this"
            }*/

        // webView.evaluateJavascript("window.nimbbl_web = '${json}';", null)
        // webView.evaluateJavascript("window.UPIIntentAvailable = 'JSON.stringify(${mainobj})';", null)

        // Inject JavaScript to log the window object
        //  webView.evaluateJavascript("console.log('window.nimbbl_web:', window.nimbbl_web);", null);

        /*   webView.evaluateJavascript(
               "javascript: nimbbl.namespace.UPIIntentAvailable()",
               null
           )*/
    }

    private var webViewClient = object : WebViewClient() {

        private fun handleUrlIfWeCan(url: String?) {
            if (url != null && url.startsWith(RestApiUtils.WEB_VIEW_RESP_CHECK_URL)) {
                Log.d("SAN", "url---->$url")
                var errorMsg = ERROR_SWW_DOT_DOT
                try {
                    val queryParameter = Uri.parse(url).getQueryParameter(RESPONSE_QUERY_PARAM)
                    if (!TextUtils.isEmpty(queryParameter)) {
                        var isSuccess = false
                        var payloadJObject = JSONObject()

                        val text = URLDecoder.decode(queryParameter)
                        Log.d("SAN", "text-->$text")
                        try {
                            val jObject = JSONObject(text)
                            if (jObject.has("payload")) {
                                payloadJObject = jObject.getJSONObject("payload")
                                if (payloadJObject.has("status")) {
                                    val status = payloadJObject.getString("status")
                                    if (status.lowercase().equals("success", true)) {
                                        isSuccess = true
                                    }
                                }
                            }
                            if (isSuccess) {
                                onSuccessPayment(payloadJObject)
                            } else {
                                if (payloadJObject.has("reason")) {
                                    errorMsg = payloadJObject.getString("reason")
                                }
                                onFailedPayment(errorMsg)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            onFailedPayment(errorMsg)
                        }
                    }
                } catch (e: Exception) {
                    onFailedPayment(errorMsg)
                }
            }
        }

        //handle redirect.
        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            debugLog(TAG, "handleThisUrl dUVH: " + (url ?: " emptyUrl") + "-> isReload$isReload")
            handleUrlIfWeCan(url)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            debugLog(TAG, "onPageFinished1 " + (url ?: " emptyUrl"))
            super.onPageFinished(view, url)
            sendUpiIntents()

        }
    }

    private fun onSuccessPayment(payloadJObject: JSONObject) {
        try {
            sendBroadcast(Intent("PaymentSuccess").apply {
                putExtra("data", payloadJObject.toString())
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun onFailedPayment(errorStr: String) {
        try {
            sendBroadcast(Intent("PaymentFailure").apply {
                putExtra("data", errorStr)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            @SuppressLint("MissingInflatedId")
            override fun handleOnBackPressed() {
                // Whatever you want
                // when back pressed
                val bottomSheetDialog = BottomSheetDialog(this@NimbblCheckoutMainActivity)


                // Inflate the custom layout for the Bottom Sheet
                val bottomSheetView: View =
                    layoutInflater.inflate(R.layout.bottom_sheet_cancel_dialog, null)
                bottomSheetDialog.setContentView(bottomSheetView)


                // Handle the buttons inside the Bottom Sheet


                // Set the button click listeners
                bottomSheetDialog.findViewById<AppCompatImageView>(R.id.iv_close)
                    ?.setOnClickListener { v: View? ->
                        // Handle cancel logic
                        bottomSheetDialog.dismiss()
                    }
                val button = bottomSheetDialog.findViewById<AppCompatButton>(R.id.btn_cancel)
                button?.setTextColor(Color.parseColor(btnBackGroundColor))
                val border = GradientDrawable()
                border.shape = GradientDrawable.RECTANGLE
                border.setStroke(
                    4,
                    Color.parseColor(btnBackGroundColor)
                )  // Set border color and width
                border.setColor(Color.parseColor(btnTextColor))
                // Set button background color

                if (button != null) {
                    button.background = border
                }
                // Set the button click listeners
                button?.setOnClickListener {
                    // Handle cancel logic
                    bottomSheetDialog.dismiss()
                    openCancelOptions()
                }

                val buttonGoBack = bottomSheetDialog.findViewById<AppCompatButton>(R.id.btn_go_back)
                buttonGoBack?.setTextColor(Color.parseColor(btnTextColor))
                buttonGoBack?.setBackgroundColor(Color.parseColor(btnBackGroundColor))
                buttonGoBack
                    ?.setOnClickListener { v: View? ->
                        // Handle go back logic
                        bottomSheetDialog.dismiss()
                    }


                // Show the Bottom Sheet Dialog
                bottomSheetDialog.show()

            }
        })
        return super.getOnBackInvokedDispatcher()
    }


    private fun openCancelOptions() {
        val bottomSheetDialog = BottomSheetDialog(this@NimbblCheckoutMainActivity)


        // Inflate the custom layout for the Bottom Sheet
        val bottomSheetView: View =
            layoutInflater.inflate(R.layout.custom_alert_dialog, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val rv: RecyclerView? = bottomSheetDialog.findViewById(R.id.rv)
        val adapter = PaymentCancelOptionsAdapter(this, optionsList)
        if (rv != null) {
            rv.adapter = adapter
        }
        if (rv != null) {
            rv.layoutManager = LinearLayoutManager(this)
        }
        // Set the button click listeners
        bottomSheetDialog.findViewById<AppCompatImageView>(R.id.iv_close)
            ?.setOnClickListener { v: View? ->
                // Handle cancel logic
                bottomSheetDialog.dismiss()
            }
        val okBtn: AppCompatButton? = bottomSheetDialog.findViewById(R.id.btn_ok)
        okBtn?.setBackgroundColor(Color.parseColor(btnBackGroundColor))
        okBtn?.setTextColor(Color.parseColor(btnTextColor))
        okBtn?.setOnClickListener {
            updateCancelOption(adapter.getSelectedOption())
            bottomSheetDialog.dismiss()
            finish()

        }
        // Show the Bottom Sheet Dialog
        bottomSheetDialog.show()
    }

    private fun updateCancelOption(cancelReason: String?) {

        CoroutineScope(Dispatchers.Main).launch {
            NimbblCoreApiSDK.getInstance()
                ?.updateCheckOutCancelReason(options.orderToken!!, orderID, cancelReason!!)
        }


    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

}
