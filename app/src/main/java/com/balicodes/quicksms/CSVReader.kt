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

package com.balicodes.quicksms

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class CSVReader(internal var context: Context, private var uri: Uri?) {

    private var rows: MutableList<Array<String>> = ArrayList()

    @Throws(IOException::class)
    fun readCSV(): MutableList<Array<String>> {
        val csvFile = context.contentResolver.openInputStream(uri)
        val isr = InputStreamReader(csvFile)
        val lines: List<String> = BufferedReader(isr).readLines()
        val csvSplitBy = "|"

        for (line in lines) {
            val row: Array<String> = line.split(csvSplitBy).toTypedArray()
            Log.d(javaClass.simpleName, "${row[0]} | ${row[1]} | ${row[2]} | ${row[3]}")
            rows.add(row)
        }
        return rows
    }
}