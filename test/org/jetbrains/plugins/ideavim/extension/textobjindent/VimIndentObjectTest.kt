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
 * @author Shrikant Sharat Kandula (@sharat87)
 */
class VimIndentObjectTest : JavaVimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("textobj-indent")
  }

  // |d| |ii|
  fun testUpperCaseInsideIndent() {
    doTest(StringHelper.parseKeys("4Gdii"), pythonCode,"""
      import os
      
      def yell():
      
      
      if __name__ == "__main__":
          yell()
    """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  private val pythonCode: String = """
    import os
    
    def yell():
        print("hello")
        print(os.environ)
    
    
    if __name__ == "__main__":
        yell()
  """.trimIndent()

}
