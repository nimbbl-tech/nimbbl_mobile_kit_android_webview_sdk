package tech.nimbbl.webviewsdk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import tech.nimbbl.coreapisdk.NimbblCoreApiSDK
import tech.nimbbl.coreapisdk.interfaces.NimbblCheckoutPaymentListener
import tech.nimbbl.webviewsdk.util.RestApiUtils.NIMBBL_TECH_URL
import tech.nimbbl.webviewsdk.util.RestApiUtils.WEB_VIEW_RESP_CHECK_URL
import tech.nimbbl.webviewsdk.util.RestApiUtils.WEB_VIEW_VIEW_URL
import tech.nimbbl.webviewsdk.util.getDeviceFingerPrint
import tech.nimbbl.webviewsdk.util.getDeviceID
import tech.nimbbl.webviewsdk.util.md5
import java.lang.ref.WeakReference


class NimbblCheckoutSDK private constructor() {
    private var myActivityReference : WeakReference<Activity?>? = null
    private var receiver: PaymentEventBroadCastReceiver? = null
    private var listener: NimbblCheckoutPaymentListener? = null
    private var mContext: Context? = null
    fun checkout(options: NimbblCheckoutOptions?) {

        //this.options = options;
        try {
            val intent = Intent(mContext, NimbblCheckoutMainActivity::class.java)
            intent.putExtra("options", options)
            mContext!!.startActivity(intent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext!!.registerReceiver(
                    receiver, IntentFilter("PaymentSuccess"),
                    Context.RECEIVER_EXPORTED
                )
                mContext!!.registerReceiver(
                    receiver, IntentFilter("PaymentFailure"),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                mContext!!.registerReceiver(receiver, IntentFilter("PaymentSuccess"))
                mContext!!.registerReceiver(receiver, IntentFilter("PaymentFailure"))
            }
        }catch (e: Exception){
            e.printStackTrace()
        }


    }

    fun dismissReactActivity() {
        if (myActivityReference!!.get() != null) {
            val handler = Handler(Looper.getMainLooper())
            handler.post { (myActivityReference!!.get() as NimbblCheckoutMainActivity?)!!.finish() }
        }
    }

    fun setMyActivityWeakReference(myActivityReference: Activity?) {
        this.myActivityReference = WeakReference(myActivityReference)
    }

    fun setEnvironmentUrl(apiUrl: String) {
        NIMBBL_TECH_URL = apiUrl
        var webViewUrl = ""
        var webViewRespUrl = ""
        when (NIMBBL_TECH_URL) {
            "https://qa1api.nimbbl.tech/" -> {
                webViewUrl = "https://qa1sonic.nimbbl.tech/?order_id=%1\$s&token=%2\$s"
                webViewRespUrl = "https://qa1sonic.nimbbl.tech/mobile/redirect"
            }
            "https://qa2api.nimbbl.tech/" -> {
                webViewUrl = "https://qa2sonic.nimbbl.tech/?order_id=%1\$s&token=%2\$s"
                webViewRespUrl = "https://qa2sonic.nimbbl.tech/mobile/redirect"
            }

            "https://apipp.nimbbl.tech/" -> {
              //  webViewUrl = "https://sonicpp.nimbbl.tech/?order_id=%1\$s&token=%2\$s"
                webViewUrl = "https://sonicpp.nimbbl.tech/?order_id=%1\$s&token=%2\$s"
                //webViewRespUrl = "https://sonicpp.nimbbl.tech/mobile/redirect"
                webViewRespUrl = "https://sonicpp.nimbbl.tech/mobile/redirect"
            }

            "https://api.nimbbl.tech/" -> {
                webViewUrl = "https://sonic.nimbbl.tech/?order_id=%1\$s&token=%2\$s"
                webViewRespUrl = "https://sonic.nimbbl.tech/mobile/redirect"
            }
        }
        WEB_VIEW_VIEW_URL = webViewUrl
        WEB_VIEW_RESP_CHECK_URL = webViewRespUrl


    }

    fun init(activity: Activity?) {
        if (activity == null) {
            Log.e("Nimbbl SDK", "activity can't be null. returning.")
            return
        }

        listener = activity as NimbblCheckoutPaymentListener?
        mContext = activity
        receiver = PaymentEventBroadCastReceiver()
        val fingerPrint = md5(getDeviceFingerPrint() + getDeviceID(mContext!!))
        NimbblCoreApiSDK.getInstance()?.initialiseAPISDK(NIMBBL_TECH_URL,fingerPrint, getDeviceFingerPrint())
    }



    private inner class PaymentEventBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (myActivityReference!!.get() != null) {
                dismissReactActivity()
            }
            val action = intent.action
            val data = intent.getStringExtra("data")
            if (action == "PaymentSuccess") {
                try {
                    listener?.onPaymentSuccess(jsonString2Map(data!!))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (action == "PaymentFailure") {
                if (data != null) {
                    listener?.onPaymentFailed(data)
                }
            }
            context.unregisterReceiver(receiver)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: NimbblCheckoutSDK? = null
            get() {
                if (field == null) {
                    field = NimbblCheckoutSDK()
                }
                return field
            }
            private set

        @Throws(JSONException::class)
        fun jsonString2Map(jsonString: String): MutableMap<String, Any> {
            val keys: MutableMap<String, Any> = HashMap()
            val jsonObject = JSONObject(jsonString) // HashMap
            val keyset: Iterator<*> = jsonObject.keys() // HM
            while (keyset.hasNext()) {
                val key = keyset.next() as String
                val value = jsonObject[key]
                //System.out.print("\n Key : "+key);
                if (value is JSONObject) {
                    //System.out.println("Incomin value is of JSONObject : ");
                    keys[key] = jsonString2Map(value.toString())
                } else {
                    keys[key] = value
                }
            }
            return keys
        }
    }


}