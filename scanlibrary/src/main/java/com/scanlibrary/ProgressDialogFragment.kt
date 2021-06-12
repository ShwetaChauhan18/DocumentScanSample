package com.scanlibrary

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.DialogFragment

class ProgressDialogFragment(var message: String?) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = ProgressDialog(requireContext())
        dialog.isIndeterminate = true
        dialog.setMessage(message)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        // Disable the back button
        val keyListener: DialogInterface.OnKeyListener =
            DialogInterface.OnKeyListener { _, keyCode, _ ->
                keyCode == KeyEvent.KEYCODE_BACK
            }
        dialog.setOnKeyListener(keyListener)
        return dialog
    }
}