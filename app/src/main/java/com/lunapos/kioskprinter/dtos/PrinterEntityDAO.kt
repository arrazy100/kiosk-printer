package com.lunapos.kioskprinter.dtos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PrinterEntityDAO {
    @Insert
    fun insert(printer: PrinterEntity): Long

    @Update
    fun update(printer: PrinterEntity)

    @Query("DELETE FROM printer WHERE id = :id")
    fun delete(id: Int)

    @Query("SELECT * FROM printer")
    fun getAll(): LiveData<List<PrinterEntity>>

    @Query("SELECT * FROM printer")
    fun getAllNonLive(): List<PrinterEntity>

}