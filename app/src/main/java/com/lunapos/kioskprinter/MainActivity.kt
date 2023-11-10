package com.lunapos.kioskprinter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.lunapos.kioskprinter.Constants.ACTION_USB_PERMISSION
import com.lunapos.kioskprinter.Constants.FORM_ADD_KEY
import com.lunapos.kioskprinter.Constants.PRINTER_ADDED_KEY
import com.lunapos.kioskprinter.Constants.PRINTER_NOTIFICATION_ID
import com.lunapos.kioskprinter.Constants.PRINTER_UPDATED_KEY
import com.lunapos.kioskprinter.Constants.SERVER_NOTIFICATION_ID
import com.lunapos.kioskprinter.PrinterUtil.BluetoothPrinterPermissions
import com.lunapos.kioskprinter.adapters.PrinterEmptyListAdapter
import com.lunapos.kioskprinter.adapters.PrinterListAdapter
import com.lunapos.kioskprinter.dtos.PrinterData
import com.lunapos.kioskprinter.singletons.CoroutinePrinter
import com.lunapos.kioskprinter.singletons.WebServer
import com.lunapos.kioskprinter.viewModel.PrinterEntityViewModel


class MainActivity : AppCompatActivity() {
    private var serverUp = false
    private val serverPort = 5000
    private var notificationManager: NotificationManagerCompat? = null

    private val webServer = WebServer.getInstance()
    private val coroutinePrinter = CoroutinePrinter.getInstance()

    private var printerListAdapter: PrinterListAdapter? = null
    private var printerEmptyListAdapter: PrinterEmptyListAdapter? = null
    private lateinit var printerViewModel: PrinterEntityViewModel

    @SuppressLint("MissingPermission")
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newPrinter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data!!.getSerializableExtra(PRINTER_ADDED_KEY, PrinterData::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data!!.getSerializableExtra(PRINTER_ADDED_KEY) as? PrinterData
            }

            if (newPrinter != null) {
                newPrinter.retrieveDeviceConnection(applicationContext, this)

                val printerEntity = printerViewModel.convertToPrinterEntity(newPrinter)
                val insertedId = printerViewModel.savePrinter(printerEntity)

                newPrinter.id = insertedId
                printerListAdapter!!.dataSet.add(newPrinter)
            }

            val updatedPrinter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data!!.getSerializableExtra(PRINTER_UPDATED_KEY, PrinterData::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data!!.getSerializableExtra(PRINTER_UPDATED_KEY) as? PrinterData
            }

            if (updatedPrinter != null) {
                updatedPrinter.retrieveDeviceConnection(applicationContext, this)

                val index = printerListAdapter!!.dataSet.indexOfFirst { it.id == updatedPrinter.id }

                if (index != -1) {
                    val printerEntity = printerViewModel.convertToPrinterEntity(updatedPrinter)
                    printerEntity.id = updatedPrinter.id!!
                    printerViewModel.updatePrinter(printerEntity)

                    printerListAdapter!!.dataSet[index] = updatedPrinter
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            // permission granted
        } else {
            Snackbar.make(
                findViewById<View>(android.R.id.content).rootView,
                "Please grant Notification permission from App Settings",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.kiosk_appbar))

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // permission granted
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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

        printerListAdapter = PrinterListAdapter(this, resultLauncher, mutableListOf())
        printerEmptyListAdapter = PrinterEmptyListAdapter(printerListView, emptyPrinterListView, btnAddPrinter)

        printerListView.adapter = printerListAdapter
        printerListView.layoutManager = LinearLayoutManager(this)
        printerListAdapter!!.registerAdapterDataObserver(printerEmptyListAdapter!!)

        printerViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[PrinterEntityViewModel::class.java]
        printerViewModel.allPrinters.observe(this) { printers ->
            printerListAdapter!!.setData(printerViewModel.convertToPrinterData(printers))
            printerListAdapter!!.dataSet.forEach { item ->
                item.retrieveDeviceConnection(applicationContext, this)
            }
            printerListAdapter!!.setViewModel(printerViewModel)
            printerListAdapter!!.notifyDataSetChanged()
            coroutinePrinter.printers = printerListAdapter!!.dataSet
        }

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
            intent.putExtra(FORM_ADD_KEY, printerListAdapter!!.dataSet.size)
            resultLauncher.launch(intent)
        }

        val btnAddPrinterEmpty = findViewById<Button>(R.id.btn_add_printer_empty)
        btnAddPrinterEmpty.setOnClickListener {
            val intent = Intent(this, PrinterInputForm::class.java)
            intent.putExtra(FORM_ADD_KEY, printerListAdapter!!.dataSet.size)
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
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Snackbar.make(
                            findViewById<View>(android.R.id.content).rootView,
                            "Permission granted for this USB",
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        Snackbar.make(
                            findViewById<View>(android.R.id.content).rootView,
                            "Permission denied for this USB",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                Snackbar.make(
                    findViewById<View>(android.R.id.content).rootView,
                    "USB device attached",
                    Snackbar.LENGTH_LONG
                ).show()

                var appCompatActivity: AppCompatActivity? = null
                if (context is AppCompatActivity) {
                    appCompatActivity = context
                }

                printerListAdapter!!.dataSet.forEach { printer ->
                    if (appCompatActivity == null || device == null) return

                    if ((printer.usbProductId != null) && (printer.usbProductId == device.productId)) {
                        printer.retrieveDeviceConnection(applicationContext, appCompatActivity)
                    }
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                Snackbar.make(
                    findViewById<View>(android.R.id.content).rootView,
                    "USB device detached",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
}