/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.openapi.ide.CopyPasteManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
import java.util.*

/**
 * @author Alex Plate
 */
class PutViaIdeaTest : VimTestCase() {

  private var optionsBefore: String = ""

  override fun setUp() {
    super.setUp()
    optionsBefore = optionsNoEditor().getStringValue(OptionConstants.clipboard)
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.clipboard, VimString("ideaput"))
  }

  override fun tearDown() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.clipboard, VimString(optionsBefore))
    super.tearDown()
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test simple insert via idea`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)

    injector.registerGroup.storeText('"', "legendary", SelectionType.CHARACTER_WISE)

    typeText(injector.parser.parseKeys("ve" + "p"))
    val after = "legendar${c}y it in a legendary land"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test insert several times`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)

    val vimEditor = myFixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)

    typeText(injector.parser.parseKeys("ppp"))
    val after = "Ilegendarylegendarylegendar${c}y found it in a legendary land"
    assertState(after)
  }

  fun `test insert doesn't clear existing elements`() {
    val randomUUID = UUID.randomUUID()
    val before = "${c}I found it in a legendary$randomUUID land"
    configureByText(before)

    CopyPasteManager.getInstance().setContents(TextBlockTransferable("Fill", emptyList(), null))
    CopyPasteManager.getInstance().setContents(TextBlockTransferable("Buffer", emptyList(), null))

    val vimEditor = myFixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary$randomUUID", SelectionType.CHARACTER_WISE, false)

    val sizeBefore = CopyPasteManager.getInstance().allContents.size
    typeText(injector.parser.parseKeys("ve" + "p"))
    assertEquals(sizeBefore, CopyPasteManager.getInstance().allContents.size)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test insert block with newline`() {
    val before = """
            A Discovery
            $c
            I found it in a legendary land
            
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)

    val vimEditor = myFixture.editor.vim
    VimPlugin.getRegister().storeText(
      vimEditor,
      vimEditor.primaryCaret(),
      before rangeOf "\nI found it in a legendary land\n",
      SelectionType.CHARACTER_WISE,
      false
    )

    typeText(injector.parser.parseKeys("p"))
    val after = """
            A Discovery
            
            I found it in a legendary land
            
            I found it in a legendary land
            
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }
}
