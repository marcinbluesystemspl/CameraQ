package com.leveron.cameraq

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

object Constants {
const val TAG = "camerax"
const val FILE_NAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
const val REQUEST_CODE_PERMISSIONS = 123
    const val APP_NAME = "CameraQ"
@RequiresApi(Build.VERSION_CODES.S)
val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                                    Manifest.permission.MANAGE_MEDIA)

}