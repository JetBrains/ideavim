/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.scopes.Transaction
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class TransactionTest : VimTestCase() {

  @Test
  fun `test insertTextBeforeCaret inserts text at specified position`() {
    doTest(
      keys = "",
      before = "Hello${c} World",
      after = "Hello Beautiful${c} World",
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      val textToInsert = " Beautiful"

      executeInsideTransaction {
        insertTextBeforeCaret(caretId, 5, textToInsert)
      }
    }
  }

  @Test
  fun `test replaceText replaces text between offsets`() {
    doTest(
      keys = "",
      before = "Hello ${c}World",
      after = "Hello Univers${c}e",
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        replaceText(caretId, 6, 11, "Universe", options = Transaction.TextOperationOptions())
      }
    }
  }

  @Test
  fun `test deleteText deletes text between offsets`() {
    doTest(
      keys = "",
      before = "Hello${c} World",
      after = "Hell${c}o",
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        deleteText(caretId, 5, 11)
      }
    }
  }

  @Test
  fun `test insertTextBeforeCaret with single line text`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
      """.trimIndent(),
      after = """
        First line - inserted tex${c}t
        Second line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextBeforeCaret(caretId, 10, " - inserted text")
      }
    }
  }

  @Test
  fun `test insertTextBeforeCaret at beginning of line`() {
    doTest(
      keys = "",
      before = """
        ${c}First line
        Second line
      """.trimIndent(),
      after = """
        Prefix: ${c}First line
        Second line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextBeforeCaret(caretId, 0, "Prefix: ")
      }
    }
  }

  @Test
  fun `test insertTextBeforeCaret at end of line`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
      """.trimIndent(),
      after = """
        First line - suffi${c}x
        Second line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextBeforeCaret(caretId, 10, " - suffix")
      }
    }
  }

  @Test
  fun `test insertTextBeforeCaret with empty text`() {
    doTest(
      keys = "",
      before = "First line${c}\nSecond line",
      after = "First${c} line\nSecond line",
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextBeforeCaret(caretId, 5, "")
      }
    }
  }

  @Test
  fun `test insertTextBeforeCaret with multiline text`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
      """.trimIndent(),
      after = """
        First line
        Inserted line 1
        Inserted line ${c}2
        Second line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextBeforeCaret(caretId, 10, "\nInserted line 1\nInserted line 2")
      }
    }
  }

  @Test
  fun `test replaceText with empty replacement`() {
    doTest(
      keys = "",
      before = "Hello ${c}World",
      after = "Hello${c} ",
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        replaceText(caretId, 6, 11, "", options = Transaction.TextOperationOptions())
      }
    }
  }

  @Test
  fun `test replaceText with multiline replacement`() {
    doTest(
      keys = "",
      before = """
        Hello ${c}World
        Next line
      """.trimIndent(),
      after = """
        Hello Universe
        Galax${c}y
        Next line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        replaceText(caretId, 6, 11, "Universe\nGalaxy", options = Transaction.TextOperationOptions())
      }
    }
  }

  @Test
  fun `test deleteText with empty range`() {
    doTest(
      keys = "",
      before = "Hello${c} World",
      after = "Hello${c} World",
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        deleteText(caretId, 5, 5)
      }
    }
  }

  @Test
  fun `test deleteText with multiline range`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
        Third line
      """.trimIndent(),
      after = """
        First lin${c}e
        Third line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        deleteText(caretId, 10, 22)
      }
    }
  }

  @Test
  fun `test insertTextAfterCaret with multiple carets`() {
    doTest(
      keys = "",
      before = """
        First${c} line
        Second${c} line
        Third${c} line
      """.trimIndent(),
      after = """
        First - modified${c} line
        Second - modified${c} line
        Third - modified${c} line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val carets = vimEditor.carets()

      executeInsideTransaction {
        for (caret in carets) {
          val offset = caret.offset
          insertTextAfterCaret(CaretId(caret.id), offset, "- modified ")
        }
      }
    }
  }

  @Test
  fun `test replaceText with multiple carets`() {
    doTest(
      keys = "",
      before = """
        First ${c}line
        Second ${c}line
        Third ${c}line
      """.trimIndent(),
      after = """
        First tex${c}t
        Second tex${c}t
        Third tex${c}t
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val carets = vimEditor.carets()

      executeInsideTransaction {
        for (caret in carets) {
          val offset = caret.offset
          replaceText(
            CaretId(caret.id),
            offset,
            offset + 4,
            "text",
            options = Transaction.TextOperationOptions()
          )
        }
      }
    }
  }

  @Test
  fun `test deleteText with multiple carets`() {
    doTest(
      keys = "",
      before = """
        First${c} line
        Second${c} line
        Third${c} line
      """.trimIndent(),
      after = """
        Firs${c}t
        Secon${c}d
        Thir${c}d
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val carets = vimEditor.carets()

      executeInsideTransaction {
        for (caret in carets) {
          val offset = caret.offset
          deleteText(CaretId(caret.id), offset, offset + 5)
        }
      }
    }
  }

  @Test
  fun `test insertTextAtLine inserts text at specified line`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
        Third line
      """.trimIndent(),
      after = """
        First line
        Inserted textSecond line
        Third line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextAtLine(caretId, 1, "Inserted text")
      }
    }
  }

  @Test
  fun `test insertTextAtLine with multiline text`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
        Third line
      """.trimIndent(),
      after = """
        First line
        Line 1
        Line 2Second line
        Third line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      val multilineText = """
        Line 1
        Line 2
      """.trimIndent()

      executeInsideTransaction {
        insertTextAtLine(caretId, 1, multilineText)
      }
    }
  }

  @Test
  fun `test insertTextAtLine with multiple carets`() {
    doTest(
      keys = "",
      before = """
        First${c} line
        Second${c} line
        Third${c} line
      """.trimIndent(),
      after = """
        Inserted at line 0First line
        Inserted at line 1Second line
        Inserted at line 2Third line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val carets = vimEditor.carets()

      executeInsideTransaction {
        carets.forEachIndexed { index, caret ->
          insertTextAtLine(CaretId(caret.id), index, "Inserted at line $index")
        }
      }
    }
  }

  @Test
  fun `test insertTextAtLine with empty text`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
        Third line
      """.trimIndent(),
      after = """
        First line
        Second line
        Third line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextAtLine(caretId, 1, "")
      }
    }
  }

  @Test
  fun `test insertTextAtLine at first line`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
        Third line
      """.trimIndent(),
      after = """
        Inserted at first lineFirst line
        Second line
        Third line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextAtLine(caretId, 0, "Inserted at first line")
      }
    }
  }

  @Test
  fun `test insertTextAtLine at last line`() {
    doTest(
      keys = "",
      before = """
        First line${c}
        Second line
        Third line
      """.trimIndent(),
      after = """
        First line
        Second line
        Inserted at last lineThird line
      """.trimIndent(),
    ) { editor ->
      val vimEditor = editor.vim
      val caret = vimEditor.primaryCaret()
      val caretId = CaretId(caret.id)

      executeInsideTransaction {
        insertTextAtLine(caretId, 2, "Inserted at last line")
      }
    }
  }

  private fun executeInsideTransaction(block: Transaction.() -> Unit) {
    com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
      val listenerOwner = ListenerOwner.IdeaVim.System
      val mappingOwner = MappingOwner.IdeaVim.System
      VimScopeImpl(listenerOwner, mappingOwner).apply {
        editor {
          change {
            block()
          }
        }
      }
    }
  }
}
