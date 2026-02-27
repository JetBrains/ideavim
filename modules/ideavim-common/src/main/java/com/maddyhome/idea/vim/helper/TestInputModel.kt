/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.google.common.collect.Lists
import com.intellij.openapi.editor.Editor
import javax.swing.KeyStroke

// Do not remove until it's used in EasyMotion plugin in tests
class TestInputModel private constructor() {
  private val myKeyStrokes: MutableList<KeyStroke> = Lists.newArrayList()
  fun setKeyStrokes(keyStrokes: List<KeyStroke>) {
    myKeyStrokes.clear()
    myKeyStrokes.addAll(keyStrokes)
  }

  fun nextKeyStroke(): KeyStroke? {
    // Return key from the unfinished mapping
    /*
MappingStack mappingStack = KeyHandler.getInstance().getMappingStack();
if (mappingStack.hasStroke()) {
  return mappingStack.feedStroke();
}
*/
    return if (myKeyStrokes.isNotEmpty()) {
      myKeyStrokes.removeAt(0)
    } else {
      null
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(editor: Editor): TestInputModel {
      var model = editor.vimTestInputModel
      if (model == null) {
        model = TestInputModel()
        editor.vimTestInputModel = model
      }
      return model
    }
  }
}
