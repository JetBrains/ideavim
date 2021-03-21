/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.extension.textobjindent

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import org.jetbrains.plugins.ideavim.JavaVimTestCase

/**
 * @author Shrikant Kandule (@sharat87)
 */
class VimIndentObjectTest : JavaVimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("textobj-indent")
  }

  // |gU| |ae|
  fun testUpperCaseInsideIndent() {
    doTest(StringHelper.parseKeys("4GgUii"), pythonCode,"""
      import os
      
      def yell():
          PRINT("HELLO")
          PRINT(OS.ENVIRON)
      
      
      if __name__ == "__main__":
          yell()
    """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
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

  private val pythonCode: String = """
    import os
    
    def yell():
        print("hello")
        print(os.environ)
    
    
    if __name__ == "__main__":
        yell()
  """.trimIndent()

  private val poemNoCaret = poem.replace("<caret>", "")
  private val poemUC = poemNoCaret.toUpperCase()
  private val poemLC = poemNoCaret.toLowerCase()
}
