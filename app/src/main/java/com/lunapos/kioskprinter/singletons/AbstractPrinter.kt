package com.lunapos.kioskprinter.singletons

import android.content.Context
import android.hardware.usb.UsbDevice
import androidx.appcompat.app.AppCompatActivity
import com.lunapos.kioskprinter.enums.PrinterModuleEnum
import kotlinx.coroutines.CoroutineScope

abstract class AbstractPrinter {
    var name: String = ""
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

    abstract suspend fun print(context: Context)
}