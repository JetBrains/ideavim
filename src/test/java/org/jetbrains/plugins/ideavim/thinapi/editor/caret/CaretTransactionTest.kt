/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.editor.caret

import com.intellij.vim.api.Range
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CaretTransactionTest : VimTestCase() {
  private lateinit var vimScope: VimScope

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    vimScope = VimScopeImpl(listenerOwner, mappingOwner)

    configureByText("\n")
  }

  fun executeAction(action: suspend () -> Unit) {
    injector.application.invokeAndWait {
      injector.actionExecutor.executeCommand(fixture.editor.vim, {
        runBlocking(Dispatchers.Unconfined) {
          action()
        }
      }, "", null)
    }
  }

  @Test
  fun `test delete word`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            deleteText(4, 8)
          }
        }
      }
    }

    assertState("one ${c}three")
  }

  @Test
  fun `test update caret position`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            updateCaret(8)
          }
        }
      }
    }

    assertState("one two ${c}three")
  }

  @Test
  fun `test update caret with selection`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            updateCaret(7, Range.Simple(4, 7))
          }
        }
      }
    }

    assertState("one ${s}two${c}${se} three")
  }

  @Test
  fun `test update caret to beginning of file`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            updateCaret(0)
          }
        }
      }
    }

    assertState("${c}one two three")
  }

  @Test
  fun `test update caret to end of file`() {
    val text = "one ${c}two three"
    configureByText(text)

    val fileSize = fixture.editor.document.textLength

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            updateCaret(fileSize - 1)
          }
        }
      }
    }

    assertState("one two thre${c}e")
  }

  @Test
  fun `test update caret with offset out of range`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: IllegalArgumentException? = null

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            exception = assertThrows<IllegalArgumentException> {
              updateCaret(100)
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
  }

  @Test
  fun `test update caret with negative offset`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: IllegalArgumentException? = null

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            exception = assertThrows<IllegalArgumentException> {
              updateCaret(-1)
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
  }

  @Test
  fun `test update caret with selection out of range`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: IllegalArgumentException? = null

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            exception = assertThrows<IllegalArgumentException> {
              updateCaret(offset = 0, selection = Range.Simple(100, 101))
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
  }

  @Test
  fun `test insert word before caret, caret after inserted text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(4, "NEW", caretAtEnd = true, insertBeforeCaret = true)
          }
        }
      }
    }

    assertState("one NE${c}Wtwo three")
  }

  @Test
  fun `test insert word before caret, caret at the beginning of inserted text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(4, "NEW", caretAtEnd = false, insertBeforeCaret = true)
          }
        }
      }
    }

    assertState("one ${c}NEWtwo three")
  }

  @Test
  fun `test insert word after caret, caret after inserted text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(4, "NEW", caretAtEnd = true, insertBeforeCaret = false)
          }
        }
      }
    }

    assertState("one tNE${c}Wwo three")
  }

  @Test
  fun `test insert word after caret, caret at the beginning of inserted text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(4, "NEW", caretAtEnd = false, insertBeforeCaret = false)
          }
        }
      }
    }

    assertState("one t${c}NEWwo three")
  }

  @Test
  fun `test insertion at beginning of file`() {
    val text = "${c}one two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(0, "START: ", insertBeforeCaret = true)
          }
        }
      }
    }

    assertState("START:${c} one two three")
  }

  @Test
  fun `test insertion at end of file`() {
    val text = "one two three${c}"
    configureByText(text)

    val fileSize = fixture.editor.document.textLength

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(fileSize - 1, " END")
          }
        }
      }
    }

    assertState("one two three EN${c}D")
  }

  @Test
  fun `test insertion of empty text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(4, "")
          }
        }
      }
    }

    assertState("one ${c}two three")
  }

  @Test
  fun `test insert when empty file`() {
    val text = "${c}"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(0, "TEXT")
          }
        }
      }
    }

    assertState("TEX${c}T")
  }

  @Test
  fun `test insert empty text into empty file`() {
    val text = "${c}"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(0, "")
          }
        }
      }
    }

    assertState("${c}")
  }

  @Test
  fun `test insertion of multi-line text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            insertText(4, "line1\nline2\nline3", insertBeforeCaret = true)
          }
        }
      }
    }

    assertState("one line1\nline2\nline${c}3two three")
  }

  @Test
  fun `test insertion with invalid position - negative`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: IllegalArgumentException? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              insertText(-1, "NEW")
            }
          }
        }
      }
    }

    assertNotNull(exception)
    assertState("one ${c}two three")
  }

  @Test
  fun `test insertion with invalid position - beyond file size`() {
    val text = "one ${c}two three"
    configureByText(text)

    val fileSize = fixture.editor.document.textLength
    var exception: IllegalArgumentException? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              insertText(fileSize + 10, "NEW")
            }
          }
        }
      }
    }

    assertNotNull(exception)

    val expectedText = "one two three"
    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test basic text replacement`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            replaceText(4, 7, "REPLACED")
          }
        }
      }
    }

    assertState("one REPLACE${c}D three")
  }

  @Test
  fun `test replacement at beginning of file`() {
    val text = "${c}one two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            replaceText(0, 3, "START")
          }
        }
      }
    }

    assertState("STAR${c}T two three")
  }

  @Test
  fun `test replacement at end of file`() {
    val text = "one two ${c}three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            replaceText(8, 13, "END")
          }
        }
      }
    }

    assertState("one two EN${c}D")
  }

  @Test
  fun `test replacement with empty text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            replaceText(4, 8, "")
          }
        }
      }
    }

    assertState("one${c} three")
  }

  @Test
  fun `test replacement of one character`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            replaceText(4, 5, "INSERTED")
          }
        }
      }
    }

    assertState("one INSERTE${c}Dwo three")
  }

  @Test
  fun `test replacement of empty range`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            replaceText(4, 4, "INSERTED")
          }
        }
      }
    }

    assertState("one ${c}INSERTEDtwo three")
  }

  @Test
  fun `test replacement with multi-line text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            replaceText(4, 7, "line1\nline2\nline3")
          }
        }
      }
    }

    assertState("one line1\nline2\nline${c}3 three")
  }

  @Test
  fun `test replacement with invalid offsets - negative start`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: Exception? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              replaceText(-1, 7, "REPLACED")
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
    assertState("one ${c}two three")
  }

  @Test
  fun `test replacement with invalid offsets - negative end`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: Exception? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              replaceText(4, -1, "REPLACED")
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
    assertState("one ${c}two three")
  }

  @Test
  fun `test replacement with invalid offsets - beyond file size`() {
    val text = "one ${c}two three"
    configureByText(text)

    val fileSize = fixture.editor.document.textLength
    var exception: IllegalArgumentException? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              replaceText(4, fileSize + 10, "REPLACED")
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
    assertState("one ${c}two three")
  }

  @Test
  fun `test replacement with start greater than end`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: Exception? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              replaceText(7, 4, "REPLACED")
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
    assertState("one ${c}two three")
  }

  @Test
  fun `test basic blockwise replacement`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            val range = Range.Block(
              arrayOf(
                Range.Simple(0, 3),
                Range.Simple(14, 18)
              )
            )
            replaceTextBlockwise(range, listOf("AAA", "BBBB"))
          }
        }
      }
    }

    val expectedText = """
            AAA two three
            BBBB five six
            seven eight nine
        """.trimIndent()

    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test blockwise replacement with mismatched sizes`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    var exception: IllegalArgumentException? = null

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            val range = Range.Block(
              arrayOf(
                Range.Simple(0, 3),
                Range.Simple(14, 18)
              )
            )
            exception = assertThrows<IllegalArgumentException> {
              replaceTextBlockwise(range, listOf("AAA"))
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
    assertState(text)
  }

  @Test
  fun `test blockwise replacement with empty text`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            val range = Range.Block(
              arrayOf(
                Range.Simple(0, 3),
                Range.Simple(14, 18)
              )
            )
            replaceTextBlockwise(range, listOf("", ""))
          }
        }
      }
    }

    val expectedText = """
             two three
             five six
            seven eight nine
        """.trimIndent()

    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test blockwise replacement with invalid ranges`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    val fileSize = fixture.editor.document.textLength
    var exception: Exception? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              val range = Range.Block(
                arrayOf(
                  Range.Simple(0, 3),
                  Range.Simple(fileSize + 10, fileSize + 15)
                )
              )
              replaceTextBlockwise(range, listOf("AAA", "BBBB"))
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
  }


  @Test
  fun `test basic text deletion`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            deleteText(4, 7)
          }
        }
      }
    }

    assertState("one ${c} three")
  }

  @Test
  fun `test deletion at beginning of file`() {
    val text = "${c}one two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            deleteText(0, 4)
          }
        }
      }
    }

    assertState("${c}two three")
  }

  @Test
  fun `test deletion at end of file`() {
    val text = "one two ${c}three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            deleteText(8, 13)
          }
        }
      }
    }

    val expectedText = "one two"
    val actualText = fixture.editor.document.text.trim()

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test deletion of empty range`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            deleteText(4, 4)
          }
        }
      }
    }

    assertState("one ${c}two three")
  }

  @Test
  fun `test deletion of entire file`() {
    val text = "${c}one two three"
    configureByText(text)

    val fileSize = fixture.editor.document.textLength

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            deleteText(0, fileSize)
          }
        }
      }
    }

    assertState("${c}")
  }

  @Test
  fun `test deletion with invalid offsets - negative start`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: Exception? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              deleteText(-1, 7)
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
  }

  @Test
  fun `test deletion with invalid offsets - negative end`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: Exception? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              deleteText(4, -1)
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
  }

  @Test
  fun `test deletion with invalid offsets - beyond file size`() {
    val text = "one ${c}two three"
    configureByText(text)

    val fileSize = fixture.editor.document.textLength
    var exception: Exception? = null

    executeAction {
      vimScope.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              deleteText(4, fileSize + 10)
            }
          }
        }
      }.join()
    }

    assertNotNull(exception)
  }

  @Test
  fun `test deletion with start greater than end`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      vimScope.editor {
        change {
          withPrimaryCaret {
            deleteText(7, 4)
          }
        }
      }
    }

    val expectedText = "one  three"
    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }
}
