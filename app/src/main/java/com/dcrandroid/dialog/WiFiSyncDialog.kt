package com.dcrandroid.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.Window
import com.dcrandroid.R
import kotlinx.android.synthetic.main.wifi_sync_dialog.*

class WiFiSyncDialog(context: Context) : Dialog(context), View.OnClickListener {

    private var btnPositiveClick: DialogInterface.OnClickListener? = null
    var checked: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.wifi_sync_dialog)

        btn_positive.setOnClickListener(this)
        btn_negative.setOnClickListener(this)
        btn_always.setOnClickListener(this)
    }

    fun setPositiveButton(listener: DialogInterface.OnClickListener?): WiFiSyncDialog {
        btnPositiveClick = listener
        return this
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_negative -> {
                cancel()
            }
            R.id.btn_positive -> {
                dismiss()
                if (btnPositiveClick != null) {
                    btnPositiveClick?.onClick(this, DialogInterface.BUTTON_POSITIVE)
                }
            }
            R.id.btn_always -> {
                checked = true
                dismiss()
                if (btnPositiveClick != null) {
                    btnPositiveClick?.onClick(this, DialogInterface.BUTTON_POSITIVE)
                }
            }
        }
    }
}