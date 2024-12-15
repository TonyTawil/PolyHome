package com.antoinetawil.polyhome.Database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.antoinetawil.polyhome.Models.Schedule
import com.antoinetawil.polyhome.Models.ScheduleCommand
import org.json.JSONArray
import org.json.JSONObject

class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "polyhome.db"
        private const val DATABASE_VERSION = 3
        private const val TABLE_SCHEDULES = "schedules"

        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_HOUSE_ID = "house_id"
        private const val COLUMN_COMMANDS = "commands"
        private const val COLUMN_RECURRING_DAYS = "recurring_days"
        private const val COLUMN_IS_ENABLED = "is_enabled"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable =
                """
            CREATE TABLE $TABLE_SCHEDULES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE_TIME TEXT NOT NULL,
                $COLUMN_HOUSE_ID INTEGER NOT NULL,
                $COLUMN_COMMANDS TEXT NOT NULL,
                $COLUMN_RECURRING_DAYS TEXT NOT NULL,
                $COLUMN_IS_ENABLED INTEGER DEFAULT 1
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                    "ALTER TABLE $TABLE_SCHEDULES ADD COLUMN $COLUMN_RECURRING_DAYS TEXT DEFAULT ''"
            )
        }
        if (oldVersion < 3) {
            db.execSQL(
                    "ALTER TABLE $TABLE_SCHEDULES ADD COLUMN $COLUMN_IS_ENABLED INTEGER DEFAULT 1"
            )
        }
    }

    fun insertSchedule(schedule: Schedule): Long {
        val db = this.writableDatabase
        val values =
                ContentValues().apply {
                    put(COLUMN_DATE_TIME, schedule.dateTime)
                    put(COLUMN_HOUSE_ID, schedule.houseId)
                    put(COLUMN_COMMANDS, commandsToJson(schedule.commands))
                    put(COLUMN_RECURRING_DAYS, schedule.recurringDays.joinToString(","))
                    put(COLUMN_IS_ENABLED, if (schedule.isEnabled) 1 else 0)
                }
        return db.insert(TABLE_SCHEDULES, null, values)
    }

    fun getAllSchedules(): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_SCHEDULES, null, null, null, null, null, null)

        with(cursor) {
            while (moveToNext()) {
                val schedule =
                        Schedule(
                                id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                                dateTime = getString(getColumnIndexOrThrow(COLUMN_DATE_TIME)),
                                houseId = getInt(getColumnIndexOrThrow(COLUMN_HOUSE_ID)),
                                commands =
                                        jsonToCommands(
                                                getString(getColumnIndexOrThrow(COLUMN_COMMANDS))
                                        ),
                                recurringDays =
                                        try {
                                            getString(getColumnIndexOrThrow(COLUMN_RECURRING_DAYS))
                                                    .split(",")
                                                    .filter { it.isNotEmpty() }
                                                    .map { it.toInt() }
                                        } catch (e: Exception) {
                                            emptyList()
                                        },
                                isEnabled = getInt(getColumnIndexOrThrow(COLUMN_IS_ENABLED)) == 1
                        )
                schedules.add(schedule)
            }
        }
        cursor.close()

        return schedules.sortedBy { schedule -> schedule.dateTime.split(" ")[1] }
    }

    private fun commandsToJson(commands: List<ScheduleCommand>): String {
        val jsonArray = JSONArray()
        commands.forEach { command ->
            val jsonObject =
                    JSONObject().apply {
                        put("peripheralId", command.peripheralId)
                        put("peripheralType", command.peripheralType)
                        put("command", command.command)
                    }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun jsonToCommands(json: String): List<ScheduleCommand> {
        val commands = mutableListOf<ScheduleCommand>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            commands.add(
                    ScheduleCommand(
                            peripheralId = obj.getString("peripheralId"),
                            peripheralType = obj.getString("peripheralType"),
                            command = obj.getString("command")
                    )
            )
        }
        return commands
    }

    fun deleteSchedule(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_SCHEDULES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun updateScheduleEnabled(scheduleId: Long, isEnabled: Boolean) {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_IS_ENABLED, if (isEnabled) 1 else 0) }
        db.update(TABLE_SCHEDULES, values, "$COLUMN_ID = ?", arrayOf(scheduleId.toString()))
    }

    fun getSchedule(id: Long): Schedule? {
        val db = this.readableDatabase
        val cursor =
                db.query(
                        TABLE_SCHEDULES,
                        null,
                        "$COLUMN_ID = ?",
                        arrayOf(id.toString()),
                        null,
                        null,
                        null
                )

        return cursor.use {
            if (it.moveToFirst()) {
                Schedule(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        dateTime = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE_TIME)),
                        houseId = it.getInt(it.getColumnIndexOrThrow(COLUMN_HOUSE_ID)),
                        commands =
                                jsonToCommands(
                                        it.getString(it.getColumnIndexOrThrow(COLUMN_COMMANDS))
                                ),
                        recurringDays =
                                try {
                                    it.getString(it.getColumnIndexOrThrow(COLUMN_RECURRING_DAYS))
                                            .split(",")
                                            .filter { it.isNotEmpty() }
                                            .map { it.toInt() }
                                } catch (e: Exception) {
                                    emptyList()
                                },
                        isEnabled = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_ENABLED)) == 1
                )
            } else null
        }
    }

    fun updateScheduleDateTime(scheduleId: Long, newDateTime: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_DATE_TIME, newDateTime) }
        db.update(TABLE_SCHEDULES, values, "$COLUMN_ID = ?", arrayOf(scheduleId.toString()))
    }
}
