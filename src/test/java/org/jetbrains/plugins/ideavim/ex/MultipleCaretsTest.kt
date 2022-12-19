/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MultipleCaretsTest : VimTestCase() {
  fun testGotoToNthCharacter() {
    val before = "qwe rty a${c}sd\n fgh zx${c}c ${c}vbn"
    configureByText(before)
    typeText(commandToKeys("go 5"))
    val after = "qwe ${c}rty asd\n fgh zxc vbn"
    assertState(after)
  }

  fun testGotoLine() {
    val before = "qwe\n" + "rty\n" + "asd\n" + "f${c}gh\n" + "zxc\n" + "v${c}bn\n"
    configureByText(before)
    typeText(commandToKeys("2"))
    val after = "qwe\n" + "${c}rty\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    assertState(after)
  }

  fun testGotoLineInc() {
    val before = """
      qwe
      rt${c}y
      asd
      fgh
      zxc
      v${c}bn
      
    """.trimIndent()
    configureByText(before)
    typeText(commandToKeys("+2"))
    val after = """
      qwe
      rty
      asd
      ${c}fgh
      zxc
      vbn
      $c
    """.trimIndent()
    assertState(after)
  }

  fun testJoinLines() {
    val before = "qwe\n" + "r${c}ty\n" + "asd\n" + "fg${c}h\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("j"))
    val after = "qwe\nrty$c asd\nfgh$c zxc\nvbn\n"
    assertState(after)
  }

//  fun testJoinVisualLines() {
//    val before = "qwe\n" + "r${c}ty\n" + "asd\n" + "fg${c}h\n" + "zxc\n" + "vbn\n"
//    configureByText(before)
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys("j"))
//    val after = "qwe\n" + "rty${c} asd\n" + "fgh${c} zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

  fun testCopyText() {
    val before = "qwe\n" + "rty\n" + "a${c}sd\n" + "fg${c}h\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("co 2"))
    val after = "qwe\n" + "rty\n" + "${c}asd\n" + "${c}fgh\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    assertState(after)
  }

