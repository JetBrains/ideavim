/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithoutPrimaryClipboard
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */

class PutVisualTextMoveCursorActionTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        TextRange(16, 25),
        SelectionType.CHARACTER_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("v2e" + "2gp"))
    val after = "legendarylegendary$c in a legendary land"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        TextRange(16, 25),
        SelectionType.LINE_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("v2e" + "gp"))
    val after = """

            legendary
            $c in a legendary land
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line linewise`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        TextRange(16, 25),
        SelectionType.CHARACTER_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("V" + "gp"))
    val after = "legendary\n$c"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test replace row`() {
    val file = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val newFile = """
            A Discovery

            Discovery
            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(file)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        TextRange(2, 11),
        SelectionType.LINE_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("V" + "gp"))
    assertState(newFile)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ound it in a legendary land
             rocks and lavender and tufted grass,
            re it was settled on some sodden sand
            d by the torrent of a mountain pass.
            ${c}A Discovery
    """,
  )
  @Test
  fun `test put line in block selection`() {
    val file = """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val newFile = """
            A Discovery

            ound it in a legendary land
             rocks and lavender and tufted grass,
            re it was settled on some sodden sand
            d by the torrent of a mountain pass.
            A Discovery
            $c
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("Y" + "2j" + "<C-v>" + "2l" + "3j" + "gp"), file)
    assertState(newFile)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test Put visual text linewise`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        TextRange(16, 25),
        SelectionType.LINE_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("v2e" + "gP"))
    val after = """

            legendary
            $c in a legendary land
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test Put visual text`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        TextRange(16, 25),
        SelectionType.CHARACTER_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("v2e" + "2gP"))
    val after = "legendarylegendary$c in a legendary land"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test Put visual text full line`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        TextRange(16, 25),
        SelectionType.CHARACTER_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("v$" + "2gP"))
    val after = "legendarylegendar${c}y"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test Put visual text line linewise`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        TextRange(16, 25),
        SelectionType.CHARACTER_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("V" + "gP"))
    val after = "legendary\n$c"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  @Test
  fun `test Put line in block selection`() {
    val file = """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val newFile = """
            A Discovery

            A Discovery
            ${c}ound it in a legendary land
             rocks and lavender and tufted grass,
            re it was settled on some sodden sand
            d by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("Y" + "2j" + "<C-v>" + "2l" + "3j" + "gP"), file)
    assertState(newFile)
  }

  // Legacy tests
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text linewise multicaret`() {
    val before = """
            q${c}werty
            as${c}dfgh
            ${c}zxcvbn

    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "zxcvbn\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("vl" + "\"+gp"))
    val after = """
            q
            zxcvbn
            ${c}rty
            as
            zxcvbn
            ${c}gh

            zxcvbn
            ${c}cvbn

    """.trimIndent()
    assertState(after)
  }

  // Legacy tests
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise multicaret clipboard register`() {
    val before = """
            q${c}werty
            as${c}dfgh
            ${c}zxcvbn

    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "zxcvbn\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("vl" + "\"+gp"))
    val after = """
            q
            zxcvbn
            ${c}rty
            as
            zxcvbn
            ${c}gh

            zxcvbn
            ${c}cvbn

    """.trimIndent()
    assertState(after)
  }

  @Disabled
  @Test
  fun `test put visual block visual line mode`() {
    val before = """
            qw${c}e
            asd
            zxc
            rty
            fgh
            vbn
    """.trimIndent()
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      TextRange(16, 19),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<S-v>" + "gp"))
    val after = """
            ${c}fgh
            asd
            zxc
            rty
            fgh
            vbn
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual block linewise`() {
    val before = """
            qw${c}e
            asd
            zxc
            rty
            fgh
            vbn
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
        TextRange(16, 19),
        SelectionType.LINE_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("<C-v>" + "h" + "gp"))
    val after = """
            q
            fgh
            $c
            asd
            zxc
            rty
            fgh
            vbn
    """.trimIndent()
    assertState(after)
  }

  @Test
  @Disabled
  fun `test put visual text multicaret`() {
    val before = "${c}qwe asd ${c}zxc rty ${c}fgh vbn"
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      TextRange(16, 19),
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("v2e" + "2gp"))
    val after = "fghfgh$c fghfgh$c fghfgh$c"
    assertState(after)
  }
}
