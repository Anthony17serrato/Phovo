package com.serratocreations.phovo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serratocreations.phovo.core.common.PhovoBuildInfo
import org.jetbrains.compose.resources.stringResource
import phovo.sharedumbrella.generated.resources.Res
import phovo.sharedumbrella.generated.resources.about_build
import phovo.sharedumbrella.generated.resources.about_prerelease_notice
import phovo.sharedumbrella.generated.resources.about_version
import phovo.sharedumbrella.generated.resources.app_name

/** Shows which build is installed, so testers can quote it in bug reports. */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    // Dev builds say so next to the build number; prod builds show the number alone.
    val build = if (PhovoBuildInfo.FLAVOR == "prod") {
        PhovoBuildInfo.VERSION_CODE.toString()
    } else {
        "${PhovoBuildInfo.VERSION_CODE} (${PhovoBuildInfo.FLAVOR})"
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineSmall
        )
        // Selectable so a tester can copy the version into a bug report.
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.about_version, PhovoBuildInfo.VERSION_NAME),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(Res.string.about_build, build),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (PhovoBuildInfo.IS_PRERELEASE) {
            Text(
                text = stringResource(Res.string.about_prerelease_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
