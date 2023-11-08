package com.lunapos.kioskprinter.singletons

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


object SharedPrefsManager {

    private lateinit var prefs: SharedPreferences
    private lateinit var objectMapper: ObjectMapper

    private const val PREFS_NAME = "default"

    private const val PRINTER_LIST_KEY = "printer_list"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        objectMapper = jacksonObjectMapper()
    }

    private fun readAsMutableSet(): MutableSet<String> {
        val stored = prefs.getStringSet(PRINTER_LIST_KEY, mutableSetOf())

        return stored!!
    }

    private fun readAsMutableList(): MutableList<String> {
        val stored = prefs.getStringSet(PRINTER_LIST_KEY,  mutableSetOf())

        if (stored!!.isNotEmpty()) return stored.toMutableList()

        return mutableListOf()
    }

    fun readAsPrinterData(context: Context, appCompatActivity: AppCompatActivity): MutableList<PrinterData> {
        val storedSet = readAsMutableList()
        val printers = mutableListOf<PrinterData>()

        storedSet.forEach { item ->
            val printer = readFromJSON(item)
            printer.retrieveDeviceConnection(context, appCompatActivity)

            printers.add(printer)
        }

        return printers
    }

    fun writeToList(printer: PrinterData): String {
        var result = ""
        val storedSet = prefs.getStringSet(PRINTER_LIST_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        val prefsEditor: SharedPreferences.Editor = prefs.edit()
        with(prefsEditor) {
            val converted = writeAsJSON(printer)
            storedSet.add(converted)
            putStringSet(PRINTER_LIST_KEY, storedSet)
            apply()

            result = converted
        }

        return result
    }

    fun getListCount(): Int {
        val data = prefs.getStringSet(PRINTER_LIST_KEY, emptySet())

        if (data!!.isNotEmpty()) return data.size + 1

        return 1
    }

    fun removeListAt(position: Int) {
        val originalSet = prefs.getStringSet(PRINTER_LIST_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()

        val elementToRemove = originalSet.elementAt(position)
        originalSet.remove(elementToRemove)

        val editor = prefs.edit()
        editor.putStringSet(PRINTER_LIST_KEY, originalSet)
        editor.apply()
    }

    fun updateListAt(position: Int, data: PrinterData) {
        data.id = position

        val originalSet = prefs.getStringSet(PRINTER_LIST_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()

        val elementToRemove = originalSet.elementAt(position)
        originalSet.remove(elementToRemove)

        val converted = writeAsJSON(data)
        originalSet.add(converted)

        val editor = prefs.edit()
        editor.putStringSet(PRINTER_LIST_KEY, originalSet)
        editor.apply()
    }

    fun writeAsJSON(data: PrinterData): String {
        return objectMapper.writeValueAsString(data)
    }

    fun readFromJSON(data: String): PrinterData {
        return objectMapper.readValue(data, PrinterData::class.java)
    }
}