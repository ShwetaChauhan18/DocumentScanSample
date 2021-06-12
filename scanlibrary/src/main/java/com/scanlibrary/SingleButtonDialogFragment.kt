package com.scanlibrary

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class SingleButtonDialogFragment(
    private var positiveButtonTitle: Int,
    private var message: String,
    private var title: String,
    private var isCanceLable: Boolean

) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(requireActivity()).setTitle(title).setCancelable(isCanceLable)
                .setMessage(message).setPositiveButton(positiveButtonTitle,
                    DialogInterface.OnClickListener { dialog, which -> })
        return builder.create()
    }
}