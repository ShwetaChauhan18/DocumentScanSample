package com.scanlibrary

import android.net.Uri

interface IScanner {
    fun onBitmapSelect(uri: Uri?)
    fun onScanFinish(uri: Uri?)
}
