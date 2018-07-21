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

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.balicodes.quicksms.entity.MessageEntity
import com.balicodes.quicksms.viewmodel.MessageViewModel
import kotlinx.android.synthetic.main.activity_export_import.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.sql.Timestamp

class ExportImportActivity : AppCompatActivity() {

    private var viewModel: MessageViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_import)

        viewModel = ViewModelProviders.of(this).get(MessageViewModel::class.java)

        btnExport.setOnClickListener {
            Log.d(javaClass.simpleName, "EXPORT")

            // Android API 23+ requires this
            val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permission == PackageManager.PERMISSION_GRANTED) {
                exportAsync()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), Config.WRITE_STORAGE_PERMISSION_REQUEST)
            }
        }

        btnImport.setOnClickListener {
            Log.d(javaClass.simpleName, "IMPORT")

            val pickIntent = Intent(Intent.ACTION_GET_CONTENT)
            pickIntent.type = "text/*"
            pickIntent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(pickIntent, Config.PICK_FILE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Config.PICK_FILE_REQUEST
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.data != null) {
            Log.d(javaClass.simpleName, data.dataString)
            importAsync(data.data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == Config.WRITE_STORAGE_PERMISSION_REQUEST && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportAsync()
            } else {
                Toast.makeText(this, getString(R.string.permission_export_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportAsync() {
        btnExport.text = getString(R.string.exporting)

        viewModel!!.setOrderBy("id")
        viewModel!!.getMessages().observe(this, Observer {

            doAsync {
                val success = export(it)
                uiThread {
                    if (success) {
                        Toast.makeText(it, getString(R.string.export_success), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(it, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
                    }
                    btnExport.text = getString(R.string.export)
                }
            }

        })
    }

    /**
     * http://androidtechnicalblog.blogspot.co.id/2014/01/exporting-sqlite-database-into-csv.html
     */
    private fun export(list: List<MessageEntity>?): Boolean {
        list?.let {
            /**First of all we check if the external storage of the device is available for writing.
             * Remember that the external storage is not necessarily the sd card. Very often it is
             * the device storage.
             */
            val state: String = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED != state) {
                return false
            } else {
                //We use the Download directory for saving our .csv file.
                val exportDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                val file: File? = File(exportDir, "QuickSMS-${Timestamp(System.currentTimeMillis())}.csv")
                var printWriter: PrintWriter? = null

                try {
                    file!!.createNewFile()
                    printWriter = PrintWriter(FileWriter(file))

                    for (item in it) {
                        val row = "${item.id}|${item.title}|${item.number}|${item.message}"
                        Log.d(javaClass.simpleName, row)
                        printWriter.println(row)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                } finally {
                    if (printWriter != null) {
                        printWriter.close()
                    }
                }
                return true
            }
        }

        return false
    }

    private fun importAsync(uri: Uri?) {
        btnImport.text = getString(R.string.importing)

        doAsync {
            val result = import(uri)

            uiThread {
                if (result == "success") {
                    Toast.makeText(it, getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(it, result, Toast.LENGTH_LONG).show()
                }
                btnImport.text = getString(R.string.import_str)
            }
        }
    }

    private fun import(uri: Uri?): String {
        try {
            val csvReader = CSVReader(this, uri)
            val messages: List<Array<String>> = csvReader.readCSV()
            val entities = arrayListOf<MessageEntity>()

            // write to DB
            for (index in messages.indices) {
                val m = messages[index]
                val e = MessageEntity(null, m[1], m[2], m[3], SMSItem.SHORTCUT_NO)
                entities.add(index, e)
            }

            viewModel!!.insertMessages(*entities.toTypedArray())

        } catch (io: IOException) {
            io.printStackTrace()
            return getString(R.string.import_error_io)
        } catch (e: Exception) {
            e.printStackTrace()
            return getString(R.string.import_error_format)
        }
        return "success"
    }
}