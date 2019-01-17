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

package com.balicodes.quicksms.data.model

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

data class Recipient(var id: Long, var name: String, var number: String) : Parcelable {

    var sent = false
    var sending = false

    // more verbose version of getSent() and getSending()
    val isSent: Boolean = sent
    val isSending: Boolean = sending

    fun toBundle(): Bundle{
        val bundle = Bundle()
        bundle.putLong("id", id)
        bundle.putString("name", name)
        bundle.putString("number", number)
        return bundle
    }

    // Parcelable methods
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeLong(id)
        dest?.writeString(name)
        dest?.writeString(number)
    }

    constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readString(), parcel.readString())

    // Static properties and methods
    companion object {
        @JvmField final val CREATOR: Parcelable.Creator<Recipient> = object: Parcelable.Creator<Recipient>{
            override fun createFromParcel(parcel: Parcel): Recipient = Recipient(parcel)
            override fun newArray(size: Int): Array<Recipient?> = newArray(size)
        }

        @JvmStatic fun fromBundle(bundle: Bundle) = Recipient(bundle.getLong("id"), bundle.getString("name"), bundle.getString("number"))
    }
}