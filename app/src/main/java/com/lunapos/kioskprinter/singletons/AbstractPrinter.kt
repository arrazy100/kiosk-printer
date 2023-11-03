package com.lunapos.kioskprinter.singletons

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope

abstract class AbstractPrinter {
    var name: String = ""
    var printerDpi: Int = 0
    var printerWidthMM: Float = 0f
    var printerNbrCharactersPerLine: Int = 0
    var text: String = ""

    val coroutineScope: CoroutineScope? = null

    abstract fun onRequestPermissionsResult(
        context: Context,
        appCompatActivity: AppCompatActivity,
        requestCode: Int,
        grantResults: IntArray
    )
    abstract suspend fun print(context: Context)
}