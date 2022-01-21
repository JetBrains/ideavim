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

package org.jetbrains.plugins.ideavim.action.motion.text

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionUnmatchedBraceOpenActionTest : VimTestCase() {
  fun `test go to bracket`() {
    doTest(
      "[{",
      """
      int main() {
        $c
      }
      """.trimIndent(),
      """
      int main() $c{
        
      }
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test go to next bracket`() {
    doTest(
      "[{",
      """
      class Xxx {
        int main() {
          $c
        }
      }
      """.trimIndent(),
      """
      class Xxx {
        int main() $c{
          
        }
      }
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test go to next next bracket`() {
    doTest(
      "[{[{",
      """
      class Xxx {
        int main() {
          $c
        }
      }
      """.trimIndent(),
      """
      class Xxx $c{
        int main() {
          
        }
      }
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test go to next next bracket with count`() {
    doTest(
      "2[{",
      """
      class Xxx {
        int main() {
          $c
        }
      }
      """.trimIndent(),
      """
      class Xxx $c{
        int main() {
          
        }
      }
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
      class Xxx $c{
        int main() {
          
        }
      }
  """
  )
  fun `test go to next next bracket with great count`() {
    doTest(
      "5[{",
      """
      class Xxx {
        int main() {
          $c
        }
      }
      """.trimIndent(),
      """
      class Xxx {
        int main() {
          $c
        }
      }
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }
}
