package com.example.gymapplication

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class CheckIn(
    var date: String = "",
    var time: String = "",
    var notes: String = ""
) {
    constructor() : this("", "", "")

    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "date" to date,
            "time" to time,
            "notes" to notes
        )
    }
}
