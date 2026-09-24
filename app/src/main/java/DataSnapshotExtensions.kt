package com.example.gymapplication

import com.google.firebase.database.DataSnapshot

fun DataSnapshot.getStringOrDefault(default: String = ""): String {
    val v = value
    return when (v) {
        is String -> v
        is Number -> v.toString()
        is Boolean -> v.toString()
        else -> default
    }
}

fun DataSnapshot.getIntOrDefault(default: Int = 0): Int {
    val v = value
    return when (v) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: default
        else -> default
    }
}

fun DataSnapshot.getDoubleOrDefault(default: Double = 0.0): Double {
    val v = value
    return when (v) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: default
        else -> default
    }
}
