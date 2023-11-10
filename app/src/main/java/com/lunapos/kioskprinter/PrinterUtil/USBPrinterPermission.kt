package com.lunapos.kioskprinter.PrinterUtil

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.lunapos.kioskprinter.Constants.ACTION_USB_PERMISSION

object USBPrinterPermission {
    fun listUsbDevice(usbDeviceList : ArrayList<UsbDevice>) : ArrayList<String> {
        val deviceList = usbDeviceList.map {
            it.deviceName
        }

        return ArrayList(deviceList)
    }

    fun requestUsbDevicePermission(context: Context, appCompatActivity: AppCompatActivity, usbDevice: UsbDevice?) {
        val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager

        var flag = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flag =
            PendingIntent.FLAG_MUTABLE

        val mPermissionIntent =
            PendingIntent.getBroadcast(
                appCompatActivity, 0, Intent(ACTION_USB_PERMISSION),
                flag
            )

        if (!usbManager.hasPermission(usbDevice)) {
//            usbManager.requestPermission(usbDevice, mPermissionIntent)
            val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager

            var flag = 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flag =
                PendingIntent.FLAG_MUTABLE

            val mPermissionIntent =
                PendingIntent.getBroadcast(
                    appCompatActivity, 0, Intent(ACTION_USB_PERMISSION),
                    flag
                )

            if (!usbManager.hasPermission(usbDevice)) {
                usbManager.requestPermission(usbDevice, mPermissionIntent)
            }
        }
    }

    fun retrieveUsbDevice(productId: Int, vendorId: Int, context: Context, appCompatActivity: AppCompatActivity) : UsbDevice? {
        val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
        val usbDeviceList = usbManager.deviceList
        var usbDevice: UsbDevice? = null

        if (usbDeviceList != null) {
            val devices = usbDeviceList.values.toList()

            devices.forEach { item ->
                if (item.productId == productId && item.vendorId == vendorId) {
                    usbDevice = item
                }
            }

            if (usbDevice != null) {
                requestUsbDevicePermission(context, appCompatActivity, usbDevice)
            }
        }

        return usbDevice
    }

    fun getUsbDevices(context: Context) : ArrayList<UsbDevice> {
        val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
        val usbDeviceList = usbManager.deviceList

        if (usbDeviceList != null) {
            val devices = usbDeviceList.values.toList()

            return ArrayList(devices)
        }

        return ArrayList()
    }
}