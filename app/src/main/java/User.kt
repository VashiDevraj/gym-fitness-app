package com.example.gymapplication

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var uid: String = "",
    var username: String = "User",
    var email: String = "",
    var accountType: String = "user",
    var basicInfo: BasicInfo? = null,
    var dietPlan: DietPlan? = null,
    var fitnessGoal: String = "maintain", // "lose", "gain", "maintain"
    var checkins: List<CheckIn> = emptyList()
) {
    companion object {
        fun fromSnapshot(snap: DataSnapshot): User? {
            val username    = snap.child("username").getStringOrDefault("User")
            val accountType = snap.child("accountType").getStringOrDefault("user")
            val fitnessGoal = snap.child("fitnessGoal").getStringOrDefault("maintain")

            val biSnap = snap.child("basicInfo")
            val basicInfo = if (biSnap.exists()) BasicInfo(
                age    = biSnap.child("age").getIntOrDefault(0),
                weight = biSnap.child("weight").getDoubleOrDefault(0.0),
                height = biSnap.child("height").getDoubleOrDefault(0.0),
                bmi    = biSnap.child("bmi").getDoubleOrDefault(0.0)
            ) else null

            val dietPlan = DietPlan.fromAny(snap.child("dietPlan").value)

            return User(
                uid         = snap.key ?: "",
                username    = username,
                accountType = accountType,
                basicInfo   = basicInfo,
                dietPlan    = dietPlan,
                fitnessGoal = fitnessGoal
            )
        }
    }
}