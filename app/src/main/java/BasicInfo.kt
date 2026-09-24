package com.example.gymapplication

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class BasicInfo(
    var age: Int = 0,
    var weight: Double = 0.0,
    var height: Double = 0.0,
    var bmi: Double = 0.0
) {
    constructor() : this(0, 0.0, 0.0, 0.0)

    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "age" to age,
            "weight" to weight,
            "height" to height,
            "bmi" to bmi
        )
    }
}
