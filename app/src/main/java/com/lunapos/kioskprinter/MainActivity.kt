package com.lunapos.kioskprinter

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Parcelable
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
    private var notificationManager: NotificationManagerCompat? = null

    private val webServer = WebServer.getInstance()
    private val notifications = Notifications.getInstance()
    private val bluetoothPrinter = BluetoothPrinter()
    private val usbPrinter = UsbPrinter()
    private val tcpPrinter = TcpPrinter()
    private val coroutinePrinter = CoroutinePrinter.getInstance()

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device =
                        intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val port = 5000
        notificationManager = NotificationManagerCompat.from(applicationContext)

        val serverButton: Button = findViewById(R.id.serverButton)
        serverButton.setOnClickListener {
            serverUp = if(!serverUp){
                webServer.startServer(port, this, applicationContext, notificationManager!!)
                true
            } else{
                webServer.stopServer(applicationContext, this, notificationManager!!)
                false
            }
        }

        val browseBluetoothButton = findViewById<View>(R.id.button_bluetooth_browse) as Button

        val textView = findViewById<TextView>(R.id.usb_device)

//        browseBluetoothButton.setOnClickListener { bluetoothPrinter.browseBluetoothDevice(applicationContext, this) }
        browseBluetoothButton.setOnClickListener { usbPrinter.browseUsb(this, browseBluetoothButton, textView) }

        val printButton = findViewById<View>(R.id.bluetooth_print) as Button
        printButton.setOnClickListener { coroutinePrinter.doPrint(notifications, applicationContext, notificationManager!!) }

        val tcpHost = findViewById<EditText>(R.id.tcp_input)
        tcpHost.setText("10.20.30.101")

        val tcpPort = findViewById<EditText>(R.id.port_input)
        tcpPort.setText("9100")

        usbPrinter.usbManager = getSystemService(USB_SERVICE) as UsbManager

        coroutinePrinter.addPrinter(usbPrinter)

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
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
}