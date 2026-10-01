package com.rentalvalidator.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(AppSize.fieldRadius),
    large = RoundedCornerShape(AppSize.cardRadius),
    extraLarge = RoundedCornerShape(20.dp)
)
