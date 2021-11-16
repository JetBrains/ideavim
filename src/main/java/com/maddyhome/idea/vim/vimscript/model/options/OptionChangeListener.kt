package com.maddyhome.idea.vim.vimscript.model.options

import com.intellij.openapi.editor.Editor

interface OptionChangeListener<T> {

  fun processGlobalValueChange(oldValue: T?)
}

// options that can change their values in specific editors
interface LocalOptionChangeListener<T> : OptionChangeListener<T> {

  fun processLocalValueChange(oldValue: T?, editor: Editor)
}

