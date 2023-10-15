package com.lunapos.kioskprinter.singletons

import android.content.Context
import android.widget.Button
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoroutinePrinter {
    companion object {

        @Volatile
        private var instance: CoroutinePrinter? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: CoroutinePrinter().also { instance = it }
            }
    }

    val printers = mutableListOf<AbstractPrinter>()
    var printing = false

    fun addPrinter(printer : AbstractPrinter) {
        printers.add(printer)
    }

    private fun calculateProgress(arraySize: Int): MutableList<Int> {
        val progressArray = mutableListOf<Int>()

        for (i in 1..arraySize) {
            val progress = (i * 100) / arraySize
            progressArray.add(progress)
        }

        return progressArray
    }

    fun doPrint(
        notifications: Notifications,
        context: Context,
        notificationManager: NotificationManagerCompat,
        printButton: Button?
    ) {
        if (printing) {
            return
        }

        if (printers.isEmpty()) {
            return
        }

        // Disable the Print
        printing = true

        // Disable Button
        printButton!!.isEnabled = false

        notifications.showProgressNotification(context, notificationManager, PRINTER_CHANNEL_ID,
            PRINTER_CHANNEL_NAME, PRINTER_NOTIFICATION_ID, "Printer", "Print Progress")

        val progressArray = calculateProgress(printers.size)

        CoroutineScope(Dispatchers.IO).launch {
            val jobs = mutableListOf<Job>()

            try {
                for ((index, printer) in printers.withIndex()) {
                    val job = launch {
                        val progress = progressArray[index]

                        withContext(Dispatchers.Main) {
                            notifications.updateProgressNotification(
                                context,
                                notificationManager,
                                progress
                            )
                        }

                        // Perform printing tasks for the specific printer (on the background thread)
                        printer.print(context)
                    }
                    jobs.add(job)
                    job.join()
                }

                // Wait for all coroutines to finish
                jobs.forEach { it.join() }

                // Cancel the notification (on the main thread)
                withContext(Dispatchers.Main) {
                    notificationManager.cancel(PRINTER_NOTIFICATION_ID)

                    // Enable the button
                    printButton.isEnabled = true

                    // Enable the print
                    printing = false
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    printButton.isEnabled = true
                }
            }
            finally {
                printing = false
            }
        }
    }

    fun doPrintOnServer(
        notifications: Notifications,
        context: Context,
        notificationManager: NotificationManagerCompat,
    ) {
        if (printing) {
            return
        }

        if (printers.isEmpty()) {
            return
        }

        // Disable the Print
        printing = true

        notifications.showProgressNotification(context, notificationManager, PRINTER_CHANNEL_ID,
            PRINTER_CHANNEL_NAME, PRINTER_NOTIFICATION_ID, "Printer", "Print Progress")

        val progressArray = calculateProgress(printers.size)

        CoroutineScope(Dispatchers.IO).launch {
            val jobs = mutableListOf<Job>()

            try {
                for ((index, printer) in printers.withIndex()) {
                    val job = launch {
                        val progress = progressArray[index]

                        withContext(Dispatchers.Main) {
                            notifications.updateProgressNotification(
                                context,
                                notificationManager,
                                progress
                            )
                        }

                        // Perform printing tasks for the specific printer (on the background thread)
                        printer.print(context)
                    }
                    jobs.add(job)
                    job.join()
                }

                // Wait for all coroutines to finish
                jobs.forEach { it.join() }

                // Cancel the notification (on the main thread)
                withContext(Dispatchers.Main) {
                    notificationManager.cancel(PRINTER_NOTIFICATION_ID)

                    // Enable the print
                    printing = false
                }
            }
            catch (_: Exception) {
            }
            finally {
                printing = false
            }
        }
    }
}