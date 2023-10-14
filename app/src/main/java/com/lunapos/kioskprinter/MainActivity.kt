package com.lunapos.kioskprinter

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
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
        browseBluetoothButton.setOnClickListener { bluetoothPrinter.browseBluetoothDevice(applicationContext, this) }

        val printButton = findViewById<View>(R.id.bluetooth_print) as Button
        printButton.setOnClickListener { coroutinePrinter.doPrint(notifications, applicationContext, notificationManager!!) }

        val tcpHost = findViewById<EditText>(R.id.tcp_input)
        tcpHost.setText("10.20.30.101")

        val tcpPort = findViewById<EditText>(R.id.port_input)
        tcpPort.setText("9100")
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer.stopServer(applicationContext, this, notificationManager!!)
        notificationManager!!.cancel(SERVER_NOTIFICATION_ID)
        notificationManager!!.cancel(PRINTER_NOTIFICATION_ID)
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