/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ChangeRot13Test : VimTestCase() {
  @Test
  fun testChangeRot13MotionAction() {
    typeTextInFile(
      injector.parser.parseKeys("g?2w"),
      "H${c}ello World ${c}This is a ${c}test for ROT13 ${c}encoding\n",
    )
    assertState("H${c}ryyb Jbeyq ${c}Guvf vf a ${c}grfg sbe ROT13 ${c}rapbqvat\n")
  }

  @Test
  fun testChangeRot13VisualAction() {
    typeTextInFile(
      injector.parser.parseKeys("v2wg?"),
      "H${c}ello World ${c}This is a ${c}test for ROT13 ${c}encoding\n",
    )
    assertState("H${c}ryyb Jbeyq Guvf vf n ${c}grfg sbe EOT13 ${c}rapbqvat\n")
  }

  @Test
  fun testChangeRot13LineAction() {
    typeTextInFile(
      injector.parser.parseKeys("g??"),
      """
        H${c}ello World
        ${c}This is a test
        ${c}for ROT13 encoding

        """.trimIndent(),
    )
    assertState(
      """
        ${c}Uryyb Jbeyq
        ${c}Guvf vf n grfg
        ${c}sbe EBG13 rapbqvat

        """.trimIndent()
    )
  }

  @Disabled("Not yet supported")
  @Test
  fun testChangeRot13LineActionDuplicated() {
    typeTextInFile(
      injector.parser.parseKeys("g?g?"),
      """
        H${c}ello World
        ${c}This is a test
        ${c}for ROT13 encoding

        """.trimIndent(),
    )
    assertState(
      """
        ${c}Uryyb Jbeyq
        ${c}Guvf vf n grfg
        ${c}sbe EBG13 rapbqvat

        """.trimIndent()
    )
  }
  @Test
  fun testChangeRot13NonEnglishLetters() {
    typeTextInFile(
      injector.parser.parseKeys("g?$"),
      "${c}Привет мир! Hello world!\n",
    )
    assertState("${c}Привет мир! Uryyb jbeyq!\n")
  }

  @Test
  fun testChangeRot13FullAlphabet() {
    typeTextInFile(
      injector.parser.parseKeys("g?$"),
      "${c}abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\n",
    )
    assertState("${c}nopqrstuvwxyzabcdefghijklmNOPQRSTUVWXYZABCDEFGHIJKLM\n")
  }

  @Test
  fun testChangeRot13Symbols() {
    typeTextInFile(
      injector.parser.parseKeys("g?$"),
      "${c}!@#$%^&*()_+-=[]{}|;:'\",.<>/?\n",
    )
    assertState("${c}!@#$%^&*()_+-=[]{}|;:'\",.<>/?\n")
  }
}
