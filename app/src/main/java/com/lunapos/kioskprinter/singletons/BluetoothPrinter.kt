package com.lunapos.kioskprinter.singletons

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections

class BluetoothPrinter : AbstractPrinter() {
    private var selectedDevice: BluetoothConnection? = null

    interface OnBluetoothPermissionsGranted {
        fun onPermissionsGranted()
    }

    private val permissionBluetooth = 1
    private val permissionBluetoothAdmin = 2
    private val permissionBluetoothConnect = 3
    private val permissionBluetoothScan = 4

    private var onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted? = null

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

    override suspend fun print(context: Context) {
        if (selectedDevice != null) {
            try {
                val printer = EscPosPrinter(
                    this.selectedDevice,
                    this.printerDpi,
                    this.printerWidthMM,
                    this.printerNbrCharactersPerLine
                )
                printer.printFormattedTextAndCut(this.text.trimIndent())
            }
            catch (_: Exception) {

            }
        }
    }
}