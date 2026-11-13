package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PakGivesLightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = OnEmeraldPrimary,
  primaryContainer = EmeraldPrimaryContainer,
  onPrimaryContainer = OnEmeraldPrimaryContainer,
  secondary = SaffronSecondary,
  onSecondary = OnSaffronSecondary,
  secondaryContainer = SaffronSecondaryContainer,
  onSecondaryContainer = OnSaffronSecondaryContainer,
  tertiary = IndusTealTertiary,
  onTertiary = OnIndusTealTertiary,
  tertiaryContainer = IndusTealContainer,
  onTertiaryContainer = OnIndusTealContainer,
  background = WarmAlabasterBg,
  onBackground = OnWarmSurface,
  surface = PureIvorySurface,
  onSurface = OnWarmSurface,
  surfaceVariant = SoftParchmentVariant,
  onSurfaceVariant = OnSoftParchmentVariant,
  outline = SubtleOutline,
)

private val PakGivesDarkColorScheme = darkColorScheme(
  primary = EmeraldDark,
  onPrimary = OnEmeraldDark,
  primaryContainer = EmeraldDarkContainer,
  onPrimaryContainer = OnEmeraldDarkContainer,
  secondary = SaffronDark,
  onSecondary = OnSaffronDark,
  secondaryContainer = SaffronDarkContainer,
  onSecondaryContainer = OnSaffronDarkContainer,
  tertiary = IndusTealDark,
  onTertiary = OnIndusTealDark,
  tertiaryContainer = IndusTealDarkContainer,
  onTertiaryContainer = OnIndusTealDarkContainer,
  background = ObsidianPineBg,
  onBackground = OnObsidianText,
  surface = CardPineSurface,
  onSurface = OnObsidianText,
  surfaceVariant = CardPineVariant,
  onSurfaceVariant = OnCardPineVariant,
  outline = DarkOutline,
)

private val PakGivesShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
)

@Composable
fun PakGivesTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) PakGivesDarkColorScheme else PakGivesLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = PakGivesShapes,
    content = content,
  )
}
