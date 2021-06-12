package com.scanlibrary

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.io.IOException
import java.util.Random
import kotlin.math.abs
import kotlinx.android.synthetic.main.pick_image_fragment.cameraButton
import kotlinx.android.synthetic.main.pick_image_fragment.selectButton

class PickImageFragmentKotlin : Fragment() {

    private var imageUri: Uri? = null
    private var scanner: IScanner? = null
    private lateinit var getContentPickFile: ActivityResultLauncher<Intent>
    private lateinit var getContentRequestCamera: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.pick_image_fragment, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is IScanner) {
            throw ClassCastException("Activity must implement IScanner")
        }
        this.scanner = context
    }

    private fun init() {
        getContentPickFile =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                onActivityResult(ScanConstants.PICKFILE_REQUEST_CODE, result)
            }

        getContentRequestCamera =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                onActivityResult(ScanConstants.START_CAMERA_REQUEST_CODE, result)
            }

        cameraButton.setOnClickListener {
            openCamera()
        }
        selectButton.setOnClickListener {
            openMediaContent()
        }

        if (isIntentPreferenceSet()) {
            handleIntentPreference()
        } else {
            activity?.finish()
        }
    }

    private fun clearTempImages() {
        try {
            val tempFolder = File(ScanConstants.IMAGE_PATH)
            for (f in tempFolder.listFiles()) f.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleIntentPreference() {
        val preference = getIntentPreference()
        if (preference == ScanConstants.OPEN_CAMERA) {
            openCamera()
        } else if (preference == ScanConstants.OPEN_MEDIA) {
            openMediaContent()
        }
    }

    private fun isIntentPreferenceSet(): Boolean {
        val preference = arguments?.getInt(ScanConstants.OPEN_INTENT_PREFERENCE, 0)
        return preference != 0
    }

    private fun getIntentPreference(): Int? {
        return arguments?.getInt(ScanConstants.OPEN_INTENT_PREFERENCE, 0)
    }

    fun openMediaContent() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            getContentPickFile.launch(this)
        }
    }

    private fun onActivityResult(requestCode: Int, result: ActivityResult) {
        Log.d("", "onActivityResult$result.resultCode")
        var bitmap: Bitmap? = null
        if (result.resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ScanConstants.PICKFILE_REQUEST_CODE -> bitmap = getBitmap(result.data?.data)

                ScanConstants.START_CAMERA_REQUEST_CODE -> bitmap = getBitmap(imageUri)

            }
        } else {
            activity?.finish()
        }
        bitmap?.let { postImagePick(it) }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 0)

            val file = getPhotoFileUri("launchCloud-" + abs(Random().nextInt()))
            val isDirectoryCreated = file.parentFile.mkdirs()

            Log.d("", "openCamera: isDirectoryCreated: $isDirectoryCreated")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                imageUri = FileProvider.getUriForFile(
                    requireActivity().applicationContext,
                    "com.scanlibrary.provider",  // As defined in Manifest
                    file
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                cameraIntent.putExtra("return-data", true)
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                val tempFileUri = Uri.fromFile(file)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri)
            }
            getContentRequestCamera.launch(cameraIntent)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_CAMERA_REQUEST_CODE
            )
        }
    }

    // Returns the File for a photo stored on disk given the fileName
    private fun getPhotoFileUri(fileName: String): File {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        val mediaStorageDir =
            File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "APP_TAG")

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d("APP_TAG", "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File(mediaStorageDir.path + File.separator + fileName)
    }

    private fun postImagePick(bitmap: Bitmap) {
        val uri = Utils.getUri(requireContext(), bitmap)
        bitmap.recycle()
        scanner?.onBitmapSelect(uri)
    }

    @Throws(IOException::class)
    private fun getBitmap(selectedImg: Uri?): Bitmap? {
        val options = BitmapFactory.Options()
        options.inSampleSize = 3
        val fileDescriptor: AssetFileDescriptor? =
            selectedImg?.let { activity?.contentResolver?.openAssetFileDescriptor(it, "r") }
        return BitmapFactory.decodeFileDescriptor(fileDescriptor?.fileDescriptor, null, options)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(activity, "camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val MY_CAMERA_REQUEST_CODE = 100
    }
}