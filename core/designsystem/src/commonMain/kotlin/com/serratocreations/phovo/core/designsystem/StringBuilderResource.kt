package com.serratocreations.phovo.core.designsystem

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

sealed interface StringBuilderResource
data class PhovoString(val stringResource: StringResource): StringBuilderResource
data class PhovoPlural(
    val pluralResource: PluralStringResource,
    val pluralCount: Int
): StringBuilderResource