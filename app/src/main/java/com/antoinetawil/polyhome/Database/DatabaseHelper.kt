package com.antoinetawil.polyhome.Database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.antoinetawil.polyhome.Models.Schedule

class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "polyhome.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_SCHEDULES = "schedules"

        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_PERIPHERAL_ID = "peripheral_id"
        private const val COLUMN_PERIPHERAL_TYPE = "peripheral_type"
        private const val COLUMN_COMMAND = "command"
        private const val COLUMN_HOUSE_ID = "house_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable =
                """
            CREATE TABLE $TABLE_SCHEDULES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE_TIME TEXT NOT NULL,
                $COLUMN_PERIPHERAL_ID TEXT NOT NULL,
                $COLUMN_PERIPHERAL_TYPE TEXT NOT NULL,
                $COLUMN_COMMAND TEXT NOT NULL,
                $COLUMN_HOUSE_ID INTEGER NOT NULL
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCHEDULES")
        onCreate(db)
    }

    fun insertSchedule(schedule: Schedule): Long {
        val db = this.writableDatabase
        val values =
                ContentValues().apply {
                    put(COLUMN_DATE_TIME, schedule.dateTime)
                    put(COLUMN_PERIPHERAL_ID, schedule.peripheralId)
                    put(COLUMN_PERIPHERAL_TYPE, schedule.peripheralType)
                    put(COLUMN_COMMAND, schedule.command)
                    put(COLUMN_HOUSE_ID, schedule.houseId)
                }
        return db.insert(TABLE_SCHEDULES, null, values)
    }

    fun getAllSchedules(): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        val db = this.readableDatabase
        val cursor =
                db.query(TABLE_SCHEDULES, null, null, null, null, null, "$COLUMN_DATE_TIME ASC")

        with(cursor) {
            while (moveToNext()) {
                val schedule =
                        Schedule(
                                id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                                dateTime = getString(getColumnIndexOrThrow(COLUMN_DATE_TIME)),
                                peripheralId =
                                        getString(getColumnIndexOrThrow(COLUMN_PERIPHERAL_ID)),
                                peripheralType =
                                        getString(getColumnIndexOrThrow(COLUMN_PERIPHERAL_TYPE)),
                                command = getString(getColumnIndexOrThrow(COLUMN_COMMAND)),
                                houseId = getInt(getColumnIndexOrThrow(COLUMN_HOUSE_ID))
                        )
                schedules.add(schedule)
            }
        }
        cursor.close()
        return schedules
    }

    fun deleteSchedule(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_SCHEDULES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }
}
