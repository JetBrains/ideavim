/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.entiretextobj

import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import java.util.*

/**
 * @author Alexandre Grison (@agrison)
 */
@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimTextObjEntireExtensionTest : VimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("textobj-entire")
  }

  // |gU| |ae|
  fun testUpperCaseEntireBuffer() {
    doTest("gUae", poem, "<caret>$poemUC")
  }

  // |gu| |ae|
  fun testLowerCaseEntireBuffer() {
    doTest("guae", poem, "<caret>$poemLC")
  }

  // |c| |ae|
  fun testChangeEntireBuffer() {
    doTest("cae", poem, "<caret>", VimStateMachine.Mode.INSERT)
  }

  // |d| |ae|
  fun testDeleteEntireBuffer() {
    doTest("dae", poem, "<caret>")
  }

  // |y| |ae|
  fun testYankEntireBuffer() {
    doTest("yae", poem, "<caret>$poemNoCaret")
  }

  // |gU| |ie|
  fun testUpperCaseEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "gUie",
      "\n  \n \n${poem}\n \n  \n",
      "\n  \n \n<caret>${poemUC}\n \n  \n"
    )
  }

  // |gu| |ae|
  fun testLowerCaseEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "guie",
      "\n  \n \n${poem}\n \n  \n",
      "\n  \n \n<caret>${poemLC}\n \n  \n"
    )
  }

  // |c| |ae|
  fun testChangeEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "cie",
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>\n\n  \n \n",
      VimStateMachine.Mode.INSERT
    ) // additional \n because poem ends with a \n
  }

  @VimBehaviorDiffers(
    originalVimAfter =
    "\n  \n \n<caret>\n\n  \n \n",
    description = "Our code changes the motion type to linewise, but it should not"
  )
  // |d| |ae|
  fun testDeleteEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "die",
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>\n\n  \n \n"
    ) // additional \n because poem ends with a \n
  }

  // |y| |ae|
  fun testYankEntireBufferIgnoreLeadingTrailing() {
    doTest(
      "yie",
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>${poemNoCaret}\n  \n \n"
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
