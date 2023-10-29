package hu.webhejj.perspektive.plantuml

data class PlantUmlOptions(
    val direction: Direction = Direction.TOP_TO_BOTTOM,
) {
    enum class Direction {
        TOP_TO_BOTTOM,
        LEFT_TO_RIGHT,
    }
}
