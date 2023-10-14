package com.lunapos.kioskprinter.singletons

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

abstract class AbstractPrinter {
    var printerDpi: Int = 0
    var printerWidthMM: Float = 0f
    var printerNbrCharactersPerLine: Int = 0
    var text: String = ""

    abstract fun onRequestPermissionsResult(
        context: Context,
        appCompatActivity: AppCompatActivity,
        requestCode: Int,
        grantResults: IntArray
    )
    abstract suspend fun print()
}