package com.lunapos.kioskprinter.singletons

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson


object SharedPrefsManager {

    private lateinit var prefs: SharedPreferences

    private const val PREFS_NAME = "default"

    private const val PRINTER_LIST_KEY = "printer_list"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        val gson = Gson()
        val storedSet = readAsMutableList()
        val printers = mutableListOf<PrinterData>()

        storedSet.forEach { item ->
            val printer = gson.fromJson(item, PrinterData::class.java)
            printer.retrieveDeviceConnection(context, appCompatActivity)

            printers.add(printer)
        }

        printers.sortBy { printerData -> printerData.id }

        return printers
    }

    fun writeToList(printer: PrinterData): String {
        var result = ""
        val storedSet = prefs.getStringSet(PRINTER_LIST_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        val prefsEditor: SharedPreferences.Editor = prefs.edit()
        with(prefsEditor) {
            val gson = Gson()
            val converted = gson.toJson(printer)
            storedSet.add(converted)
            putStringSet(PRINTER_LIST_KEY, storedSet)
            commit()

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

        val gson = Gson()
        val converted = gson.toJson(data)
        originalSet.add(converted)

        val editor = prefs.edit()
        editor.putStringSet(PRINTER_LIST_KEY, originalSet)
        editor.apply()
    }
}