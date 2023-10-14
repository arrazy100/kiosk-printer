package com.lunapos.kioskprinter.singletons

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections

class UsbPrinter : AbstractPrinter() {

    override fun onRequestPermissionsResult(
        context: Context,
        appCompatActivity: AppCompatActivity,
        requestCode: Int,
        grantResults: IntArray
    ) {
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val usbManager =
                        context.getSystemService(Context.USB_SERVICE) as UsbManager?
                    val usbDevice =
                        intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            printToUsbAttached(usbManager, usbDevice)
                        }
                    }
                }
            }
        }
    }

    private fun printToUsbAttached(usbManager: UsbManager, usbDevice: UsbDevice) {
        val printer = EscPosPrinter(UsbConnection(usbManager, usbDevice), this.printerDpi,
            this.printerWidthMM, this.printerNbrCharactersPerLine)

        printer.printFormattedTextAndCut(this.text.trimIndent())
    }

    override suspend fun print(context: Context) {
        val usbConnection = UsbPrintersConnections.selectFirstConnected(context)
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager?
        if (usbConnection != null && usbManager != null) {
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            context.registerReceiver(usbReceiver, filter)
            usbManager.requestPermission(usbConnection.device, permissionIntent)
        }
    }
}