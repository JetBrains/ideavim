/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.replacewithregister

import com.intellij.testFramework.UsefulTestCase.assertContainsElements
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.register.RegisterConstants.UNNAMED_REGISTER
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithPrimaryClipboard
import org.jetbrains.plugins.ideavim.rangeOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

class ReplaceWithRegisterTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("ReplaceWithRegister")
  }

  @Test
  fun `test replace with empty register`() {
    val text = "one ${c}two three"
    VimPlugin.getRegister().resetRegisters()

    configureByText(text)
    typeText(injector.parser.parseKeys("griw"))
    assertState(text)
  }

  @Test
  fun `test simple replace`() {
    val text = "one ${c}two three"

    configureByText(text)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), text rangeOf "one", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("griw"))
    assertState("one on${c}e three")
    assertEquals("one", VimPlugin.getRegister().lastRegister?.text)
  }

  @Test
  fun `test empty text`() {
    val text = ""

    configureByText(text)
    VimPlugin.getRegister().storeTextSpecial(UNNAMED_REGISTER, "one")
    typeText(injector.parser.parseKeys("griw"))
    assertState("on${c}e")
  }

  @Test
  fun `test replace with empty text`() {
    val text = "${c}one"

    configureByText(text)
    VimPlugin.getRegister().storeTextSpecial(UNNAMED_REGISTER, "")
    typeText(injector.parser.parseKeys("griw"))
    assertState(c)
  }

  @Test
  fun `test replace use different register`() {
    val text = "one ${c}two three four"

    configureByText(text)
    typeText(injector.parser.parseKeys("\"ayiw" + "w" + "\"agriw"))
    assertState("one two tw${c}o four")
    assertEquals("two", VimPlugin.getRegister().lastRegister?.text)
    typeText(injector.parser.parseKeys("w" + "griw"))
    assertState("one two two tw${c}o")
    assertEquals("two", VimPlugin.getRegister().lastRegister?.text)
  }

  @Test
  fun `test replace use clipboard register`() {
    val text = "one ${c}two three four"

    configureByText(text)
    typeText(injector.parser.parseKeys("\"+yiw" + "w" + "\"+griw" + "w" + "\"+griw"))
    assertState("one two two tw${c}o")
    assertEquals("two", VimPlugin.getRegister().lastRegister?.text)
  }

  @Test
  fun `test replace use wrong register`() {
    val text = "one ${c}two three"

    configureByText(text)
    typeText(injector.parser.parseKeys("\"ayiw" + "\"bgriw"))
    assertState(text)
  }

  @Test
  fun `test replace with line`() {
    val text = """
            |I fou${c}nd it in a legendary land|
            all rocks and lavender and tufted grass,
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("yy" + "j" + "griw"))
    assertState(
      """
            |I found it in a legendary land|
            all |I found it in a legendary land$c| and lavender and tufted grass,
      """.trimIndent(),
    )
  }

  @Test
  fun `test replace with line with clipboard register`() {
    val text = """
            |I fou${c}nd it in a legendary land|
            all rocks and lavender and tufted grass,
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("\"+yy" + "j" + "\"+griw"))
    assertState(
      """
            |I found it in a legendary land|
            all |I found it in a legendary land$c| and lavender and tufted grass,
      """.trimIndent(),
    )
  }

  @Test
  fun `test replace block selection`() {
    val text = """
            ${c}one two three
            one two three
            one two three
            one two three
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("<C-v>jjlly" + "gg^w" + "griw"))
    assertState(
      """
            one ${c}one three
            one onetwo three
            one onetwo three
            one two three
      """.trimIndent(),
    )
  }

  @Test
  fun `test replace with number`() {
    val text = "one ${c}two three four"

    configureByText(text)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), text rangeOf "one", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("3griw"))
    assertState("one on${c}e four")
    assertEquals("one", VimPlugin.getRegister().lastRegister?.text)
  }

  @VimBehaviorDiffers("one on${c}e on${c}e four")
  @Test
  fun `test replace with multiple carets`() {
    val text = "one ${c}two ${c}three four"

    configureByText(text)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), text rangeOf "one", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("griw"))
    assertState("one two one four")
    assertEquals("one", VimPlugin.getRegister().lastRegister?.text)
  }

  @Test
  fun `test dot repeat`() {
    val text = "one ${c}two three four"

    configureByText(text)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), text rangeOf "one", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("griw" + "w" + "."))
    assertState("one one on${c}e four")
    assertEquals("one", VimPlugin.getRegister().lastRegister?.text)
  }

  // --------------------------------------- grr --------------------------

  @Test
  fun `test line replace`() {
    val text = """
            I found it in ${c}a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()

    configureByText(text)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), text rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("grr"))
    assertState(
      """
            ${c}legendary
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertEquals("legendary", VimPlugin.getRegister().lastRegister?.text)
  }

  @Test
  fun `test line replace with line`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("yyj" + "grr"))
    assertState(
      """
            I found it in a legendary land
            ${c}I found it in a legendary land
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }

  @Test
  fun `test line replace with line empty line`() {
    val text = """
            I found it in ${c}a legendary land
            
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("yyj" + "grr"))
    assertState(
      """
            I found it in a legendary land
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }

  @VimBehaviorDiffers(description = "Where is the new line comes from?...")
  @Test
  fun `test line replace with block`() {
    val text = """
            ${c}one two three
            one two three
            one two three
            one two three
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("<C-V>lljjyj" + "grr"))
    assertState(
      """
            one two three
            ${c}one
            one
            one
            one two three
            one two three
            
      """.trimIndent(),
    )
  }

  @Test
  fun `test line with number`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("yyj" + "2grr"))
    assertState(
      """
            I found it in a legendary land
            ${c}I found it in a legendary land
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }

  @Test
  fun `test line dot repeat`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("yyj" + "grr" + "j" + "."))
    assertState(
      """
            I found it in a legendary land
            I found it in a legendary land
            ${c}I found it in a legendary land
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }

  @VimBehaviorDiffers(
    """
            Lorem ipsum dolor sit amet,
            ${c}Lorem ipsum dolor sit amet,
            Sed in orci mauris.
            ${c}Sed in orci mauris.
  """,
  )
  @Test
  fun `test line multicaret`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was s${c}ettled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("yyj" + "grr"))
    assertState(
      """
            I found it in a legendary land
            I found it in a legendary land
            where it was settled on some sodden sand
            where it was settled on some sodden sand
            
      """.trimIndent(),
    )
  }

  // ------------------------------------- gr + visual ----------------------

  @Test
  fun `test visual replace`() {
    val text = """
            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()

    configureByText(text)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), text rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("viw" + "gr"))
    assertState(
      """
            I legendar${c}y it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertEquals("legendary", VimPlugin.getRegister().lastRegister?.text)
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test visual replace with line`() {
    val text = """
            |I fo${c}und it in a legendary land|
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("yyj" + "viw" + "gr"))
    assertState(
      """
            |I found it in a legendary land|
            all |I found it in a legendary land$c| and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test visual replace with two lines`() {
    val text = """
            |I found it in ${c}a legendary land|
            |all rocks and lavender and tufted grass,|
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("Vjyjj3w" + "viw" + "gr"))
    assertState(
      """
            |I found it in a legendary land|
            |all rocks and lavender and tufted grass,|
            where it was |I found it in a legendary land|
            |all rocks and lavender and tufted grass,$c| on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test visual line replace`() {
    val text = """
            I fo${c}und it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()

    configureByText(text)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), text rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("V" + "gr"))
    assertState(
      """
            ${c}legendary
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test visual line replace with line`() {
    val text = """
            I fo${c}und it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(injector.parser.parseKeys("yyj" + "V" + "gr"))
    assertState(
      """
            I found it in a legendary land
            ${c}I found it in a legendary land
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
  }

  //  https://youtrack.jetbrains.com/issue/VIM-2881/ReplaceRegister-does-no-longer-worker-with-MultiCursor
  @Test
  fun `test multiple carets`() {
    // Behaviour of pasting a full line with multiple carets is undefined in Vim and has different implementation in
    // IdeaVim depending on if ideaput is specified in 'clipboard' or not
    assertContainsElements(optionsNoEditor().clipboard, OptionConstants.clipboard_ideaput)

    enableExtensions("multiple-cursors")
    val text = """
      ${c}copyMe
      selectMe
      selectMe
      selectMe
      selectMe
    """.trimIndent()
    configureByText(text)
    enterCommand("set clipboard+=unnamedplus")
    typeText(injector.parser.parseKeys("ye" + "j" + "<A-n><A-n><A-n><A-n>" + "gr"))
    assertState(
      """
        copyMe
        copyMe
        copyMe
        copyMe
        copyMe
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test replace in visual with clipboard unnamed`() {
    VimPlugin.getRegister().resetRegisters()

    configureByText("""
      ${c}test
      copyMe

      selectMe
      selectMe
      selectMe
      selectMe
    """.trimIndent())
    enableExtensions("multiple-cursors")
    enterCommand("set clipboard+=unnamed")

    typeText(injector.parser.parseKeys("yiw" + "j" + "griw" + "jj" + "<A-n><A-n><A-n><A-n>" + "gr"))
    assertState("""
      test
      test

      test
      test
      test
      test
    """.trimIndent())

    enterCommand("set clipboard&")
  }

  @TestWithPrimaryClipboard
  fun `test replace in visual with clipboard unnamed linux`() {
    VimPlugin.getRegister().resetRegisters()

    configureByText("one ${c}two three")
    enterCommand("set clipboard+=unnamed")
    injector.registerGroup.storeText('*', "four")

    typeText(injector.parser.parseKeys("vegr"))
    assertState("one two three")
    enterCommand("set clipboard&")
  }
}
