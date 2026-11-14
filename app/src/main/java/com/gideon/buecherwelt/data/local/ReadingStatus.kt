package com.gideon.buecherwelt.data.local

enum class ReadingStatus(val label: String) {
    PLANNED("Geplant"),
    READING("Lesend"),
    READ("Gelesen"),
    ABANDONED("Abgebrochen");

    companion object {
        fun fromString(value: String?): ReadingStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PLANNED
    }
}
