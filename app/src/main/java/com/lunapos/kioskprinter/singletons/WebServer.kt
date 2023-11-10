package com.lunapos.kioskprinter.singletons

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lunapos.kioskprinter.Constants.SERVER_CHANNEL_ID
import com.lunapos.kioskprinter.Constants.SERVER_CHANNEL_NAME
import com.lunapos.kioskprinter.Constants.SERVER_NOTIFICATION_ID
import com.lunapos.kioskprinter.dtos.PrinterBody
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
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

    var coroutinePrinter: CoroutinePrinter? = null
    private lateinit var context: WeakReference<Context>
    private var notificationManager: NotificationManagerCompat? = null
    private var mHttpServer: HttpServer? = null
    private val notifications = Notifications.getInstance()

    fun startServer(port: Int, context: Context, notificationManager: NotificationManagerCompat) = try {
        this.context = WeakReference(context)
        this.notificationManager = notificationManager

        mHttpServer = HttpServer.create(InetSocketAddress(port), 0)
        mHttpServer?.executor = Executors.newCachedThreadPool()
        mHttpServer?.createContext("/", rootHandler)

        mHttpServer?.start() // Start the server

        val serverAddress = mHttpServer?.address
        if (serverAddress != null) {
            println("Server is running on $serverAddress:$port")

            notifications.showStickyNotification(context, notificationManager, SERVER_CHANNEL_ID,
                SERVER_CHANNEL_NAME, SERVER_NOTIFICATION_ID, "Server", "Server is running")
        } else {
            println("Failed to determine server address")
        }
    } catch (e: IOException) {
        e.printStackTrace()
        // Handle the exception more gracefully if needed
    }

    fun stopServer(context: Context, notificationManager: NotificationManagerCompat) {
        mHttpServer?.stop(0)
        println("Server is stop running")

        notifications.stopStickyNotification(context, notificationManager, SERVER_CHANNEL_ID,
            SERVER_NOTIFICATION_ID, "Server", "Server is stop running")
    }

    private fun sendResponse(httpExchange: HttpExchange, responseText: String){
        httpExchange.sendResponseHeaders(200, responseText.length.toLong())
        val os = httpExchange.responseBody
        os.write(responseText.toByteArray())
        os.close()
    }

    private fun handleCorsHeaders(exchange: HttpExchange) {
        val responseHeaders = exchange.responseHeaders
        responseHeaders.add("Access-Control-Allow-Origin", "*")
        responseHeaders.add("Access-Control-Allow-Methods", "GET, POST")
        responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
    }

    private fun handleOptionsRequest(exchange: HttpExchange) {
        handleCorsHeaders(exchange)
        exchange.sendResponseHeaders(204, -1)
        exchange.close()
    }

    private inline fun <reified T> parseBody(exchange: HttpExchange): T {
        // Read the POST request body
        val isr = InputStreamReader(exchange.requestBody, "UTF-8")
        val br = BufferedReader(isr)
        val requestBody = StringBuilder()
        var line: String?
        while (br.readLine().also { line = it } != null) {
            requestBody.append(line)
        }
        br.close()

        // Parse the JSON data from the request body
        val json = requestBody.toString()

        // Parse as object DTO
        val objectMapper = jacksonObjectMapper()

        return objectMapper.readValue(json)
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
                exchange.responseHeaders.set("Content-Type", "application/json")

                var characterNbrPerLines = mutableListOf<Int>()
                coroutinePrinter!!.printers!!.forEach {
                    characterNbrPerLines.add(it.printerNbrCharactersPerLine)
                }

                val responseMap = mapOf(
                    "data" to characterNbrPerLines
                )

                val response = jacksonObjectMapper().writeValueAsString(responseMap)

                sendResponse(exchange, response)
                return@HttpHandler
            }
            "POST" -> {
                var body = PrinterBody()

                handleCorsHeaders(exchange)
                try {
                    if (coroutinePrinter!!.printing) {
                        sendResponse(exchange, "Still printing in background")
                    }

                    val context = this.context.get()
                    body = parseBody(exchange)

                    if (context != null) {
                        for (print in coroutinePrinter!!.printers!!) {
                            print.text = body.message
                        }
                        coroutinePrinter?.doPrintOnServer(
                            notifications,
                            context,
                            notificationManager!!
                        )
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    sendResponse(exchange, "Failed with exception")
                } finally {
                    sendResponse(exchange, "Print on progress")
                }

                sendResponse(exchange, body.message)
            }
            else -> {
                // Handle other request methods or provide a response for unsupported methods
                sendResponse(exchange, "Unsupported HTTP method")
            }
        }
    }
}