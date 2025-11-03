package com.ali8dev.webviewdemo.util

import android.app.AlertDialog
import android.content.Context
import com.ali8dev.webviewdemo.R

class AlertDialog {

    fun showAlertDialog(context: Context, title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(context.getString(R.string.close)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false) // Prevent dismiss on outside touch
        dialog.show()
    }
}