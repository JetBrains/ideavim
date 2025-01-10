/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */
class YankVisualLinesActionTest : VimTestCase() {
  @Test
  fun `test from visual mode`() {
    val text = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val yankedTest = """
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            
    """.trimIndent()
    configureByText(text)
    typeText(injector.parser.parseKeys("vjY"))
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val savedText =
      registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text ?: kotlin.test.fail()
    kotlin.test.assertEquals(yankedTest, savedText)
  }

  @Test
  fun `test from visual mode till the end`() {
    val text = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was sett${c}led on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val textAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest("vjY", text, textAfter, Mode.NORMAL())
    val yankedTest = """
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            
    """.trimIndent()
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val savedText =
      registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text ?: kotlin.test.fail()
    kotlin.test.assertEquals(yankedTest, savedText)
  }

  @Test
  fun `test from line visual mode`() {
    val text = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val yankedTest = """
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            
    """.trimIndent()
    configureByText(text)
    typeText(injector.parser.parseKeys("VjY"))
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val savedText =
      registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text ?: kotlin.test.fail()
    kotlin.test.assertEquals(yankedTest, savedText)
  }
}
