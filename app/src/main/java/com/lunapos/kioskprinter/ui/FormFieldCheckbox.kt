package com.lunapos.kioskprinter.ui

import android.widget.CheckBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import reactivecircus.flowbinding.android.widget.checkedChanges

class FormFieldCheckbox(
    scope: CoroutineScope,
    private val checkboxLayout: CheckBox
) : FormField<Boolean>() {

    private var isEnabled: Boolean
        get() = checkboxLayout.isEnabled
        set(value) {
            checkboxLayout.isEnabled = value
        }

    var value: Boolean?
        get() = stateInternal.value
        set(value) {
            if (value != null) {
                checkboxLayout.isChecked = value
            }
        }

    init {
        checkboxLayout.checkedChanges().skipInitialValue().onEach { checked ->
            clearError()
            stateInternal.update { checked }
        }.launchIn(scope)
    }

    override suspend fun validate(focusIfError: Boolean): Boolean {
        return true
    }

    override fun clearError() {
        if (checkboxLayout.error != null) {
            checkboxLayout.error = null
        }
    }

    override fun clearFocus() {
        checkboxLayout.clearFocus()
    }

    override fun disable() {
        isEnabled = false
    }

    override fun enable() {
        isEnabled = true
    }
}