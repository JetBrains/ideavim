/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.CaretStateTransferableData
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.testFramework.ExtensionTestUtil
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
import org.junit.jupiter.api.Test
import java.awt.datatransfer.Transferable

class PutTestAfterCursorActionTest : VimTestCase() {
  @Test
  fun `test platform handlers are called`() {
    injector.globalOptions().clipboard.prependValue(OptionConstants.clipboard_ideaput)

    val extension = TestExtension()
    ExtensionTestUtil.maskExtensions(
      CopyPastePostProcessor.EP_NAME,
      listOf(extension),
      fixture.testRootDisposable,
    )
    ExtensionTestUtil.maskExtensions(
      CopyPastePreProcessor.EP_NAME,
      listOf(),
      fixture.testRootDisposable,
    )
    setRegister('4', "XXX ")
    doTest(
      "\"4p",
      "This is my$c text",
      "This is my XXX$c text",
      Mode.NORMAL(),
    )
    kotlin.test.assertEquals(1, extension.calledExtractTransferableData)
  }

  @Test
  fun `test put from number register`() {
    setRegister('4', "XXX ")
    doTest(
      "\"4p",
      "This is my$c text",
      "This is my XXX$c text",
      Mode.NORMAL(),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """,
  )
  @Test
  fun `test put visual text line to last line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        before rangeOf "A Discovery\n",
        SelectionType.LINE_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery
            ${c}I found it in a legendary land
            GUARD
            I found it in a legendary land
            all rocks and lavender and tufted grass,
    """,
  )
  @Test
  fun `test put visual text line before Guard`() {
    val before = """
            A ${c}Discovery
            GUARD
            I found it in a legendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    val editor = configureByText(before)
    // Add Guard to simulate Notebook behaviour. See (VIM-2577)
    val guardRange = before rangeOf "\nGUARD\n"
    ApplicationManager.getApplication().runReadAction {
      editor.document.createGuardedBlock(guardRange.startOffset, guardRange.endOffset)
    }
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        before rangeOf "I found it in a legendary land\n",
        SelectionType.LINE_WISE,
        false,
      )
    }
    typeText(injector.parser.parseKeys("p"))
    val after = """
            A Discovery
            ${c}I found it in a legendary land
            
            GUARD
            I found it in a legendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test inserting same content to multiple carets`() {
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            ${c}all rocks and lavender and tufted grass,
            ${c}where it was settled on some sodden sand
            ${c}hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        before rangeOf "Discovery",
        SelectionType.CHARACTER_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("vep"))
    val after = """
            A Discovery

            Discovery it in a legendary land
            Discovery rocks and lavender and tufted grass,
            Discovery it was settled on some sodden sand
            Discovery by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  private class TestExtension : CopyPastePostProcessor<TextBlockTransferableData>() {
    var calledExtractTransferableData = 0
    override fun collectTransferableData(
      file: PsiFile,
      editor: Editor,
      startOffsets: IntArray,
      endOffsets: IntArray,
    ): List<TextBlockTransferableData> {
      return emptyList()
    }

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
      calledExtractTransferableData += 1
      return listOf(
        // Just some random data
        CaretStateTransferableData(intArrayOf(), intArrayOf()),
      )
    }
  }

  @Test
  fun `test undo after put after cursor`() {
    configureByText("Hello ${c}world")
    typeText("yy")
    typeText("p")
    assertState("""
      Hello world
      ${c}Hello world
      
    """.trimIndent())
    typeText("u")
    assertState("Hello ${c}world")
  }

  @Test
  fun `test undo after put character after cursor`() {
    configureByText("abc${c}def")
    typeText("yl")  // Yank 'd'
    typeText("h")   // Move left
    assertState("ab${c}cdef")
    typeText("p")
    assertState("abc${c}ddef")
    typeText("u")
    assertState("ab${c}cdef")
  }

  @Test
  fun `test undo after put word after cursor`() {
    configureByText("The ${c}quick brown fox")
    typeText("yiw")  // Yank "quick"
    typeText("w")    // Move to "brown"
    assertState("The quick ${c}brown fox")
    typeText("p")
    assertState("The quick bquic${c}krown fox")
    typeText("u")
    assertState("The quick ${c}brown fox")
  }

  @Test
  fun `test multiple undo after sequential puts after cursor`() {
    configureByText("${c}Hello")
    typeText("yy")
    typeText("p")
    assertState("""
      Hello
      ${c}Hello
      
    """.trimIndent())
    typeText("p")
    assertState("""
      Hello
      Hello
      ${c}Hello
      
    """.trimIndent())
    
    // Undo second put
    typeText("u")
    assertState("""
      Hello
      ${c}Hello
      
    """.trimIndent())
    
    // Undo first put
    typeText("u")
    assertState("""
      ${c}Hello
    """.trimIndent())
  }

  @Test
  fun `test undo put and move cursor`() {
    configureByText("${c}abc def")
    typeText("yiw")  // Yank "abc"
    typeText("w")    // Move to "def"
    assertState("abc ${c}def")
    typeText("gp")   // Put and move cursor after pasted text
    assertState("abc dabc${c}ef")
    typeText("u")
    assertState("abc ${c}def")
  }

  @Test
  fun `test undo put visual block after cursor`() {
    configureByText("""
      ${c}abc
      def
      ghi
    """.trimIndent())
    typeText("<C-V>jjl")  // Visual block select first 2 columns of all lines
    typeText("y")
    typeText("$")
    assertState("""
      ab${c}c
      def
      ghi
    """.trimIndent())
    typeText("p")
    assertState("""
      abc${c}ab
      defde
      ghigh
    """.trimIndent())
    typeText("u")
    assertState("""
      ab${c}c
      def
      ghi
    """.trimIndent())
  }
}
