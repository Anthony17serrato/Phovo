package com.serratocreations.phovo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.serratocreations.phovo.di.initApplication
import com.serratocreations.phovo.ui.PhovoApp
import kotlin.system.exitProcess
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import com.serratocreations.phovo.core.designsystem.icon.PhovoIcons

fun main() = application {
    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
        size = DpSize(1280.dp, 800.dp),
    )
    var isVisible by remember { mutableStateOf(true) }
    val isDark = isSystemInDarkMode()

    Tray(
        icon = tintedVectorPainter(
            imageVector = PhovoIcons.PhovoIcon,
            tint = if (isDark) Color.White else Color.Black
        ),
        tooltip = "Phovo"
    ) {
        Item("Open") { isVisible = true }
        Item("Exit") { exitProcess(0) }
    }

    Window(
        onCloseRequest = { isVisible = false },// Keep the process alive
        title = "Phovo",
        visible = isVisible,
        state = windowState
    ) {
        initApplication()
        PhovoApp()
    }
}

@Composable
fun tintedVectorPainter(
    imageVector: ImageVector,
    tint: Color
): Painter {
    val basePainter = rememberVectorPainter(imageVector)

    return remember(tint) {
        object : Painter() {
            override val intrinsicSize = basePainter.intrinsicSize

            override fun DrawScope.onDraw() {
                with(basePainter) {
                    draw(
                        size = size,
                        colorFilter = ColorFilter.tint(tint)
                    )
                }
            }
        }
    }
}