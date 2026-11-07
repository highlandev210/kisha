package com.example.kisha.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps a stored category icon key to its icon; unknown keys fall back to the generic category icon. */
fun categoryIcon(key: String?): ImageVector = when (key) {
    "restaurant" -> Icons.Outlined.Restaurant
    "directions_bus" -> Icons.Outlined.DirectionsBus
    "local_grocery_store" -> Icons.Outlined.LocalGroceryStore
    "shopping_bag" -> Icons.Outlined.ShoppingBag
    "receipt_long" -> Icons.AutoMirrored.Outlined.ReceiptLong
    "medical_services" -> Icons.Outlined.MedicalServices
    "movie" -> Icons.Outlined.Movie
    else -> Icons.Outlined.Category
}
