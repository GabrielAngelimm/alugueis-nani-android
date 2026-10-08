package com.rentalvalidator.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radius follows hierarchy: marks are tight, controls are firm, sheets are generous. */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(AppSize.markRadius),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(AppSize.controlRadius),
    large = RoundedCornerShape(AppSize.sheetRadius),
    extraLarge = RoundedCornerShape(AppSize.modalRadius)
)
