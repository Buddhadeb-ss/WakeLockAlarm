package com.antigravity.riselock.model

enum class MissionType(val displayName: String, val description: String) {
    NONE(
        displayName = "Standard Wake",
        description = "Standard dismiss button"
    ),
    STEPS(
        displayName = "Step Challenge",
        description = "Must walk 20 to 100 physical steps to turn off"
    ),
    PHOTO_MATCH(
        displayName = "Photo Match",
        description = "Must take a live photo of a registered spot (e.g., bathroom, kitchen)"
    )
}

