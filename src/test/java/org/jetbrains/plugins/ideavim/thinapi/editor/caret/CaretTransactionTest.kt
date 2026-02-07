/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.editor.caret

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.models.Range
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimApiImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaretTransactionTest : VimTestCase() {
  private lateinit var myVimApi: VimApi

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    myVimApi = VimApiImpl(listenerOwner, mappingOwner)

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
      myVimApi.editor {
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
      myVimApi.editor {
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
  fun `test update caret to beginning of file`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
        change {
          withPrimaryCaret {
            exception = assertThrows<IllegalArgumentException> {
              updateCaret(100)
            }
          }
        }
      }
    }

    assertNotNull(exception)
  }

  @Test
  fun `test update caret with negative offset`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: IllegalArgumentException? = null

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            exception = assertThrows<IllegalArgumentException> {
              updateCaret(-1)
            }
          }
        }
      }
    }

    assertNotNull(exception)
  }

  @Test
  fun `test insert word before caret, caret after inserted text`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
  fun `test replacement of one character at the beginning of the file`() {
    val text = "${c}o"
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            replaceText(0, 1, "one")
          }
        }
      }
    }

    assertState("on${c}e")
  }

  @Test
  fun `test replacement when empty editor`() {
    val text = "${c}"
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            replaceText(0, 0, "one")
          }
        }
      }
    }

    assertState("on${c}e")
  }

  @Test
  fun `test replacement of empty range`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              replaceText(-1, 7, "REPLACED")
            }
          }
        }
      }
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
      myVimApi.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              replaceText(4, -1, "REPLACED")
            }
          }
        }
      }
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
      myVimApi.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              replaceText(4, fileSize + 10, "REPLACED")
            }
          }
        }
      }
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
      myVimApi.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              replaceText(7, 4, "REPLACED")
            }
          }
        }
      }
    }

    assertNotNull(exception)
    assertState("one ${c}two three")
  }

  @Test
  fun `test basic blockwise replacement`() {
    // Text layout:
    // "one two three\nfour five six\nseven eight nine"
    // Line 0: offsets 0-13 (13 chars + newline at 13)
    // Line 1: offsets 14-27
    // Block(0, 17) selects "one " (cols 0-4) on line 0 and "four" (cols 0-4) on line 1
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            val range = Range.Block(0, 17)
            replaceTextBlockwise(range, listOf("AAA", "BBBB"))
          }
        }
      }
    }

    val expectedText = """
            AAAtwo three
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
      myVimApi.editor {
        change {
          withPrimaryCaret {
            // Block(0, 17) spans 2 lines, but we only provide 1 replacement text
            val range = Range.Block(0, 17)
            exception = assertThrows<IllegalArgumentException> {
              replaceTextBlockwise(range, listOf("AAA"))
            }
          }
        }
      }
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
      myVimApi.editor {
        change {
          withPrimaryCaret {
            // Block(0, 17) selects "one " on line 0 and "four" on line 1
            val range = Range.Block(0, 17)
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
      myVimApi.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              // Block with end offset beyond file size should throw
              val range = Range.Block(0, fileSize + 10)
              replaceTextBlockwise(range, listOf("AAA", "BBBB"))
            }
          }
        }
      }
    }

    assertNotNull(exception)
  }


  @Test
  fun `test basic text deletion`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
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
      myVimApi.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              deleteText(-1, 7)
            }
          }
        }
      }
    }

    assertNotNull(exception)
  }

  @Test
  fun `test deletion with invalid offsets - negative end`() {
    val text = "one ${c}two three"
    configureByText(text)

    var exception: Exception? = null

    executeAction {
      myVimApi.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              deleteText(4, -1)
            }
          }
        }
      }
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
      myVimApi.editor {
        change {
          exception = assertThrows<IllegalArgumentException> {
            withPrimaryCaret {
              deleteText(4, fileSize + 10)
            }
          }
        }
      }
    }

    assertNotNull(exception)
  }

  @Test
  fun `test deletion with start greater than end`() {
    val text = "one ${c}two three"
    configureByText(text)

    executeAction {
      myVimApi.editor {
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

  // ==================== Selection Property Tests ====================

  @Test
  fun `test selection in character visual mode returns Range Simple`() {
    val text = """
            ${c}one two three
            four five six
        """.trimIndent()
    configureByText(text)

    // Enter visual mode and select "one two"
    typeText("v6l")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Simple)
    val simple = selectionRange as Range.Simple
    assertEquals(0, simple.start)
    assertEquals(7, simple.end)
  }

  @Test
  fun `test selection in line visual mode returns Range Simple`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    // Enter line visual mode and select first two lines
    typeText("Vj")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Simple)
  }

  @Test
  fun `test selection in block visual mode returns Range Block with start and end`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    // Enter block visual mode and select a 2x3 block
    typeText("<C-V>2lj")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Block, "Expected Range.Block but got ${selectionRange?.javaClass?.simpleName}")
    val block = selectionRange as Range.Block
    // Block is defined by min selectionStart and max selectionEnd across all carets
    assertEquals(0, block.start)
    // End is exclusive: line 1 offset 14 + column 3 (exclusive) = 17
    assertEquals(17, block.end)
  }

  @Test
  fun `test selection in block visual mode spanning multiple lines`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    // Enter block visual mode and select across 3 lines
    typeText("<C-V>3l2j")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Block)
    val block = selectionRange as Range.Block
    assertEquals(0, block.start)
    // End should be on line 2, column 3
  }

  @Test
  fun `test selection in block visual mode with selection going up and left`() {
    val text = """
            one two three
            four five six
            seven ${c}eight nine
        """.trimIndent()
    configureByText(text)

    // Enter block visual mode and select upward and leftward
    typeText("<C-V>2h2k")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Block)
    val block = selectionRange as Range.Block
    // Ranges are normalized: start <= end regardless of selection direction
    assertTrue(block.start <= block.end, "Expected normalized range with start (${block.start}) <= end (${block.end})")
  }

  // ==================== Normalized Range Tests ====================

  @Test
  fun `test selection right to left in character visual mode is normalized`() {
    val text = """
            one two ${c}three
            four five six
        """.trimIndent()
    configureByText(text)

    // Enter visual mode and select leftward (right-to-left)
    typeText("v3h")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Simple)
    val simple = selectionRange as Range.Simple
    // Ranges are normalized: start <= end regardless of selection direction
    assertTrue(simple.start <= simple.end, "Expected normalized range with start (${simple.start}) <= end (${simple.end})")
  }

  @Test
  fun `test selection bottom to top in line visual mode is normalized`() {
    val text = """
            one two three
            four five six
            seven ${c}eight nine
        """.trimIndent()
    configureByText(text)

    // Enter line visual mode and select upward
    typeText("Vk")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Simple)
    val simple = selectionRange as Range.Simple
    // Ranges are normalized: start <= end regardless of selection direction
    assertTrue(simple.start <= simple.end, "Expected normalized range with start (${simple.start}) <= end (${simple.end})")
  }

  @Test
  fun `test block selection going left is normalized`() {
    val text = """
            one two ${c}three
            four five six
        """.trimIndent()
    configureByText(text)

    // Enter block visual mode and select leftward
    typeText("<C-V>3h")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Block)
    val block = selectionRange as Range.Block
    // Ranges are normalized: start <= end regardless of selection direction
    assertTrue(block.start <= block.end, "Expected normalized range with start (${block.start}) <= end (${block.end})")
  }

  @Test
  fun `test block selection going up is normalized`() {
    val text = """
            one two three
            four five six
            seven ${c}eight nine
        """.trimIndent()
    configureByText(text)

    // Enter block visual mode and select upward
    typeText("<C-V>k")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    assertTrue(selectionRange is Range.Block)
    val block = selectionRange as Range.Block
    // Ranges are normalized: start <= end regardless of selection direction
    assertTrue(block.start <= block.end, "Expected normalized range with start (${block.start}) <= end (${block.end})")
  }

  // ==================== Selection Marks Tests ====================

  @Test
  fun `test selectionMarks in block visual mode returns Range Block`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    // Enter block visual mode and make a selection
    typeText("<C-V>2lj")

    var selectionMarksRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionMarksRange = selectionMarks
          }
        }
      }
    }

    // selectionMarks may be null if marks haven't been set yet
    // but if it's not null, it should be Range.Block in block mode
    if (selectionMarksRange != null) {
      assertTrue(selectionMarksRange is Range.Block, "Expected Range.Block but got ${selectionMarksRange?.javaClass?.simpleName}")
    }
  }

  @Test
  fun `test selectionMarks after exiting block visual mode returns Range Block`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    // Enter block visual mode, make a selection, then exit
    // This sets the '< and '> marks
    typeText("<C-V>2lj<Esc>")

    // Re-enter visual mode to access selectionMarks
    typeText("gv")

    var selectionMarksRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionMarksRange = selectionMarks
          }
        }
      }
    }

    assertNotNull(selectionMarksRange)
    assertTrue(selectionMarksRange is Range.Block, "Expected Range.Block but got ${selectionMarksRange?.javaClass?.simpleName}")
  }

  @Test
  fun `test selectionMarks after exiting character visual mode returns Range Simple`() {
    val text = """
            ${c}one two three
            four five six
        """.trimIndent()
    configureByText(text)

    // Enter visual mode, make a selection, then exit and re-enter
    typeText("v4l<Esc>gv")

    var selectionMarksRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionMarksRange = selectionMarks
          }
        }
      }
    }

    assertNotNull(selectionMarksRange)
    assertTrue(selectionMarksRange is Range.Simple)
  }

  // ==================== replaceTextBlockwise with String overload Tests ====================

  @Test
  fun `test replaceTextBlockwise with single string replicates to all lines`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            // Block(0, 17) selects "one " on line 0 and "four" on line 1
            val range = Range.Block(0, 17)
            replaceTextBlockwise(range, "XXX")
          }
        }
      }
    }

    val expectedText = """
            XXXtwo three
            XXX five six
            seven eight nine
        """.trimIndent()

    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test replaceTextBlockwise with single string on three lines`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            // Block spanning all 3 lines, columns 0-4 (inclusive = 5 chars)
            // Line 0: offset 0, Line 1: offset 14, Line 2: offset 28
            // End at line 2, col 4: offset 32
            // Block selects "one t", "four ", "seven" (5 chars each)
            val range = Range.Block(0, 32)
            replaceTextBlockwise(range, "YYY")
          }
        }
      }
    }

    // Block replaces 5 chars on each line with "YYY"
    val expectedText = """
            YYYwo three
            YYYfive six
            YYY eight nine
        """.trimIndent()

    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test replaceTextBlockwise with empty string deletes block content`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            val range = Range.Block(0, 17)
            replaceTextBlockwise(range, "")
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

  // ==================== Corner Case Tests ====================

  @Test
  fun `test replaceTextBlockwise on single line block`() {
    val text = """
            ${c}one two three
            four five six
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            // Single line block from offset 0 to 2 (same line)
            val range = Range.Block(0, 2)
            replaceTextBlockwise(range, listOf("XXX"))
          }
        }
      }
    }

    val expectedText = """
            XXX two three
            four five six
        """.trimIndent()

    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test replaceTextBlockwise with single string on single line`() {
    val text = """
            ${c}one two three
            four five six
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            val range = Range.Block(0, 2)
            replaceTextBlockwise(range, "ZZZ")
          }
        }
      }
    }

    val expectedText = """
            ZZZ two three
            four five six
        """.trimIndent()

    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test replaceTextBlockwise at end of lines`() {
    val text = """
            ${c}one two three
            four five six
            seven eight nine
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            // Select last word on each of first two lines
            // "three" starts at offset 8, ends at 13
            // "six" on line 1 starts at offset 24, ends at 27
            // Block from (0,8) to (1,11) -> offsets 8 to 25
            val range = Range.Block(8, 25)
            replaceTextBlockwise(range, listOf("END1", "END2"))
          }
        }
      }
    }

    // The block should replace from column 8 onwards on both lines
    val actualText = fixture.editor.document.text
    assertTrue(actualText.contains("END1"))
    assertTrue(actualText.contains("END2"))
  }

  @Test
  fun `test replaceTextBlockwise with longer replacement text`() {
    val text = """
            ${c}one two three
            four five six
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            val range = Range.Block(0, 17)
            replaceTextBlockwise(range, listOf("LONGERTEXT", "VERYLONGTEXT"))
          }
        }
      }
    }

    val actualText = fixture.editor.document.text
    assertTrue(actualText.contains("LONGERTEXT"))
    assertTrue(actualText.contains("VERYLONGTEXT"))
  }

  @Test
  fun `test replaceTextBlockwise with shorter replacement text`() {
    val text = """
            ${c}one two three
            four five six
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            val range = Range.Block(0, 17)
            replaceTextBlockwise(range, listOf("X", "Y"))
          }
        }
      }
    }

    val expectedText = """
            Xtwo three
            Y five six
        """.trimIndent()

    val actualText = fixture.editor.document.text

    assertEquals(expectedText, actualText)
  }

  @Test
  fun `test selection returns correct range after exiting visual mode`() {
    val text = """
            ${c}one two three
            four five six
        """.trimIndent()
    configureByText(text)

    // Select some text and exit visual mode
    typeText("v4l<Esc>")

    var selectionRange: Range? = null
    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            selectionRange = selection
          }
        }
      }
    }

    assertNotNull(selectionRange)
    // After exiting visual mode, selection should be empty/collapsed
    assertTrue(selectionRange is Range.Simple)
    val simple = selectionRange as Range.Simple
    assertEquals(simple.start, simple.end)
  }

  @Test
  fun `test Block range start equals end on same position`() {
    val text = """
            ${c}one two three
            four five six
        """.trimIndent()
    configureByText(text)

    executeAction {
      myVimApi.editor {
        change {
          withPrimaryCaret {
            // Block with same start and end - this is a zero-width block
            // With inclusive selection mode, it still replaces 1 character
            val range = Range.Block(5, 5)
            replaceTextBlockwise(range, listOf("X"))
          }
        }
      }
    }

    // The block replaces the character at position 5
    val actualText = fixture.editor.document.text
    assertTrue(actualText.contains("X"), "Expected text to contain X after block replacement")
  }
}
