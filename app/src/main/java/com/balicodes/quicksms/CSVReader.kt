package com.balicodes.quicksms

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by ekaputra on 12/26/17.
 * http://en.proft.me/2017/07/6/how-read-csv-file-android/
 */
class CSVReader(internal var context: Context, internal var uri: Uri?) {

    internal var rows: MutableList<Array<String>> = ArrayList<Array<String>>()

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