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

@OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
fun <T: R, R> T.letIfElse(
    condition: Boolean,
    onTrue: (T) -> R,
    onFalse: (T) -> R
): R {
    contract {
        callsInPlace(onTrue, InvocationKind.AT_MOST_ONCE)
        condition.holdsIn(onTrue)
        callsInPlace(onFalse, InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) {
        onTrue(this)
    } else {
        onFalse(this)
    }
}