/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionOuterBlockBraceActionTest : VimTestCase() {
  @Test
  fun testOutside() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "${c}foo{bar}\n",
    )
    assertState("foo{}\n")
  }

  @Test
  fun testOutsideInString() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "\"1${c}23\"foo{bar}\n",
    )
    assertState("\"123\"foo{}\n")
  }

  @Test
  fun testOutsideInString2() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "\"1${c}23{dsa}d\"foo{bar}\n",
    )
    assertState("\"123{}d\"foo{bar}\n")
  }
}
