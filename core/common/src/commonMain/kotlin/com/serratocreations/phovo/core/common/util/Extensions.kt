package com.serratocreations.phovo.core.common.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
fun <T: R, R> T.letIf(condition: Boolean, block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        condition.holdsIn(block)
    }
    return if (condition) {
        block(this)
    } else {
        this
    }
}