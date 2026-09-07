package com.wink.eye.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Claude 风格（参考 anthropic DESIGN.md）
// 奶油画布 canvas + 珊瑚主色 primary + 暖墨 ink + 深色 surface
// 强调"温暖编辑排版"，避免冷蓝/纯白
// ============================================================

// ---- 亮色 ----
val LightPrimary = Color(0xFFCC785C)              // coral 珊瑚主色
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFF6DDD3)     // 浅珊瑚暖容器
val LightOnPrimaryContainer = Color(0xFF3B1F15)

val LightSecondary = Color(0xFF5DB8A6)            // 青绿强调（克制使用）
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD3EFE9)
val LightOnSecondaryContainer = Color(0xFF103C35)

val LightTertiary = Color(0xFFE8A55A)             // 琥珀暖调
val LightOnTertiary = Color(0xFF3F2A0E)
val LightTertiaryContainer = Color(0xFFFBDFC2)
val LightOnTertiaryContainer = Color(0xFF3C2505)

val LightBackground = Color(0xFFFAF9F5)           // 奶油画布 canvas
val LightOnBackground = Color(0xFF141413)          // 暖墨 ink
val LightSurface = Color(0xFFFAF9F5)
val LightOnSurface = Color(0xFF141413)
val LightSurfaceVariant = Color(0xFFF3EDE4)        // 奶油卡片 surface-card
val LightOnSurfaceVariant = Color(0xFF6C6A64)      // muted
val LightSurfaceContainer = LightSurfaceVariant
val LightSurfaceContainerHigh = Color(0xFFECE5D9)  // surface-cream-strong
val LightOutline = Color(0xFFE0D6C9)               // hairline
val LightOutlineVariant = Color(0xFFE6DFD8)

val LightError = Color(0xFFC64545)
val LightOnError = Color(0xFFFFFFFF)

// ---- 暗色（claude surface-dark #181715 画布）----
val DarkPrimary = Color(0xFFE09078)               // 珊瑚在暗色上提亮
val DarkOnPrimary = Color(0xFF3B1F15)
val DarkPrimaryContainer = Color(0xFF3A2A22)
val DarkOnPrimaryContainer = Color(0xFFF6D8CC)

val DarkSecondary = Color(0xFF8FCEC2)
val DarkOnSecondary = Color(0xFF10302A)
val DarkSecondaryContainer = Color(0xFF1E4C44)
val DarkOnSecondaryContainer = Color(0xFFD3EFE9)

val DarkTertiary = Color(0xFFE8C08A)
val DarkOnTertiary = Color(0xFF3C2505)
val DarkTertiaryContainer = Color(0xFF50340C)
val DarkOnTertiaryContainer = Color(0xFFFBDFC2)

val DarkBackground = Color(0xFF181715)            // surface-dark 画布
val DarkOnBackground = Color(0xFFEDEBE5)          // on-dark 奶油白
val DarkSurface = Color(0xFF181715)
val DarkOnSurface = Color(0xFFEDEBE5)
val DarkSurfaceVariant = Color(0xFF262320)        // surface-dark-soft
val DarkOnSurfaceVariant = Color(0xFFA09D96)      // on-dark-soft
val DarkSurfaceContainer = DarkSurfaceVariant
val DarkSurfaceContainerHigh = Color(0xFF2E2B26)  // surface-dark-elevated
val DarkOutline = Color(0xFF3A3732)
val DarkOutlineVariant = Color(0xFF3A3732)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)