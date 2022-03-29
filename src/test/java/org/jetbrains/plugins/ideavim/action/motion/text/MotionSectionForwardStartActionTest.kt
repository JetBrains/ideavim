package org.jetbrains.plugins.ideavim.action.motion.text

import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionSectionForwardStartActionTest : VimTestCase() {
  fun `test move forward`() {
    doTest(
      "]]",
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
      $c{
        {
        }
      }
      {
        {
        }
      }
    """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test move forward twice`() {
    doTest(
      "]]]]",
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
      $c{
        {
        }
      }
    """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test move forward till the end`() {
    doTest(
      "]]]]]]",
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
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }
}