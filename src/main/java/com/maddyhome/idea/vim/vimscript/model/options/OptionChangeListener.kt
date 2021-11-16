package com.maddyhome.idea.vim.vimscript.model.options

import com.intellij.openapi.editor.Editor

abstract class OptionChangeListener<T> {

  abstract fun processGlobalValueChange(oldValue: T?)
}

// options that can change their values in specific editors
abstract class LocalOptionChangeListener<T> : OptionChangeListener<T>() {

  abstract fun processLocalValueChange(oldValue: T?, editor: Editor)
}

