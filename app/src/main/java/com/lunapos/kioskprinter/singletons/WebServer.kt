package com.lunapos.kioskprinter.singletons

import android.content.Context
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.lunapos.kioskprinter.R
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class WebServer private constructor() {
    companion object {

        @Volatile
        private var instance: WebServer? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: WebServer().also { instance = it }
            }
    }

    private var mHttpServer: HttpServer? = null

    private val notifications = Notifications.getInstance()

    fun startServer(port: Int, appCompatActivity: AppCompatActivity, context: Context, notificationManager: NotificationManagerCompat) = try {
        mHttpServer = HttpServer.create(InetSocketAddress(port), 0)
        mHttpServer?.executor = Executors.newCachedThreadPool()
        mHttpServer?.createContext("/", rootHandler)

        mHttpServer?.start() // Start the server

        val serverAddress = mHttpServer?.address
        if (serverAddress != null) {
            println("Server is running on $serverAddress:$port")

            val serverButton: Button = appCompatActivity.findViewById(R.id.serverButton)
            serverButton.text = "Stop Server"

            notifications.showStickyNotification(context, notificationManager, SERVER_CHANNEL_ID,
                SERVER_CHANNEL_NAME, SERVER_NOTIFICATION_ID, "Server", "Server is running")
        } else {
            println("Failed to determine server address")
        }
    } catch (e: IOException) {
        e.printStackTrace()
        // Handle the exception more gracefully if needed
    }

    fun stopServer(context: Context, appCompatActivity: AppCompatActivity, notificationManager: NotificationManagerCompat) {
        mHttpServer?.stop(0)
        println("Server is stop running")
        val serverButton: Button = appCompatActivity.findViewById(R.id.serverButton)
        serverButton.text = "Start Server"

        notifications.stopStickyNotification(context, notificationManager, SERVER_CHANNEL_ID,
            SERVER_NOTIFICATION_ID, "Server", "Server is stop running")
    }

    private fun sendResponse(httpExchange: HttpExchange, responseText: String){
        httpExchange.sendResponseHeaders(200, responseText.length.toLong())
        val os = httpExchange.responseBody
        os.write(responseText.toByteArray())
        os.close()
    }

    private fun handleCorsHeaders(t: HttpExchange) {
        val responseHeaders = t.responseHeaders
        responseHeaders.add("Access-Control-Allow-Origin", "*")
        responseHeaders.add("Access-Control-Allow-Methods", "GET, POST")
        responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
    }

    private fun handleOptionsRequest(t: HttpExchange) {
        handleCorsHeaders(t)
        t.sendResponseHeaders(204, -1)
        t.close()
    }

    // Handler for root endpoint
    private val rootHandler = HttpHandler { exchange : HttpExchange ->
        // Get request method
        when (exchange.requestMethod) {
            "OPTIONS" -> {
                handleOptionsRequest(exchange)
                return@HttpHandler
            }
            "GET" -> {
//                printBluetooth()
//                printTcp()
                sendResponse(exchange, "Welcome to my server")
            }
            "POST" -> {
                handleCorsHeaders(exchange)
                sendResponse(exchange, "POST is working!")
//                try {
//                    // Read the POST request body
//                    val isr = InputStreamReader(exchange.requestBody, "UTF-8")
//                    val br = BufferedReader(isr)
//                    val requestBody = StringBuilder()
//                    var line: String?
//                    while (br.readLine().also { line = it } != null) {
//                        requestBody.append(line)
//                    }
//                    br.close()
//
//                    // Parse the JSON data from the request body
//                    val json = requestBody.toString()
//
//                    // Now 'json' contains the JSON data as a string. You can parse it further.
//                    // For example, you can use a JSON library like Jackson to deserialize it into an object.
//
//                    // Example using Jackson:
//                    val objectMapper = jacksonObjectMapper()
//                    val printerData = objectMapper.readValue<printerBody>(json)
//
//                    // Now 'yourObject' contains the parsed JSON data as an object.
//                    printBluetooth(printerData.data)
//
//                    // Respond to the request
//                    val response = "Received and processed JSON data"
//                    exchange.sendResponseHeaders(200, response.length.toLong())
//                    exchange.responseBody.use { os -> os.write(response.toByteArray()) }
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                    // Handle exceptions appropriately
//                }
            }
            else -> {
                // Handle other request methods or provide a response for unsupported methods
                sendResponse(exchange, "Unsupported HTTP method")
            }
        }
    }
}