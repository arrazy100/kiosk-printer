package com.lunapos.kioskprinter.singletons

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections

object BluetoothPrinterPermissions {
    interface OnBluetoothPermissionsGranted {
        fun onPermissionsGranted()
    }

    private const val permissionBluetooth = 1
    private const val permissionBluetoothAdmin = 2
    private const val permissionBluetoothConnect = 3
    private const val permissionBluetoothScan = 4

    private var onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted? = null

    fun onRequestPermissionsResult(
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

    @SuppressLint("MissingPermission")
    fun listBluetoothDevice(bluetoothDeviceList : ArrayList<BluetoothConnection?>) : ArrayList<String> {
        val deviceList = bluetoothDeviceList.map {
            it!!.device.name
        }

        return ArrayList(deviceList)
    }

    fun retrieveBluetoothDevice(macAddress: String?, context: Context, appCompatActivity: AppCompatActivity) : BluetoothConnection? {
        var selectedDevice : BluetoothConnection? = null

        if (macAddress == null) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            if (bluetoothAdapter?.isEnabled == false) {
                return null
            }
        } else {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                return null
            }
        }

        checkBluetoothPermissions(context, appCompatActivity, object : OnBluetoothPermissionsGranted {
            override fun onPermissionsGranted() {
                val devices = BluetoothPrintersConnections().list

                devices?.forEach { item ->
                    if (item.device.address == macAddress.toString()) {
                        selectedDevice = item
                        return
                    }
                }
            }
        })

        return selectedDevice
    }

    fun getBluetoothDevices(context: Context, appCompatActivity: AppCompatActivity) : ArrayList<BluetoothConnection?> {
        var bluetoothDevices = arrayListOf<BluetoothConnection?>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter ?: return bluetoothDevices

            if (!bluetoothAdapter.isEnabled) {
                return bluetoothDevices
            }
        } else {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return bluetoothDevices

            if (!bluetoothAdapter.isEnabled) {
                return bluetoothDevices
            }
        }

        checkBluetoothPermissions(context, appCompatActivity, object : OnBluetoothPermissionsGranted {
            override fun onPermissionsGranted() {
                val devices = BluetoothPrintersConnections().list!!.toList()
                bluetoothDevices = ArrayList(devices)
            }
        })

        return bluetoothDevices
    }
}