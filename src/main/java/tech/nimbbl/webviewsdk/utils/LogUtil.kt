package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.util.Log

class LogUtil {

    companion object {

        fun debugLog(tag: String, message: String){
            if (SDKUtils.isDebugLoggingEnabled()){
                Log.d(tag, message);
            }
        }

    }
}