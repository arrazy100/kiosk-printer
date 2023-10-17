package com.lunapos.kioskprinter.singletons

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.usb.UsbConnection


class UsbPrinter : AbstractPrinter() {
    var usbManager: UsbManager? = null
    var usbDevice: UsbDevice? = null
    private var usbDeviceList: HashMap<String, UsbDevice>? = null
    var usbConnection: UsbConnection? = null

    var browseButton: Button? = null
    var usbDeviceInformation: TextView? = null

    fun browseUsb(appCompatActivity: AppCompatActivity, button: Button?, textView: TextView?) {
        usbDeviceList = usbManager!!.deviceList

        if (usbDeviceList != null) {
            val devices = usbDeviceList!!.values.toList() // Convert the map values to a list

            val items = arrayOfNulls<String>(devices.size + 1)
            items[0] = "Default printer"

            val deviceNames = devices.map { it.deviceName } // Extract device names

            for (i in devices.indices) {
                items[i + 1] = deviceNames[i]
            }

            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(appCompatActivity)
            alertDialog.setTitle("USB printer selection")
            alertDialog.setItems(
                items
            ) { _, i1 ->
                val index: Int = i1 - 1
                usbDevice = if (index == -1) {
                    null
                } else {
                    devices[index]
                }

                if (button != null) button.text = items[i1].toString()

                if (usbDevice != null) {
                    val deviceInformation = """
                        DeviceID: ${usbDevice!!.deviceId}
                        DeviceName: ${usbDevice!!.deviceName}
                        Protocol: ${usbDevice!!.deviceProtocol}
                        DeviceClass: ${usbDevice!!.deviceClass} - ${translateDeviceClass(usbDevice!!.deviceClass)}
                        DeviceSubClass: ${usbDevice!!.deviceSubclass}
                        VendorID: ${usbDevice!!.vendorId}
                        ProductID: ${usbDevice!!.productId}
                    """

                    if (textView != null) textView.text = deviceInformation

                    var flag = 0
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.S) flag = PendingIntent.FLAG_MUTABLE

                    val mPermissionIntent =
                        PendingIntent.getBroadcast(appCompatActivity, 0, Intent(ACTION_USB_PERMISSION),
                            flag)

                    usbManager!!.requestPermission(usbDevice, mPermissionIntent)

                    this.usbConnection = UsbConnection(usbManager, usbDevice)
                }
            }
            val alert: AlertDialog = alertDialog.create()
            alert.setCanceledOnTouchOutside(false)
            alert.show()
        }
    }

    private fun translateDeviceClass(deviceClass: Int): String {
        return when (deviceClass) {
            UsbConstants.USB_CLASS_APP_SPEC -> "Application specific USB class"
            UsbConstants.USB_CLASS_AUDIO -> "USB class for audio devices"
            UsbConstants.USB_CLASS_CDC_DATA -> "USB class for CDC devices (communications device class)"
            UsbConstants.USB_CLASS_COMM -> "USB class for communication devices"
            UsbConstants.USB_CLASS_CONTENT_SEC -> "USB class for content security devices"
            UsbConstants.USB_CLASS_CSCID -> "USB class for content smart card devices"
            UsbConstants.USB_CLASS_HID -> "USB class for human interface devices (for example, mice and keyboards)"
            UsbConstants.USB_CLASS_HUB -> "USB class for USB hubs"
            UsbConstants.USB_CLASS_MASS_STORAGE -> "USB class for mass storage devices"
            UsbConstants.USB_CLASS_MISC -> "USB class for wireless miscellaneous devices"
            UsbConstants.USB_CLASS_PER_INTERFACE -> "USB class indicating that the class is determined on a per-interface basis"
            UsbConstants.USB_CLASS_PHYSICA -> "USB class for physical devices"
            UsbConstants.USB_CLASS_PRINTER -> "USB class for printers"
            UsbConstants.USB_CLASS_STILL_IMAGE -> "USB class for still image devices (digital cameras)"
            UsbConstants.USB_CLASS_VENDOR_SPEC -> "Vendor specific USB class"
            UsbConstants.USB_CLASS_VIDEO -> "USB class for video devices"
            UsbConstants.USB_CLASS_WIRELESS_CONTROLLER -> "USB class for wireless controller devices"
            else -> "Unknown USB class!"
        }
    }

    override fun onRequestPermissionsResult(
        context: Context,
        appCompatActivity: AppCompatActivity,
        requestCode: Int,
        grantResults: IntArray
    ) {
    }

    override suspend fun print(context: Context) {
//        if (usbManager != null && usbDevice != null) {
//            if (usbManager!!.hasPermission(usbDevice)) {
//                Log.i("Printer", "USB Printer Established")
//            }
//            else {
//                Log.i("Printer", "USB Printer Permission Denied")
//            }
//        }

        if (usbConnection != null) {
            try {
                val printer = EscPosPrinter(
                    usbConnection, this.printerDpi,
                    this.printerWidthMM, this.printerNbrCharactersPerLine
                )

                printer.printFormattedTextAndCut(this.text.trimIndent())
            } catch (_: Exception) {

            }
        }
    }
}