package tech.nimbbl.webviewsdk.util

import android.util.Log
import tech.nimbbl.webviewsdk.BuildConfig

/**
 * Created by Sandeep Y. on 20-05-2024 at 16:14
 */
class LogUtil {

    companion object {

        fun debugLog(tag: String, message: String){
            if (BuildConfig.DEBUG){
                Log.d(tag, message);
            }
        }

    }
}