package com.lunapos.kioskprinter.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class FormField<T> {
    val stateInternal = MutableStateFlow<T?>(null)

    // state is StateFlow. It will be helpful for collecting any change in current value.
    val state = stateInternal.asStateFlow()

    val isValidInternal = MutableStateFlow(true)

    // isValid is StateFlow. It will be helpful for collecting any change in validation process.
    val isValid = isValidInternal.asStateFlow()

    // We will call validate method, when we want to perform validation.
    abstract suspend fun validate(focusIfError: Boolean = true): Boolean

    open fun clearError() {}
    open fun clearFocus() {}
    open fun disable() {}
    open fun enable() {}
}