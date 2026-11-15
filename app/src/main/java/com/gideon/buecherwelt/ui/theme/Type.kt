package com.gideon.buecherwelt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gideon.buecherwelt.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_400, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_500, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_600, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_700, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_800, FontWeight.ExtraBold),
    Font(R.font.plus_jakarta_sans_italic_400, FontWeight.Normal, FontStyle.Italic)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = PlusJakartaSans, fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontFamily = PlusJakartaSans, fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontFamily = PlusJakartaSans, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontFamily = PlusJakartaSans, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontFamily = PlusJakartaSans, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontFamily = PlusJakartaSans, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = PlusJakartaSans, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = PlusJakartaSans, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = PlusJakartaSans, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = PlusJakartaSans, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)
