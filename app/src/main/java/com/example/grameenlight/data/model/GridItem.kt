package com.example.grameenlight.data.model

import androidx.compose.runtime.MutableState

sealed class GridItem {
    data class Road(val name: String? = null, val isMain: Boolean = false) : GridItem()
    data class House(val label: String? = null) : GridItem()
    data class Landmark(val name: String, val icon: String) : GridItem()
    data class Pole(val id: String, var status: MutableState<String>) : GridItem()
    object PanchayathHouse : GridItem()
    object Grass : GridItem()
    object Empty : GridItem()
}