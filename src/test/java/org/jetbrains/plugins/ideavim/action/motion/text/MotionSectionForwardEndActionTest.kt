/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.text

import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionSectionForwardEndActionTest : VimTestCase() {
  @VimBehaviorDiffers(originalVimAfter = c, description = "Full text is deleted")
  fun `test remove full text`() {
    doTest(
      "d][",
      """
          ${c}I found it in a legendary land
          all rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass.
      """.trimIndent(),
      "$c.",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @VimBehaviorDiffers(originalVimAfter = c, description = "Full text is deleted")
  fun `test remove full text with new line at the end`() {
    doTest(
      "d][",
      """
          ${c}I found it in a legendary land
          all rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass.
          
      """.trimIndent(),
      "$c.\n",
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test move forward`() {
    doTest(
      "][",
      """
      {
        {
        
        }
      $c}
      {
        {
        }
      }
      {
        {
        }
      }
      """.trimIndent(),
      """
      {
        {
        
        }
      }
      {
        {
        }
      $c}
      {
        {
        }
      }
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test move forward twice`() {
    doTest(
      "][][",
      """
      {
        {
        $c
        }
      }
      {
        {
        }
      }
      {
        {
        }
      }
      """.trimIndent(),
      """
      {
        {
        
        }
      }
      {
        {
        }
      $c}
      {
        {
        }
      }
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test move forward till the end`() {
    doTest(
      "][][][",
      """
      {
        {
        $c
        }
      }
      {
        {
        }
      }
      {
        {
        }
      }
      """.trimIndent(),
      """
      {
        {
        
        }
      }
      {
        {
        }
      }
      {
        {
        }
      $c}
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }
}
