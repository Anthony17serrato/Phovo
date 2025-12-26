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
import org.jetbrains.compose.resources.vectorResource
import phovo.composeapp.generated.resources.Res
import phovo.composeapp.generated.resources.phovo_icon
import kotlin.system.exitProcess
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    var isVisible by remember { mutableStateOf(true) }
    val isDark = isSystemInDarkMode()

    Tray(
        icon = tintedVectorPainter(
            imageVector = vectorResource(Res.drawable.phovo_icon),
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