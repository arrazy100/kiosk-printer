package com.lunapos.kioskprinter.singletons

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    fun addPrinter(printer : AbstractPrinter) {
        printers.add(printer)
    }

    fun doPrint(notifications: Notifications, context: Context, notificationManager: NotificationManagerCompat) {
        if (printers.isEmpty()) {
            return
        }

        notifications.showProgressNotification(context, notificationManager, PRINTER_CHANNEL_ID,
            PRINTER_CHANNEL_NAME, PRINTER_NOTIFICATION_ID, "Printer", "Print Progress")

        CoroutineScope(Dispatchers.IO).launch {
            val jobs = mutableListOf<Job>()

            for ((index, printer) in printers.withIndex()) {
                val job = launch {
                    val progress = ((index + 1) * 10)

                    withContext(Dispatchers.Main) {
                        notifications.updateProgressNotification(context, notificationManager, progress)
                    }

                    // Perform printing tasks for the specific printer (on the background thread)
                    printer.print()
                }
                jobs.add(job)
                job.join()
            }

            // Wait for all coroutines to finish
            jobs.forEach { it.join() }

            // Cancel the notification (on the main thread)
            withContext(Dispatchers.Main) {
                notificationManager.cancel(PRINTER_NOTIFICATION_ID)
            }
        }
    }
}