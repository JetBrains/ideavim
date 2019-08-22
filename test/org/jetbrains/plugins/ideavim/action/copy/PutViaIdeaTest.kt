package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.openapi.ide.CopyPasteManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
import java.util.*

/**
 * @author Alex Plate
 */
class PutViaIdeaTest : VimTestCase() {

  var optionsBefore: String = ""

  override fun setUp() {
    super.setUp()
    optionsBefore = OptionsManager.clipboard.value
    OptionsManager.clipboard.set(ClipboardOptionsData.ideaput)
  }

  override fun tearDown() {
    super.tearDown()
    OptionsManager.clipboard.set(optionsBefore)
  }

  fun `test simple insert via idea`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)

    VimPlugin.getRegister().storeText(myFixture.editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)

    typeText(StringHelper.parseKeys("ve", "p"))
    val after = "legendar${c}y it in a legendary land"
    myFixture.checkResult(after)
  }

  fun `test insert several times`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)

    VimPlugin.getRegister().storeText(myFixture.editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)

    typeText(StringHelper.parseKeys("ppp"))
    val after = "Ilegendarylegendarylegendar${c}y found it in a legendary land"
    myFixture.checkResult(after)
  }

  fun `test insert doesn't clear existing elements`() {
    val randomUUID = UUID.randomUUID()
    val before = "${c}I found it in a legendary$randomUUID land"
    configureByText(before)

    CopyPasteManager.getInstance().setContents(TextBlockTransferable("Fill", emptyList(), null))
    CopyPasteManager.getInstance().setContents(TextBlockTransferable("Buffer", emptyList(), null))

    VimPlugin.getRegister().storeText(myFixture.editor, before rangeOf "legendary$randomUUID", SelectionType.CHARACTER_WISE, false)

    val sizeBefore = CopyPasteManager.getInstance().allContents.size
    typeText(StringHelper.parseKeys("ve", "p"))
    assertEquals(sizeBefore, CopyPasteManager.getInstance().allContents.size)
  }
}