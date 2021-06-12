package com.scanlibrary

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.IOException
import kotlinx.android.synthetic.main.result_layout.BWMode
import kotlinx.android.synthetic.main.result_layout.doneButton
import kotlinx.android.synthetic.main.result_layout.grayMode
import kotlinx.android.synthetic.main.result_layout.magicColor
import kotlinx.android.synthetic.main.result_layout.originalButton
import kotlinx.android.synthetic.main.result_layout.scannedImage

class ResultFragmentKotlin : Fragment() {

    private var original: Bitmap? = null
    private var transformed: Bitmap? = null
    private var progressDialogFragment: ProgressDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.result_layout, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        originalButton.setOnClickListener {
            try {
                showProgressDialog(resources.getString(R.string.applying_filter))

                originalButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.full_dim
                    )
                )
                magicColor.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), android.R.color.transparent
                    )
                )
                grayMode.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), android.R.color.transparent
                    )
                )
                BWMode.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.transparent
                    )
                )

                transformed = original
                scannedImage.setImageBitmap(original)
                dismissDialog()
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                dismissDialog()
            }
        }

        magicColor.setOnClickListener { v ->
            showProgressDialog(resources.getString(R.string.applying_filter))

            originalButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), android.R.color.transparent
                )
            )
            magicColor.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.full_dim
                )
            )
            grayMode.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), android.R.color.transparent
                )
            )
            BWMode.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.transparent
                )
            )

            AsyncTask.execute {
                try {
                    transformed = (activity as ScanActivity).getMagicColorBitmap(original)
                } catch (e: OutOfMemoryError) {
                    activity?.runOnUiThread {
                        transformed = original
                        scannedImage.setImageBitmap(original)
                        e.printStackTrace()
                        dismissDialog()
                        v.performClick()
                        //onClick(v)
                    }
                }
                activity?.runOnUiThread {
                    scannedImage.setImageBitmap(transformed)
                    dismissDialog()
                }
            }
        }

        grayMode.setOnClickListener { v ->
            showProgressDialog(resources.getString(R.string.applying_filter))

            originalButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), android.R.color.transparent
                )
            )
            magicColor.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), android.R.color.transparent
                )
            )
            grayMode.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.full_dim
                )
            )
            BWMode.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.transparent
                )
            )

            AsyncTask.execute {
                try {
                    transformed = (activity as ScanActivity).getGrayBitmap(original)
                } catch (e: OutOfMemoryError) {
                    activity?.runOnUiThread {
                        transformed = original
                        scannedImage.setImageBitmap(original)
                        e.printStackTrace()
                        dismissDialog()
                        v.performClick()
                        //onClick(v)
                    }
                }
                activity?.runOnUiThread {
                    scannedImage.setImageBitmap(transformed)
                    dismissDialog()
                }
            }
        }

        BWMode.setOnClickListener {
            showProgressDialog(resources.getString(R.string.applying_filter))

            originalButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), android.R.color.transparent
                )
            )
            magicColor.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), android.R.color.transparent
                )
            )
            grayMode.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), android.R.color.transparent
                )
            )
            BWMode.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.full_dim))

            AsyncTask.execute {
                try {
                    transformed = (activity as ScanActivity).getBWBitmap(original)
                } catch (e: OutOfMemoryError) {
                    activity?.runOnUiThread {
                        transformed = original
                        scannedImage.setImageBitmap(original)
                        e.printStackTrace()
                        dismissDialog()
                        //onClick(v)
                    }
                }
                activity?.runOnUiThread {
                    scannedImage.setImageBitmap(transformed)
                    dismissDialog()
                }
            }
        }

        val bitmap = getBitmap()
        setScannedImage(bitmap)

        doneButton.setOnClickListener {
            showProgressDialog(resources.getString(R.string.loading))
            AsyncTask.execute {
                try {
                    val data = Intent()
                    var bitmap: Bitmap? = transformed
                    if (bitmap == null) {
                        bitmap = original
                    }
                    val uri = Utils.getUri(requireContext(), bitmap)
                    data.putExtra(ScanConstants.SCANNED_RESULT, uri)
                    activity?.setResult(Activity.RESULT_OK, data)
                    original?.recycle()
                    System.gc()
                    activity?.runOnUiThread {
                        dismissDialog()
                        activity?.finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getBitmap(): Bitmap? {
        val uri = getUri()
        try {
            original = Utils.getBitmap(requireContext(), uri)
            requireActivity().contentResolver.delete(uri!!, null, null)
            return original
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getUri(): Uri? {
        return arguments?.getParcelable(ScanConstants.SCANNED_RESULT)
    }

    fun setScannedImage(scannedImageBitmap: Bitmap?) {
        scannedImage?.setImageBitmap(scannedImageBitmap)
    }

    @Synchronized
    private fun showProgressDialog(message: String?) {
        if (progressDialogFragment != null && progressDialogFragment?.isVisible == true) {
            // Before creating another loading dialog, close all opened loading dialogs (if any)
            progressDialogFragment?.dismissAllowingStateLoss()
        }
        progressDialogFragment = null
        progressDialogFragment = ProgressDialogFragment(message)
        val fm = childFragmentManager
        progressDialogFragment?.show(fm, ProgressDialogFragment::class.java.toString())
    }

    @Synchronized
    private fun dismissDialog() {
        progressDialogFragment?.dismissAllowingStateLoss()
    }
}