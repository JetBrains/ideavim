/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import org.jetbrains.plugins.ideavim.JavaVimTestCase

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
    doTest(StringHelper.parseKeys("gUae"), poem,"<caret>${poemUC}")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gu| |ae|
  fun testLowerCaseEntireBuffer() {
    doTest(StringHelper.parseKeys("guae"), poem, "<caret>${poemLC}")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |c| |ae|
  fun testChangeEntireBuffer() {
    doTest(StringHelper.parseKeys("cae"), poem, "<caret>")
    assertMode(CommandState.Mode.INSERT)
    assertSelection(null)
  }

  // |d| |ae|
  fun testDeleteEntireBuffer() {
    doTest(StringHelper.parseKeys("dae"), poem, "<caret>")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |y| |ae|
  fun testYankEntireBuffer() {
    doTest(StringHelper.parseKeys("yae"), poem, "<caret>${poemNoCaret}")
    assertMode(CommandState.Mode.COMMAND)
    myFixture.checkResult(poemNoCaret)
    assertSelection(null)
  }


  // |gU| |ie|
  fun testUpperCaseEntireBufferIgnoreLeadingTrailing() {
    doTest(StringHelper.parseKeys("gUie"),
      "\n  \n \n${poem}\n \n  \n",
      "\n  \n \n<caret>${poemUC}\n \n  \n")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |gu| |ae|
  fun testLowerCaseEntireBufferIgnoreLeadingTrailing() {
    doTest(StringHelper.parseKeys("guie"),
      "\n  \n \n${poem}\n \n  \n",
      "\n  \n \n<caret>${poemLC}\n \n  \n")
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |c| |ae|
  fun testChangeEntireBufferIgnoreLeadingTrailing() {
    doTest(StringHelper.parseKeys("cie"),
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>\n\n  \n \n") // additional \n because poem ends with a \n
    assertMode(CommandState.Mode.INSERT)
    assertSelection(null)
  }

  // |d| |ae|
  fun testDeleteEntireBufferIgnoreLeadingTrailing() {
    doTest(StringHelper.parseKeys("die"),
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>\n\n  \n \n") // additional \n because poem ends with a \n
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  // |y| |ae|
  fun testYankEntireBufferIgnoreLeadingTrailing() {
    doTest(StringHelper.parseKeys("yie"),
      "\n  \n \n${poem}\n  \n \n",
      "\n  \n \n<caret>${poemNoCaret}\n  \n \n")
    assertMode(CommandState.Mode.COMMAND)
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
  private val poemUC = poemNoCaret.toUpperCase()
  private val poemLC = poemNoCaret.toLowerCase()
}
