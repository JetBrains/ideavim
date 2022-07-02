package org.jetbrains.plugins.ideavim.action.motion.text

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionParagraphNextActionTest : VimTestCase() {
  fun `test delete paragraph`() {
    doTest(
      "d}",
      """
        void foo() {
        }
        $c
        void bar() {
        }

        void baz() {
        }
      """.trimIndent(),
      """
        void foo() {
        }
        $c
        void baz() {
        }
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }
}
