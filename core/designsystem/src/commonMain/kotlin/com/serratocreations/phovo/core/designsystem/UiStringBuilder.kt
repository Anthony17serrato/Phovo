package com.serratocreations.phovo.core.designsystem

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Given the provided constructor arguments this model is able to build strings correctly at the
 * time in which the UI needs them.
 * @param resource is the defined resource type
 * @param templateArgs any arguments that are necessary to build the string
 */
class UiStringBuilder(
    private val resource: StringBuilderResource,
    private vararg val templateArgs: String
) {
    /**
     * Build the string defined by this builder
     */
    @Composable
    fun build(): String {
        return when(resource) {
            is PhovoPlural -> {
                pluralStringResource(resource.pluralResource, resource.pluralCount,
                    *(templateArgs))
            }
            is PhovoString -> {
                stringResource(resource.stringResource, *(templateArgs))
            }
        }
    }
}