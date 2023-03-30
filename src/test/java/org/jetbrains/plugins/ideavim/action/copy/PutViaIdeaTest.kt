/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.openapi.ide.CopyPasteManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption
import org.jetbrains.plugins.ideavim.rangeOf
import java.awt.datatransfer.StringSelection
import java.util.*

/**
 * @author Alex Plate
 */
@TraceOptions(TestOptionConstants.clipboard)
class PutViaIdeaTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [OptionConstants.clipboard_ideaput]))
  fun `test simple insert via idea`() {
    val before = "${c}Lorem ipsum dolor sit amet,"
    configureByText(before)

    injector.registerGroup.storeText('"', "legendary", SelectionType.CHARACTER_WISE)

    typeText("ve", "p")
    val after = "legendar${c}y ipsum dolor sit amet,"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [OptionConstants.clipboard_ideaput]))
  fun `test insert several times`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)

    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)

    typeText("ppp")
    val after = "Ilegendarylegendarylegendar${c}y found it in a legendary land"
    assertState(after)
  }

  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [OptionConstants.clipboard_ideaput]))
  fun `test insert doesn't clear existing elements`() {
    val randomUUID = UUID.randomUUID()
    val before = "${c}I found it in a legendary$randomUUID land"
    configureByText(before)

    CopyPasteManager.getInstance().setContents(TextBlockTransferable("Fill", emptyList(), null))
    CopyPasteManager.getInstance().setContents(TextBlockTransferable("Buffer", emptyList(), null))

    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(
        vimEditor,
        vimEditor.primaryCaret(),
        before rangeOf "legendary$randomUUID",
        SelectionType.CHARACTER_WISE,
        false,
      )

    val sizeBefore = CopyPasteManager.getInstance().allContents.size
    typeText("ve", "p")
    kotlin.test.assertEquals(sizeBefore, CopyPasteManager.getInstance().allContents.size)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [OptionConstants.clipboard_ideaput]))
  fun `test insert block with newline`() {
    val before = """
            Lorem Ipsum
            $c
            Lorem ipsum dolor sit amet,
            
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(before)

    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister().storeText(
      vimEditor,
      vimEditor.primaryCaret(),
      before rangeOf "\nLorem ipsum dolor sit amet,\n",
      SelectionType.CHARACTER_WISE,
      false,
    )

    typeText("p")
    val after = """
            Lorem Ipsum
            
            Lorem ipsum dolor sit amet,
            
            Lorem ipsum dolor sit amet,
            
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [OptionConstants.clipboard_ideaput]))
  fun `test insert block w1ith newline`() {
    val before = """
            A Discovery
            $c
            I found it in a legendary land
            
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)

    // For this particular test, we want to set exact this type of transferable
    CopyPasteManagerEx.getInstance().setContents(StringSelection("Hello"))

    typeText("\"+p", "\"+p")
    val after = """
            A Discovery
            HelloHello
            I found it in a legendary land
            
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }
}
