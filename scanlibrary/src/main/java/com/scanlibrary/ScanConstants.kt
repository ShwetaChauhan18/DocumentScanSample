package com.scanlibrary

import android.os.Environment

object ScanConstants {
    const val PICKFILE_REQUEST_CODE = 1
    const val START_CAMERA_REQUEST_CODE = 2
    const val OPEN_INTENT_PREFERENCE = "selectContent"
    const val IMAGE_BASE_PATH_EXTRA = "ImageBasePath"
    const val OPEN_CAMERA = 4
    const val OPEN_MEDIA = 5
    const val SCANNED_RESULT = "scannedResult"
    val IMAGE_PATH = Environment.getExternalStorageDirectory().path + "/scanSample"
    const val SELECTED_BITMAP = "selectedBitmap"
}
