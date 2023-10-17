package com.lunapos.kioskprinter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.lunapos.kioskprinter.singletons.ACTION_USB_PERMISSION
import com.lunapos.kioskprinter.singletons.BluetoothPrinter
import com.lunapos.kioskprinter.singletons.CoroutinePrinter
import com.lunapos.kioskprinter.singletons.Notifications
import com.lunapos.kioskprinter.singletons.PRINTER_NOTIFICATION_ID
import com.lunapos.kioskprinter.singletons.SERVER_NOTIFICATION_ID
import com.lunapos.kioskprinter.singletons.TcpPrinter
import com.lunapos.kioskprinter.singletons.UsbPrinter
import com.lunapos.kioskprinter.singletons.WebServer


class MainActivity : AppCompatActivity() {
    private var serverUp = false
    private val serverPort = 5000
    private var notificationManager: NotificationManagerCompat? = null

    private val webServer = WebServer.getInstance()
    private val notifications = Notifications.getInstance()
    private val bluetoothPrinter = BluetoothPrinter()
    private val usbPrinter = UsbPrinter()
    private val tcpPrinter = TcpPrinter()
    private val coroutinePrinter = CoroutinePrinter.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val serverButton: Button = findViewById(R.id.serverButton)
        val browseBluetoothButton = findViewById<View>(R.id.button_bluetooth_browse) as Button
        val browseUsbButton = findViewById<View>(R.id.button_usb_browse) as Button
        val tcpHost = findViewById<EditText>(R.id.tcp_input)
        val tcpPort = findViewById<EditText>(R.id.port_input)
        val printButton = findViewById<View>(R.id.bluetooth_print) as Button
        val textView = findViewById<TextView>(R.id.usb_device)


        tcpHost.setText("10.20.30.101")
        tcpPort.setText("9100")


        notificationManager = NotificationManagerCompat.from(applicationContext)


        // Register USB Receiver
        val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
        val attachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        val detachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, permissionFilter)
        registerReceiver(usbReceiver, attachedFilter)
        registerReceiver(usbReceiver, detachedFilter)


        // Register Printer
        bluetoothPrinter.printerDpi = 203
        bluetoothPrinter.printerWidthMM = 48f
        bluetoothPrinter.printerNbrCharactersPerLine = 32
        bluetoothPrinter.text = "[C]Test Bluetooth Printer"

        usbPrinter.usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        usbPrinter.printerDpi = 203
        usbPrinter.printerWidthMM = 48f
        usbPrinter.printerNbrCharactersPerLine = 32
        usbPrinter.text = "[C]Test USB Printer"
        usbPrinter.browseButton = browseUsbButton
        usbPrinter.usbDeviceInformation = textView

        tcpPrinter.ipAddress = tcpHost.text.toString()
        tcpPrinter.portAddress = tcpPort.text.toString()
        tcpPrinter.timeout = 1000
        tcpPrinter.printerDpi = 203
        tcpPrinter.printerWidthMM = 48f
        tcpPrinter.printerNbrCharactersPerLine = 32
        tcpPrinter.text = "[C]Test TCP Printer"

        coroutinePrinter.addPrinter(bluetoothPrinter)
        coroutinePrinter.addPrinter(usbPrinter)
        coroutinePrinter.addPrinter(tcpPrinter)


        // Register Button Listener
        serverButton.setOnClickListener {
            serverUp = if(!serverUp){
                webServer.startServer(serverPort, this, applicationContext, notificationManager!!)
                webServer.coroutinePrinter = coroutinePrinter
                true
            } else{
                webServer.stopServer(applicationContext, this, notificationManager!!)
                notificationManager!!.cancel(SERVER_NOTIFICATION_ID)
                false
            }
        }

        browseBluetoothButton.setOnClickListener { bluetoothPrinter.browseBluetoothDevice(applicationContext, this) }

        browseUsbButton.setOnClickListener { usbPrinter.browseUsb(this, browseUsbButton, textView) }

        printButton.setOnClickListener { coroutinePrinter.doPrint(notifications, applicationContext, notificationManager!!, printButton) }
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer.stopServer(applicationContext, this, notificationManager!!)
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

        for (printer in coroutinePrinter.printers) {
            printer.onRequestPermissionsResult(
                applicationContext,
                this,
                requestCode,
                grantResults
            )
        }
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
                for (printer in coroutinePrinter.printers) {
                    if (printer !is UsbPrinter) continue

                    if (usbPrinter.usbDevice != null) {
                        if (usbPrinter.usbDevice == device) {
                            Toast.makeText(
                                context,
                                "Usb device detached",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        printer.usbConnection = null
                        printer.browseButton!!.text = "Browse USB"
                        printer.usbDeviceInformation!!.text = ""
                    }
                }
            }
        }
    }
}