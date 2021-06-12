package com.scanner.demo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.scanlibrary.ScanActivity
import com.scanlibrary.ScanConstants
import java.io.IOException
import kotlinx.android.synthetic.main.activity_main.cameraButton
import kotlinx.android.synthetic.main.activity_main.mediaButton
import kotlinx.android.synthetic.main.activity_main.scannedImage

class MainActivity : AppCompatActivity() {

    private lateinit var getContentFile: ActivityResultLauncher<Intent>

    companion object {
        private const val REQUEST_CODE = 99
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {

        cameraButton.setOnClickListener {
            startScan(ScanConstants.OPEN_CAMERA)
        }

        mediaButton.setOnClickListener {
            startScan(ScanConstants.OPEN_MEDIA)
        }

        getContentFile =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                onActivityResult(REQUEST_CODE, result)
            }
    }

    private fun startScan(preference: Int) {
        val intent = Intent(this, ScanActivity::class.java)
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference)
        getContentFile.launch(intent)
    }

    private fun onActivityResult(requestCode: Int, result: ActivityResult) {
        if (requestCode == REQUEST_CODE && result.resultCode == RESULT_OK) {
            val uri = result.data?.extras?.getParcelable<Uri>(ScanConstants.SCANNED_RESULT)
            var bitmap: Bitmap? = null
            try {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                contentResolver.delete(uri!!, null, null)
                scannedImage.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun convertByteArrayToBitmap(data: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }
}