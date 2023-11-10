package com.lunapos.kioskprinter.PrinterUtil

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lunapos.kioskprinter.dtos.PrinterEntity
import com.lunapos.kioskprinter.dtos.PrinterEntityDAO


@Database(entities = [PrinterEntity::class], version = 1)
abstract class PrinterEntityDatabase : RoomDatabase() {
    abstract fun printerDao(): PrinterEntityDAO

    companion object {
        @Volatile
        private var INSTANCE: PrinterEntityDatabase? = null

        fun getInstance(context: Context): PrinterEntityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrinterEntityDatabase::class.java,
                    "Printers.db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }
}