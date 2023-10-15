package com.lunapos.kioskprinter.singletons

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.lunapos.kioskprinter.R

class BluetoothPrinter : AbstractPrinter() {
    interface OnBluetoothPermissionsGranted {
        fun onPermissionsGranted()
    }

    private val permissionBluetooth = 1
    private val permissionBluetoothAdmin = 2
    private val permissionBluetoothConnect = 3
    private val permissionBluetoothScan = 4

    private var onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted? = null

    override fun onRequestPermissionsResult(
        context: Context,
        appCompatActivity: AppCompatActivity,
        requestCode: Int,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                permissionBluetooth, permissionBluetoothAdmin, permissionBluetoothConnect, permissionBluetoothScan -> checkBluetoothPermissions(
                    context, appCompatActivity, onBluetoothPermissionsGranted)
            }
        }
    }

    private fun checkBluetoothPermissions(context: Context, appCompatActivity: AppCompatActivity, onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted?) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                appCompatActivity,
                arrayOf(Manifest.permission.BLUETOOTH),
                permissionBluetooth
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                appCompatActivity,
                arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                permissionBluetoothAdmin
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                appCompatActivity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                permissionBluetoothConnect
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                appCompatActivity,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                permissionBluetoothScan
            )
        } else {
            this.onBluetoothPermissionsGranted!!.onPermissionsGranted()
        }
    }

    private var selectedDevice: BluetoothConnection? = null

    fun browseBluetoothDevice(context: Context, appCompatActivity: AppCompatActivity) {
        checkBluetoothPermissions(context, appCompatActivity, object : OnBluetoothPermissionsGranted {
            override fun onPermissionsGranted() {
                val bluetoothDevicesList =
                    BluetoothPrintersConnections().list
                if (bluetoothDevicesList != null) {
                    val items =
                        arrayOfNulls<String>(bluetoothDevicesList.size + 1)
                    items[0] = "Default printer"
                    for ((i, device) in bluetoothDevicesList.withIndex()) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        items[i + 1] = device.device.name
                    }
                    val alertDialog: AlertDialog.Builder = AlertDialog.Builder(appCompatActivity)
                    alertDialog.setTitle("Bluetooth printer selection")
                    alertDialog.setItems(
                        items
                    ) { _, i1 ->
                        val index: Int = i1 - 1
                        selectedDevice = if (index == -1) {
                            null
                        } else {
                            bluetoothDevicesList[index]
                        }
                        val button =
                            appCompatActivity.findViewById<View>(R.id.button_bluetooth_browse) as Button
                        button.text = items[i1]
                    }
                    val alert: AlertDialog = alertDialog.create()
                    alert.setCanceledOnTouchOutside(false)
                    alert.show()
                }
            }
        })
    }

    override suspend fun print(context: Context) {
        if (selectedDevice != null) {
            val printer = EscPosPrinter(
                this.selectedDevice,
                this.printerDpi,
                this.printerWidthMM,
                this.printerNbrCharactersPerLine
            )
            printer.printFormattedTextAndCut(this.text.trimIndent())
        }
    }
}