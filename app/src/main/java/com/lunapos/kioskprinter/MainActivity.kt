package com.lunapos.kioskprinter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.lunapos.kioskprinter.adapters.PrinterEmptyListAdapter
import com.lunapos.kioskprinter.adapters.PrinterListAdapter
import com.lunapos.kioskprinter.singletons.ACTION_USB_PERMISSION
import com.lunapos.kioskprinter.singletons.BluetoothPrinterPermissions
import com.lunapos.kioskprinter.singletons.CoroutinePrinter
import com.lunapos.kioskprinter.singletons.PRINTER_ADDED_KEY
import com.lunapos.kioskprinter.singletons.PRINTER_NOTIFICATION_ID
import com.lunapos.kioskprinter.singletons.PRINTER_UPDATED_KEY
import com.lunapos.kioskprinter.singletons.PrinterData
import com.lunapos.kioskprinter.singletons.SERVER_NOTIFICATION_ID
import com.lunapos.kioskprinter.singletons.SharedPrefsManager
import com.lunapos.kioskprinter.singletons.WebServer


class MainActivity : AppCompatActivity() {
    private var serverUp = false
    private val serverPort = 5000
    private var notificationManager: NotificationManagerCompat? = null

    private val webServer = WebServer.getInstance()
    private val coroutinePrinter = CoroutinePrinter.getInstance()

    private var printerListAdapter: PrinterListAdapter? = null
    private var printerEmptyListAdapter: PrinterEmptyListAdapter? = null

    @SuppressLint("MissingPermission")
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newPrinter = result.data!!.getStringExtra(PRINTER_ADDED_KEY)

            if (newPrinter != null) {
                val gson = Gson()
                val newPrinterObj = gson.fromJson(newPrinter, PrinterData::class.java)
                coroutinePrinter.printers.add(newPrinterObj)

                printerListAdapter!!.notifyDataSetChanged()

                newPrinterObj.retrieveDeviceConnection(applicationContext, this)
            }

            val updatedPrinter = result.data!!.getStringExtra(PRINTER_UPDATED_KEY)

            if (updatedPrinter != null) {
                val gson = Gson()
                val newPrinterObj = gson.fromJson(updatedPrinter, PrinterData::class.java)
                coroutinePrinter.printers[newPrinterObj.id!!] = newPrinterObj

                printerListAdapter!!.notifyDataSetChanged()

                newPrinterObj.retrieveDeviceConnection(applicationContext, this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.kiosk_appbar))

        SharedPrefsManager.init(applicationContext)

        val serverSwitch: SwitchCompat = findViewById(R.id.server_switch)

        notificationManager = NotificationManagerCompat.from(applicationContext)


        // Register USB Receiver
        val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
        val attachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        val detachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, permissionFilter)
        registerReceiver(usbReceiver, attachedFilter)
        registerReceiver(usbReceiver, detachedFilter)

        val printerListView: RecyclerView = findViewById(R.id.printer_list_view)
        val emptyPrinterListView: View = findViewById(R.id.empty_printer_list_view)
        val btnAddPrinter = findViewById<Button>(R.id.btn_add_printer)

        // load data
        coroutinePrinter.printers = SharedPrefsManager.readAsPrinterData(applicationContext, this)
        coroutinePrinter.printers.forEach { item ->
            item.retrieveDeviceConnection(applicationContext, this)
        }

        printerListAdapter = PrinterListAdapter(this, resultLauncher, coroutinePrinter.printers)
        printerEmptyListAdapter = PrinterEmptyListAdapter(printerListView, emptyPrinterListView, btnAddPrinter)

        printerListView.adapter = printerListAdapter
        printerListView.layoutManager = LinearLayoutManager(this)
        printerListAdapter!!.registerAdapterDataObserver(printerEmptyListAdapter!!)

        printerListAdapter!!.notifyDataSetChanged()

        // Register Server switch listener
        serverSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (isChecked) {
                serverUp = true
                webServer.startServer(serverPort, this, notificationManager!!)
                webServer.coroutinePrinter = coroutinePrinter
            } else {
                serverUp = false
                webServer.stopServer(this, notificationManager!!)
                notificationManager!!.cancel(SERVER_NOTIFICATION_ID)
            }
        }

        btnAddPrinter.setOnClickListener {
            val intent = Intent(this, PrinterInputForm::class.java)
            resultLauncher.launch(intent)
        }

        val btnAddPrinterEmpty = findViewById<Button>(R.id.btn_add_printer_empty)
        btnAddPrinterEmpty.setOnClickListener {
            val intent = Intent(this, PrinterInputForm::class.java)
            resultLauncher.launch(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer.stopServer(this, notificationManager!!)
        notificationManager!!.cancel(SERVER_NOTIFICATION_ID)
        notificationManager!!.cancel(PRINTER_NOTIFICATION_ID)
        unregisterReceiver(usbReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        BluetoothPrinterPermissions.onRequestPermissionsResult(applicationContext, this, requestCode, grantResults)
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Toast.makeText(
                            context,
                            "Permission granted for this device",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Permission denied for this device",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                Toast.makeText(
                    context,
                    "USB device attached",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                Toast.makeText(
                    context,
                    "Usb device detached",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}