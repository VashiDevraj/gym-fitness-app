package com.example.gymapplication

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DietPlan(
    var calories: Int = 0,
    var protein: Int = 0,
    var carbs: Int = 0,
    var fats: Int = 0,
    var notes: String = "",
    // Meal breakdown
    var breakfast: String = "",
    var lunch: String = "",
    var dinner: String = "",
    var snacks: String = "",
    // Meta
    var assignedGoal: String = "maintain", // mirrors user's goal when plan was assigned
    var lastUpdated: Long = 0L
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromAny(value: Any?): DietPlan? {
            if (value !is Map<*, *>) return null
            return DietPlan(
                calories     = (value["calories"] as? Long)?.toInt() ?: 0,
                protein      = (value["protein"] as? Long)?.toInt() ?: 0,
                carbs        = (value["carbs"] as? Long)?.toInt() ?: 0,
                fats         = (value["fats"] as? Long)?.toInt() ?: 0,
                notes        = value["notes"] as? String ?: "",
                breakfast    = value["breakfast"] as? String ?: "",
                lunch        = value["lunch"] as? String ?: "",
                dinner       = value["dinner"] as? String ?: "",
                snacks       = value["snacks"] as? String ?: "",
                assignedGoal = value["assignedGoal"] as? String ?: "maintain",
                lastUpdated  = value["lastUpdated"] as? Long ?: 0L
            )
        }
    }
}