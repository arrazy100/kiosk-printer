package com.lunapos.kioskprinter

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.lunapos.kioskprinter.singletons.BluetoothPrinter
import com.lunapos.kioskprinter.singletons.CoroutinePrinter
import com.lunapos.kioskprinter.singletons.Notifications
import com.lunapos.kioskprinter.singletons.PRINTER_CHANNEL_ID
import com.lunapos.kioskprinter.singletons.PRINTER_CHANNEL_NAME
import com.lunapos.kioskprinter.singletons.PRINTER_NOTIFICATION_ID
import com.lunapos.kioskprinter.singletons.SERVER_NOTIFICATION_ID
import com.lunapos.kioskprinter.singletons.WebServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private var serverUp = false
    private var notificationManager: NotificationManagerCompat? = null

    private val webServer = WebServer.getInstance()
    private val notifications = Notifications.getInstance()
    private val bluetoothPrinter = BluetoothPrinter()
    private val coroutinePrinter = CoroutinePrinter.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val port = 5000
        notificationManager = NotificationManagerCompat.from(applicationContext)

        for(i in 1..10) {
            val printer = BluetoothPrinter()
            printer.text = "Printer $i"

            coroutinePrinter.addPrinter(printer)
        }

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

    private fun doTask(context: Context, notificationManager : NotificationManagerCompat) {
        notifications.showProgressNotification(context, notificationManager, PRINTER_CHANNEL_ID,
            PRINTER_CHANNEL_NAME, PRINTER_NOTIFICATION_ID, "Printer", "Printer Progress")

        CoroutineScope(Dispatchers.Default).launch {
            // Simulated task with progress updates
            for (progress in 0..100 step 10) {
                // Update the progress on the notification
                notifications.updateProgressNotification(context, notificationManager, progress)
            }

            notificationManager.cancel(PRINTER_NOTIFICATION_ID)
        }

    }

    private fun printTcp() {
        Log.i("Print", "Print Tcp started")

        val ipAddress = findViewById<View>(R.id.tcp_input) as EditText
        val portAddress = findViewById<View>(R.id.port_input) as EditText

        try {
            Log.i("Print", "Print Tcp started")

            Thread {
                try {
                    val printer =
                        EscPosPrinter(TcpConnection(ipAddress.text.toString(), portAddress.text.toString().toInt(), 1000), 203, 80f, 32)
                    printer
                        .printFormattedTextAndCut(
                                    "[L]\n" +
                                    "[C]<u><font size='big'>ORDER N°045</font></u>\n" +
                                    "[L]\n" +
                                    "[C]================================\n" +
                                    "[L]\n" +
                                    "[L]<b>BEAUTIFUL SHIRT</b>[R]9.99e\n" +
                                    "[L]  + Size : S\n" +
                                    "[L]\n" +
                                    "[L]<b>AWESOME HAT</b>[R]24.99e\n" +
                                    "[L]  + Size : 57/58\n" +
                                    "[L]\n" +
                                    "[C]--------------------------------\n" +
                                    "[R]TOTAL PRICE :[R]34.98e\n" +
                                    "[R]TAX :[R]4.23e\n" +
                                    "[L]\n" +
                                    "[C]================================\n" +
                                    "[L]\n" +
                                    "[L]<font size='tall'>Customer :</font>\n" +
                                    "[L]Raymond DUPONT\n" +
                                    "[L]5 rue des giraffes\n" +
                                    "[L]31547 PERPETES\n" +
                                    "[L]Tel : +33801201456\n" +
                                    "[L]\n" +
                                    "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
                                    "[C]<qrcode size='20'>https://dantsu.com/</qrcode>"
                        )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            Log.i("Print", "Print Tcp Finished")
        } catch (e : NumberFormatException) {
            AlertDialog.Builder(this)
                .setTitle("Invalid TCP port address")
                .setMessage("Port field must be an integer.")
                .show()
            e.printStackTrace()
        }
    }
}