/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.regex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.mark.VimMark
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.regexp.VimRegex
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VimRegexEngineTest : VimTestCase() {
  private fun findAll(pattern: String): List<TextRange> {
    var result: List<TextRange>? = null
    ApplicationManager.getApplication().runReadAction {
      val regex = VimRegex(pattern)
      result = regex.findAll(fixture.editor.vim).map { it.range }
    }
    return result!!
  }

  @Test
  fun `test end of word at middle of text`() {
    configureByText("Lorem Ipsum")
    val result = findAll("Lorem\\>")
    assertEquals(result, listOf(TextRange(0, 5)))
  }

  @Test
  fun `test end of word should fail`() {
    configureByText("Lorem Ipsum")
    val result = findAll("Lo\\>rem")
    assertEquals(result, emptyList())
  }

  @Test
  fun `test start of word at offset`() {
    configureByText("Lorem Ipsum")
    val result = findAll("\\<Ipsum")
    assertEquals(result, listOf(TextRange(6, 11)))
  }

  @Test
  fun `test start of word should fail`() {
    configureByText("Lorem Ipsum")
    val result = findAll("Lo\\<rem")
    assertEquals(result, emptyList())
  }

  @Test
  fun `test end of word at end of text`() {
    configureByText("Lorem Ipsum")
    val result = findAll("Ipsum\\>")
    assertEquals(result, listOf(TextRange(6, 11)))
  }

  @Test
  fun `test start of word at start of text`() {
    configureByText("Lorem Ipsum")
    val result = findAll("\\<Lorem")
    assertEquals(result, listOf(TextRange(0, 5)))
  }

  @Test
  fun `test cursor and mark belong to the same cursor`() {
    /*
    In this test, there are two cursors, one at offset 3 and the other at 6.
    The second cursor (at offset 6) has a mark 'm' at offset 0.
    The pattern reads as "match the character at the cursor position that is after a mark 'm'".
    Since the cursor and mark tokens have to "belong" to the same cursor, the resulting match
    is at offset 6 (the offset of the second cursor), even though the first cursor appears first
    in the text.
    */
    configureByText("Lor${c}em ${c}Ipsum")
    val editor = fixture.editor.vim
    val mark = VimMark.create('m', 0, 0, editor.getPath(), editor.extractProtocol())!!
    ApplicationManager.getApplication().invokeAndWait {
      val secondCaret = editor.carets().maxByOrNull { it.offset }!!
      secondCaret.markStorage.setMark(mark)

      val result = findAll("\\%>'m\\%#.")
      assertEquals(result, listOf(TextRange(6, 7)))
    }
  }

  @Test
  fun `test text at mark position`() {
    configureByText("Lorem Ipsum")
    val editor = fixture.editor.vim
    val mark = VimMark.create('m', 0, 5, editor.getPath(), editor.extractProtocol())!!
    injector.markService.setMark(editor.primaryCaret(), mark)

    val result = findAll("\\%'m...")
    assertEquals(result, listOf(TextRange(5, 8)))
  }

  @Test
  fun `test text before mark position`() {
    configureByText("Lorem Ipsum")
    val editor = fixture.editor.vim
    val mark = VimMark.create('m', 0, 5, editor.getPath(), editor.extractProtocol())!!
    injector.markService.setMark(editor.primaryCaret(), mark)

    val result = findAll("\\%<'m...")
    assertEquals(result, listOf(TextRange(0, 3), TextRange(3, 6)))
  }

  @Test
  fun `test text after mark position`() {
    configureByText("Lorem Ipsum")
    val editor = fixture.editor.vim
    val mark = VimMark.create('m', 0, 5, editor.getPath(), editor.extractProtocol())!!
    injector.markService.setMark(editor.primaryCaret(), mark)

    val result = findAll("\\%>'m...")
    assertEquals(result, listOf(TextRange(6, 9)))
  }

  @Test
  fun `test text is inside visual area`() {
    configureByText("${c}Lorem Ipsum")
    typeText("v$")
    val result = findAll("\\%VLorem Ipsu\\%Vm")
    assertEquals(result, listOf(TextRange(0, 11)))
  }

  @Test
  fun `test text is not inside visual area`() {
    configureByText("${c}Lorem Ipsum")
    typeText("vw")
    val result = findAll("\\%VLorem Ipsu\\%Vm")
    assertEquals(result, emptyList())
  }

  @Test
  fun `test cursor and visual belong to the same cursor`() {
    configureByText("Lorem Ipsum")

    val caretModel = fixture.editor.caretModel
    typeText("v") // a workaround to trigger visual mode
    ApplicationManager.getApplication().invokeAndWait {
      caretModel.addCaret(VisualPosition(0, 2))
      val caret = caretModel.getCaretAt(VisualPosition(0, 2))!!
      caret.setSelection(0, 5)
      caretModel.addCaret(VisualPosition(0, 0))
      caretModel.addCaret(VisualPosition(0, 1))
      caretModel.addCaret(VisualPosition(0, 3))

      val result = findAll("\\%V.\\{-}\\%#.")
      assertEquals(result, listOf(TextRange(0, 3)))
    }
  }

}