package com.scanlibrary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import kotlinx.android.synthetic.main.scan_fragment_layout.polygonView
import kotlinx.android.synthetic.main.scan_fragment_layout.scanButton
import kotlinx.android.synthetic.main.scan_fragment_layout.sourceFrame
import kotlinx.android.synthetic.main.scan_fragment_layout.sourceImageView

class ScanFragmentKotlin : Fragment() {

    private var progressDialogFragment: ProgressDialogFragment? = null
    private var scanner: IScanner? = null
    private var original: Bitmap? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is IScanner) {
            throw ClassCastException("Activity must implement IScanner")
        }
        this.scanner = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.scan_fragment_layout, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        scanButton.setOnClickListener {
            val points: Map<Int?, PointF?> = polygonView.points
            if (isScanPointsValid(points)) {
                scanAsyncTaskAlter(points)
            } else {
                showErrorDialog()
            }
        }
        sourceFrame.post {
            original = getBitmap()
            if (original != null) {
                setBitmap(original!!)
            }
        }
    }

    private fun getImagePath(): String? {
        return if (arguments != null) arguments?.getString(ScanConstants.IMAGE_BASE_PATH_EXTRA) else ""
    }

    private fun getBitmap(): Bitmap? {
        val uri = getUri()
        try {
            val bitmap = Utils.getBitmap(requireContext(), uri)
            requireActivity().contentResolver.delete(uri!!, null, null)
            return bitmap
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getUri(): Uri? {
        return arguments?.getParcelable(ScanConstants.SELECTED_BITMAP)
    }

    private fun setBitmap(original: Bitmap) {
        val scaledBitmap = scaledBitmap(original, sourceFrame!!.width, sourceFrame!!.height)
        sourceImageView.setImageBitmap(scaledBitmap)
        val tempBitmap = (sourceImageView.drawable as BitmapDrawable).bitmap
        val pointFs = getEdgePoints(tempBitmap)
        polygonView.points = pointFs
        polygonView.visibility = View.VISIBLE
        val padding = resources.getDimension(R.dimen.scanPadding).toInt()
        val layoutParams = FrameLayout.LayoutParams(
            tempBitmap.width + 2 * padding, tempBitmap.height + 2 * padding
        )
        layoutParams.gravity = Gravity.CENTER
        polygonView.layoutParams = layoutParams
    }

    private fun getEdgePoints(tempBitmap: Bitmap): Map<Int, PointF> {
        val pointFs = getContourEdgePoints(tempBitmap)
        return orderedValidEdgePoints(tempBitmap, pointFs)
    }

    private fun getContourEdgePoints(tempBitmap: Bitmap): List<PointF> {
        val points: FloatArray? = (activity as ScanActivity?)?.getPoints(tempBitmap)
        val pointFs: MutableList<PointF> = ArrayList()

        points?.let { point ->
            val x1 = point[0]
            val x2 = point[1]
            val x3 = point[2]
            val x4 = point[3]
            val y1 = point[4]
            val y2 = point[5]
            val y3 = point[6]
            val y4 = point[7]
            pointFs.add(PointF(x1, y1))
            pointFs.add(PointF(x2, y2))
            pointFs.add(PointF(x3, y3))
            pointFs.add(PointF(x4, y4))
        }

        return pointFs
    }

    private fun getOutlinePoints(tempBitmap: Bitmap): Map<Int, PointF> {
        val outlinePoints: MutableMap<Int, PointF> = HashMap()
        outlinePoints[0] = PointF(0f, 0f)
        outlinePoints[1] = PointF(tempBitmap.width.toFloat(), 0f)
        outlinePoints[2] = PointF(0f, tempBitmap.height.toFloat())
        outlinePoints[3] = PointF(tempBitmap.width.toFloat(), tempBitmap.height.toFloat())
        return outlinePoints
    }

    private fun orderedValidEdgePoints(
        tempBitmap: Bitmap, pointFs: List<PointF>
    ): Map<Int, PointF> {
        var orderedPoints = polygonView!!.getOrderedPoints(pointFs)
        if (!polygonView!!.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap)
        }
        return orderedPoints
    }

    private fun showErrorDialog() {
        val fragment =
            SingleButtonDialogFragment(R.string.ok, getString(R.string.cantCrop), "Error", true)
        val fm = childFragmentManager
        fragment.show(fm, SingleButtonDialogFragment::class.java.toString())
    }

    private fun isScanPointsValid(points: Map<Int?, PointF?>): Boolean {
        return points.size == 4
    }

    private fun scaledBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val m = Matrix()
        m.setRectToRect(
            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            Matrix.ScaleToFit.CENTER
        )
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun getScannedBitmap(original: Bitmap?, points: Map<Int?, PointF?>): Bitmap? {
        val width = original?.width!!
        val height = original.height
        val xRatio = width.toFloat() / sourceImageView.width
        val yRatio = height.toFloat() / sourceImageView.height

        val x1 = points[0]!!.x * xRatio
        val x2 = points[1]!!.x * xRatio
        val x3 = points[2]!!.x * xRatio
        val x4 = points[3]!!.x * xRatio
        val y1 = points[0]!!.y * yRatio
        val y2 = points[1]!!.y * yRatio
        val y3 = points[2]!!.y * yRatio
        val y4 = points[3]!!.y * yRatio

        Log.d("", "Points($x1,$y1)($x2,$y2)($x3,$y3)($x4,$y4)")

        return (activity as ScanActivity?)?.getScannedBitmap(
            original, x1, y1, x2, y2, x3, y3, x4, y4
        )
    }

    private fun scanAsyncTaskAlter(points: Map<Int?, PointF?>): Bitmap? {
        lifecycleScope.executeAsyncTask(onPreExecute = {
            // ... runs in Main Thread
            showProgressDialog(getString(R.string.scanning))
        }, doInBackground = { _: suspend (progress: Int) -> Unit ->

            // ... runs in Background Thread

            val bitmap: Bitmap? = getScannedBitmap(original, points)
            val uri = Utils.getUri(requireActivity(), bitmap)
            scanner?.onScanFinish(uri)
            bitmap
            //send data to "onPostExecute"
        }, onPostExecute = { bitmap ->
            // runs in Main Thread
            // ... here "it" is a data returned from "doInBackground"
            bitmap?.recycle()
            dismissDialog()
        }, onProgressUpdate = {
            // runs in Main Thread
            // ... here "it" contains progress
        })

        return null
    }

    fun showProgressDialog(message: String?) {
        progressDialogFragment = ProgressDialogFragment(message)
        val fm = parentFragmentManager
        progressDialogFragment?.show(fm, ProgressDialogFragment::class.java.toString())
    }

    fun dismissDialog() {
        progressDialogFragment?.dismissAllowingStateLoss()
    }
}