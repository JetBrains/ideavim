/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key

/**
 * Extension property for storing TestInputModel on Editor.
 * Kept in common so both TestInputModel (common) and UserDataManager (frontend) can use it.
 */
var Editor.vimTestInputModel: TestInputModel?
  get() = getUserData(testInputModelKey)
  set(value) {
    putUserData(testInputModelKey, value)
  }

private val testInputModelKey = Key.create<TestInputModel>("vimTestInputModel by userData()")
