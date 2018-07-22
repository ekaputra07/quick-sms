/*
 * Copyright (C) 2016-2018 Eka Putra
 *
 * Quick SMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.balicodes.quicksms;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

@Deprecated
class DBHelper extends SQLiteOpenHelper {
    private final Context context;
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_NUMBER = "number";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_CONFIRM = "confirm";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "quicksms.db";
    private static final String TABLE_NAME = "sms";
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (id integer primary key, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_NUMBER + " TEXT, " +
                    COLUMN_MESSAGE + " TEXT, " +
                    COLUMN_CONFIRM + " TEXT);";

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    public int numRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
    }

    public long insert(String title, String number, String message, String confirm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_NUMBER, number);
        contentValues.put(COLUMN_MESSAGE, message);
        contentValues.put(COLUMN_CONFIRM, confirm);
        return db.insert(TABLE_NAME, null, contentValues);
    }

    public long update(long id, String title, String number, String message, String confirm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_NUMBER, number);
        contentValues.put(COLUMN_MESSAGE, message);
        contentValues.put(COLUMN_CONFIRM, confirm);
        long updateId = db.update(TABLE_NAME, contentValues, "id = ? ", new String[]{Long.toString(id)});
        return id;
    }

    public Integer delete(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME,
                "id = ? ",
                new String[]{Long.toString(id)});
    }

    public SMSItem get(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where id=" + id + "", null);

        if (res != null && res.moveToFirst()) {
            SMSItem item = new SMSItem(
                    res.getLong(res.getColumnIndex("id")),
                    res.getString(res.getColumnIndex(COLUMN_TITLE)),
                    res.getString(res.getColumnIndex(COLUMN_NUMBER)),
                    res.getString(res.getColumnIndex(COLUMN_MESSAGE)),
                    res.getString(res.getColumnIndex(COLUMN_CONFIRM)));

            res.close();
            return item;
        }
        return null;
    }

    public List<SMSItem> all() {
        List<SMSItem> array_list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String sort_by = sp.getString(context.getString(R.string.pref_sort_by_key),
                context.getString(R.string.pref_sort_by_default_value));

        Cursor res;
        res = db.rawQuery("select * from " + TABLE_NAME + " " + sort_by, null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            Log.d("ALL", "ALL");
            SMSItem item = new SMSItem(
                    res.getInt(res.getColumnIndex("id")),
                    res.getString(res.getColumnIndex(COLUMN_TITLE)),
                    res.getString(res.getColumnIndex(COLUMN_NUMBER)),
                    res.getString(res.getColumnIndex(COLUMN_MESSAGE)),
                    res.getString(res.getColumnIndex(COLUMN_CONFIRM)));
            array_list.add(item);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }
}