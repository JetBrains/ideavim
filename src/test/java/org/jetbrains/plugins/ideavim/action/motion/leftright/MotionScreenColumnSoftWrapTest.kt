/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@TestWithoutNeovim(SkipNeovimReason.SEE_DESCRIPTION, "Soft wraps are set up through the IntelliJ test framework")
class MotionScreenColumnSoftWrapTest : VimTestCase() {
  private fun configureWrapped(text: String) {
    configureByText(text)
    ApplicationManager.getApplication().invokeAndWait {
      val charWidth = EditorUtil.getPlainSpaceWidth(fixture.editor)
      EditorTestUtil.configureSoftWraps(fixture.editor, 11 * charWidth + 1, 1000, charWidth, false)
    }
  }

  @Test
  fun `test g$ on first screen line of wrapped line`() {
    configureWrapped("01${c}23456789abcdefghijABCDE")
    typeText("g$")
    assertState("012345678${c}9abcdefghijABCDE")
  }

  @Test
  fun `test g$ on middle screen line of wrapped line`() {
    configureWrapped("0123456789ab${c}cdefghijABCDE")
    typeText("g$")
    assertState("0123456789abcdefgh${c}ijABCDE")
  }

  @Test
  fun `test g$ on last screen line of wrapped line`() {
    configureWrapped("0123456789abcdefghijA${c}BCDE")
    typeText("g$")
    assertState("0123456789abcdefghijABCD${c}E")
  }

  @Test
  fun `test g0 on middle screen line of wrapped line`() {
    configureWrapped("0123456789abcd${c}efghijABCDE")
    typeText("g0")
    assertState("0123456789${c}abcdefghijABCDE")
  }

  @Test
  fun `test g^ on middle screen line of wrapped line`() {
    configureWrapped("0123456789abcd${c}efghijABCDE")
    typeText("g^")
    assertState("0123456789${c}abcdefghijABCDE")
  }

  @Test
  fun `test g^ skips leading whitespace on first screen line of wrapped line`() {
    configureWrapped("    456${c}789abcdefghijABCDE")
    typeText("g^")
    assertState("    ${c}456789abcdefghijABCDE")
  }

  @Test
  fun `test dg$ on middle screen line of wrapped line`() {
    configureWrapped("0123456789ab${c}cdefghijABCDE")
    typeText("dg$")
    assertState("0123456789ab${c}jABCDE")
  }
}
