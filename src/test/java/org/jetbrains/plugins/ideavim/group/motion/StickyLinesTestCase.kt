/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.impl.stickyLines.StickyLinesModel
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.EditorTestUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import org.jetbrains.plugins.ideavim.VimTestCase
import kotlin.math.roundToInt

/**
 * Tests with a real sticky lines panel. The panel is only created for main editors, and the fixture's editor isn't
 * one, so the tests run in a main editor of their own on the fixture's document.
 */
abstract class StickyLinesTestCase : VimTestCase() {
  protected fun withMainEditor(rows: Int, action: (Editor) -> Unit) {
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
      editor = EditorFactory.getInstance()
        .createEditor(fixture.editor.document, fixture.project, EditorKind.MAIN_EDITOR)
      EditorTestUtil.setEditorVisibleSizeInPixels(
        editor,
        (screenWidth * EditorHelper.getPlainSpaceWidthFloat(editor!!)).roundToInt(),
        rows * editor.lineHeight,
      )
    }
    try {
      action(editor!!)
    } finally {
      ApplicationManager.getApplication().invokeAndWait { EditorFactory.getInstance().releaseEditor(editor!!) }
    }
  }

  /** A sticky line for every line that ends with `{`, until the matching `}` */
  protected fun addStickyLinesForBlocks(editor: Editor) {
    addStickyLines(editor) { add ->
      val document = editor.document
      val open = ArrayDeque<Int>()
      for (line in 0 until document.lineCount) {
        val text = document.getText(TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)))
        if (text.trimEnd().endsWith("{")) open.addLast(line)
        if (text.trim() == "}") add(open.removeLast(), line)
      }
    }
  }

  /** Adds sticky lines from the first line to the last line of each scope that [scopes] passes to `add` */
  protected fun addStickyLines(editor: Editor, scopes: (add: (Int, Int) -> Unit) -> Unit) {
    ApplicationManager.getApplication().invokeAndWait {
      val document = editor.document
      val model = StickyLinesModel.getModel(fixture.project, document)!!
      scopes { first, last ->
        model.addStickyLine(document.getLineStartOffset(first), document.getLineEndOffset(last), null)
      }
      model.notifyLinesUpdate()
    }
  }

  protected fun typeText(editor: Editor, keys: String) =
    typeText(injector.parser.parseKeys(keys), editor, fixture.project)
}
