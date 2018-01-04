package com.balicodes.quicksms;

import android.os.Bundle
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by eka on 7/3/16.
 * A Parcelable Recipient class
 */
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

        @JvmStatic fun fromBundle(bundle: Bundle) =  Recipient(bundle.getLong("id"), bundle.getString("name"), bundle.getString("number"))
    }
}