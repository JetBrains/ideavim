/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.entiretextobj

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.util.*

/**
 * @author Alexandre Grison (@agrison)
 */
@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimTextObjEntireExtensionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("textobj-entire")
  }

  // |gU| |ae|
  @Test
  fun testUpperCaseEntireBuffer() {
    doTest("gUae", poem, "<caret>$poemUC")
  }

  // |gu| |ae|
  @Test
  fun testLowerCaseEntireBuffer() {
    doTest("guae", poem, "<caret>$poemLC")
  }

  // |c| |ae|
  @Test
  fun testChangeEntireBuffer() {
    doTest("cae", poem, "<caret>", Mode.INSERT)
  }

  // |d| |ae|
  @Test
  fun testDeleteEntireBuffer() {
    doTest("dae", poem, "<caret>")
  }

  // |y| |ae|
  @Test
  fun testYankEntireBuffer() {
    doTest("yae", poem, "<caret>$poemNoCaret")
    assertRegisterString(injector.registerGroup.defaultRegister, poemNoCaret)
  }

  // |y| |ae|
  @Test
  fun testYankEntireBufferWithCustomRegister() {
    doTest("\"kyae", poem, "<caret>$poemNoCaret")
    assertRegisterString('k', poemNoCaret)
  }

  // |gU| |ie|
  @Test
  fun testUpperCaseEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "gUie",
      "\n  \n \n${poem}\n \n  \n",
      "\n  \n \n<caret>${poemUC}\n \n  \n",
    )
  }

  // |gu| |ae|
  @Test
  fun testLowerCaseEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "guie",
      "\n  \n \n${poem}\n \n  \n",
      "\n  \n \n<caret>${poemLC}\n \n  \n",
    )
  }

  // |c| |ae|
  @Test
  fun testChangeEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "cie",
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>\n\n  \n \n",
      Mode.INSERT,
    ) // additional \n because poem ends with a \n
  }

  // |d| |ae|
  @VimBehaviorDiffers(
    originalVimAfter =
      "\n  \n \n<caret>\n\n  \n \n",
    description = "Our code changes the motion type to linewise, but it should not",
  )
  @Test
  fun testDeleteEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "die",
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>\n\n  \n \n",
    ) // additional \n because poem ends with a \n
  }

  // |y| |ae|
  @Test
  fun testYankEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "yie",
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>${poemNoCaret}\n  \n \n",
    )
  }

  private val poem: String = """Two roads diverged in a yellow wood,
And sorry I could not travel both
And be one traveler, long I stood
And looked down one as far as I could
To where it bent in the undergrowth;

Then took the other, as just as fair,
And having perhaps the better claim,
Because it was grassy and wanted wear;
Though as for that the passing there
Had worn them really about the same,

And both that morning equally lay
In leaves no step had trodden black.
Oh, I kept the first for another day!
Yet knowing how way leads on to way,
<caret>I doubted if I should ever come back.

I shall be telling this with a sigh
Somewhere ages and ages hence:
Two roads diverged in a wood, and I—
I took the one less traveled by,
And that has made all the difference.
"""
  private val poemNoCaret = poem.replace("<caret>", "")
  private val poemUC = poemNoCaret.uppercase(Locale.getDefault())
  private val poemLC = poemNoCaret.lowercase(Locale.getDefault())
}
