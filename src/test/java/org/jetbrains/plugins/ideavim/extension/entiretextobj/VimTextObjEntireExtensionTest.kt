/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.extension.entiretextobj

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.helper.experimentalApi
import org.jetbrains.plugins.ideavim.JavaVimTestCase
import java.util.*

/**
 * @author Alexandre Grison (@agrison)
 */
class VimTextObjEntireExtensionTest : JavaVimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("textobj-entire")
  }

  // |gU| |ae|
  fun testUpperCaseEntireBuffer() {
    doTest(injector.parser.parseKeys("gUae"), poem, "<caret>$poemUC")
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSelection(null)
  }

  // |gu| |ae|
  fun testLowerCaseEntireBuffer() {
    doTest(injector.parser.parseKeys("guae"), poem, "<caret>$poemLC")
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSelection(null)
  }

  // |c| |ae|
  fun testChangeEntireBuffer() {
    doTest(injector.parser.parseKeys("cae"), poem, "<caret>")
    assertMode(VimStateMachine.Mode.INSERT)
    assertSelection(null)
  }

  // |d| |ae|
  fun testDeleteEntireBuffer() {
    doTest(injector.parser.parseKeys("dae"), poem, "<caret>")
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSelection(null)
  }

  // |y| |ae|
  fun testYankEntireBuffer() {
    doTest(injector.parser.parseKeys("yae"), poem, "<caret>$poemNoCaret")
    assertMode(VimStateMachine.Mode.COMMAND)
    myFixture.checkResult(poemNoCaret)
    assertSelection(null)
  }

  // |gU| |ie|
  fun testUpperCaseEntireBufferIgnoreLeadingTrailing() {
    doTest(
      injector.parser.parseKeys("gUie"),
      "\n  \n \n${poem}\n \n  \n",
      "\n  \n \n<caret>${poemUC}\n \n  \n"
    )
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSelection(null)
  }

  // |gu| |ae|
  fun testLowerCaseEntireBufferIgnoreLeadingTrailing() {
    doTest(
      injector.parser.parseKeys("guie"),
      "\n  \n \n${poem}\n \n  \n",
      "\n  \n \n<caret>${poemLC}\n \n  \n"
    )
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSelection(null)
  }

  // |c| |ae|
  fun testChangeEntireBufferIgnoreLeadingTrailing() {
    doTest(
      injector.parser.parseKeys("cie"),
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>\n\n  \n \n"
    ) // additional \n because poem ends with a \n
    assertMode(VimStateMachine.Mode.INSERT)
    assertSelection(null)
  }

  @VimBehaviorDiffers(
    originalVimAfter =
    "\n  \n \n<caret>\n\n  \n \n",
    description = "Our code changes the motion type to linewise, but it should not"
  )
  // |d| |ae|
  fun testDeleteEntireBufferIgnoreLeadingTrailing() {
    doTest(
      injector.parser.parseKeys("die"),
      "\n  \n \n${poem}\n  \n \n",
      if (experimentalApi()) {
        "\n  \n \n<caret>\n  \n \n"
      } else {
        "\n  \n \n<caret>\n\n  \n \n"
      }
    ) // additional \n because poem ends with a \n
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSelection(null)
  }

  // |y| |ae|
  fun testYankEntireBufferIgnoreLeadingTrailing() {
    doTest(
      injector.parser.parseKeys("yie"),
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>${poemNoCaret}\n  \n \n"
    )
    assertMode(VimStateMachine.Mode.COMMAND)
    myFixture.checkResult("\n  \n \n${poemNoCaret}\n  \n \n")
    assertSelection(null)
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
