package com.example.gymapplication

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Equipment(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var category: String = "",       // "Cardio" | "Strength" | "Flexibility"
    var status: String = "Working",  // "Working" | "Needs Repair" | "Out of Order"
    var location: String = "",       // e.g. "Ground Floor", "First Floor"
    var reportCount: Int = 0,
    var lastMaintenanceDate: String = "",
    var addedDate: String = ""
) {
    constructor() : this("","","","","","Working","",0,"","")
}

@IgnoreExtraProperties
data class EquipmentReport(
    var id: String = "",
    var equipmentId: String = "",
    var equipmentName: String = "",
    var userId: String = "",
    var username: String = "",
    var issue: String = "",
    var severity: String = "Medium",  // "Low" | "Medium" | "High"
    var date: String = "",
    var time: String = "",
    var status: String = "Open"       // "Open" | "In Progress" | "Resolved"
) {
    constructor() : this("","","","","","","Medium","","","Open")
}