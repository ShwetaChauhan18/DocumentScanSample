package com.scanlibrary

import android.app.AlertDialog
import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scanlibrary.Utils.getPhotoFileUri

class ScanActivityKotlin : AppCompatActivity(), ComponentCallbacks2, IScanner {

    private var scanner: IScanner? = null
    private var mRecordFilePath: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_layout)
        this.scanner = this
        mRecordFilePath = getPhotoFileUri("scan_document", this)
        initScreen()
    }

    private fun initScreen() {
        val fragment = PickImageFragmentKotlin()
        val bundle = Bundle()
        bundle.putInt(ScanConstants.OPEN_INTENT_PREFERENCE, getPreferenceContent())
        fragment.arguments = bundle
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.content, fragment)
        fragmentTransaction.commit()
    }

    protected fun getPreferenceContent(): Int {
        return intent.getIntExtra(ScanConstants.OPEN_INTENT_PREFERENCE, 0)
    }

    override fun onBitmapSelect(uri: Uri?) {
        val fragment = ScanFragmentKotlin()
        val bundle = Bundle()
        bundle.putParcelable(ScanConstants.SELECTED_BITMAP, uri)
        bundle.putString(ScanConstants.IMAGE_BASE_PATH_EXTRA, mRecordFilePath)
        fragment.arguments = bundle
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.content, fragment)
        fragmentTransaction.addToBackStack(ScanFragmentKotlin::class.java.toString())
        fragmentTransaction.commit()
    }

    override fun onScanFinish(uri: Uri?) {
        val fragment = ResultFragmentKotlin()
        val bundle = Bundle()
        bundle.putParcelable(ScanConstants.SCANNED_RESULT, uri)
        fragment.arguments = bundle
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.content, fragment)
        fragmentTransaction.addToBackStack(ResultFragmentKotlin::class.java.toString())
        fragmentTransaction.commit()
    }

    override fun onTrimMemory(level: Int) {
        when (level) {
            TRIM_MEMORY_UI_HIDDEN -> {
            }
            TRIM_MEMORY_RUNNING_MODERATE, TRIM_MEMORY_RUNNING_LOW, TRIM_MEMORY_RUNNING_CRITICAL -> {
            }
            TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_MODERATE, TRIM_MEMORY_COMPLETE ->                 /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */AlertDialog.Builder(this).setTitle(R.string.low_memory)
                .setMessage(R.string.low_memory_message).create().show()
            else -> {
            }
        }
    }

    external fun getScannedBitmap(
        bitmap: Bitmap?,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        x4: Float,
        y4: Float
    ): Bitmap?

    external fun getGrayBitmap(bitmap: Bitmap?): Bitmap?

    external fun getMagicColorBitmap(bitmap: Bitmap?): Bitmap?

    external fun getBWBitmap(bitmap: Bitmap?): Bitmap?

    external fun getPoints(bitmap: Bitmap?): FloatArray?

    companion object {
        init {
            System.loadLibrary("opencv_java3")
            System.loadLibrary("Scanner")
        }
    }
}