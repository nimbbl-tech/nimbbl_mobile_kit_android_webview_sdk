package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.util.Log
import tech.nimbbl.webviewsdk.BuildConfig

class LogUtil {

    companion object {

        fun debugLog(tag: String, message: String){
            if (BuildConfig.DEBUG){
                Log.d(tag, message);
            }
        }

    }
}