//  fun testCopyVisualText() {
//    val before = "qwe\n" + "${c}rty\n" + "asd\n" + "f${c}gh\n" + "zxc\n" + "vbn\n"
//    configureByText(before)
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys(":co 2"))
//    val after = "qwe\n" + "rty\n" + "${c}rty\n" + "asd\n" + "${c}fgh\n" + "zxc\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

  fun testPutText() {
    // This test produces double ${c}zxc on 3rd line if non-idea paste is used
    val before = """
          ${c}qwe
          rty
          ${c}as${c}d
          fgh
          zxc
          vbn

    """.trimIndent()
    val editor = configureByText(before)
    val vimEditor = editor.vim
    VimPlugin.getRegister().storeText(vimEditor, vimEditor.primaryCaret(), TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
    typeText(commandToKeys("pu"))
    val after = """
          qwe
          ${c}zxc
          rty
          asd
          ${c}zxc
          fgh
          zxc
          vbn

    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "register")
  fun testPutTextCertainLine() {
    // This test produces triple ${c}zxc if non-idea paste is used
    val before = """
          ${c}qwe
          rty
          ${c}as${c}d
          fgh
          zxc
          vbn

    """.trimIndent()
    val editor = configureByText(before)
    val vimEditor = editor.vim
    VimPlugin.getRegister().storeText(vimEditor, vimEditor.primaryCaret(), TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
    typeText(commandToKeys("4pu"))
    val after = """
          qwe
          rty
          asd
          fgh
          ${c}zxc
          zxc
          vbn

    """.trimIndent()
    assertState(after)
  }

//  fun testPutVisualLines() {
//    val before = "${c}qwe\n" + "rty\n" + "as${c}d\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    val editor = configureByText(before)
//    VimPlugin.getRegister().storeText(editor.vim, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
//
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys("pu"))
//
//    val after = "qwe\n" + "rty\n" + "${c}zxc\n" + "asd\n" + "fgh\n" + "${c}zxc\n" + "zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

//  fun testMoveTextBeforeCarets() {
//    val before = "qwe\n" + "rty\n" + "${c}asd\n" + "fgh\n" + "z${c}xc\n" + "vbn\n"
//    configureByText(before)
//    typeText(commandToKeys("m 1"))
//    val after = "qwe\n" + "${c}asd\n" + "${c}zxc\n" + "rty\n" + "fgh\n" + "vbn\n"
//    assertState(after)
//  }
//
//  fun testMoveTextAfterCarets() {
//    val before = "q${c}we\n" + "rty\n" + "${c}asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    configureByText(before)
//    typeText(commandToKeys("m 4"))
//    val after = "rty\n" + "fgh\n" + "zxc\n" + "${c}qwe\n" + "${c}asd\n" + "vbn\n"
//    assertState(after)
//  }
//
//  fun testMoveTextBetweenCarets() {
//    val before = "q${c}we\n" + "rty\n" + "${c}asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    configureByText(before)
//    typeText(commandToKeys("m 2"))
//    val after = "rty\n" + "${c}qwe\n" + "${c}asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    assertState(after)
//  }

  fun testYankLines() {
    val before = """qwe
      |rt${c}y
      |asd
      |${c}fgh
      |zxc
      |vbn
    """.trimMargin()
    configureByText(before)
    typeText(commandToKeys("y"))

    val lastRegister = VimPlugin.getRegister().lastRegister
    assertNotNull(lastRegister)
    val text = lastRegister!!.text
    assertNotNull(text)

    typeText(injector.parser.parseKeys("p"))
    val after = """qwe
      |rty
      |${c}rty
      |asd
      |fgh
      |${c}fgh
      |zxc
      |vbn
    """.trimMargin()
    assertState(after)
  }

  fun testDeleteLines() {
    val before = """qwe
      |r${c}ty
      |asd
      |f${c}gh
      |zxc
      |vbn
    """.trimMargin()

    configureByText(before)
    typeText(commandToKeys("d"))

    val lastRegister = VimPlugin.getRegister().lastRegister
    assertNotNull(lastRegister)
    val text = lastRegister!!.text
    assertNotNull(text)

    val after = """qwe
      |${c}asd
      |${c}zxc
      |vbn
    """.trimMargin()
    assertState(after)
  }

  fun testSortRangeWholeFile() {
    val before = """qwe
      |as${c}d
      |zxc
      |${c}rty
      |fgh
      |vbn
    """.trimMargin()
    configureByText(before)

    typeText(commandToKeys("sor"))

    val after = c + before.replace(c, "").split('\n').sorted().joinToString(separator = "\n")
    assertState(after)
  }

  fun testSortRange() {
    val before = """qwe
      |as${c}d
      | zxc
      |rty
      |f${c}gh
      |vbn
    """.trimMargin()
    configureByText(before)

    typeText(commandToKeys("2,4 sor"))

    val after = """qwe
      | ${c}zxc
      |asd
      |rty
      |fgh
      |vbn
    """.trimMargin()
    assertState(after)
  }

  fun testSortRangeReverse() {
    val before = """qwe
      |as${c}d
      |zxc
      |${c}rty
      |fgh
      |vbn
    """.trimMargin()
    configureByText(before)

    typeText(commandToKeys("sor!"))

    val after = c +
      before
        .replace(c, "")
        .split('\n')
        .sortedWith(reverseOrder())
        .joinToString(separator = "\n")
    assertState(after)
  }

  fun testSortRangeIgnoreCase() {
    val before = """qwe
      |as${c}d
      |   zxc
      |${c}Rty
      |fgh
      |vbn
    """.trimMargin()
    configureByText(before)

    typeText(commandToKeys("2,4 sor i"))

    val after = """qwe
      |   ${c}zxc
      |asd
      |Rty
      |fgh
      |vbn
    """.trimMargin()
    assertState(after)
  }
}
