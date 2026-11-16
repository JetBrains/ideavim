/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import java.awt.event.MouseEvent

/**
 * Ctrl+Click on a symbol is the IDE's Go to Declaration bound to a mouse shortcut. It never goes through `<C-]>`, but
 * it is still a jump to a definition, so `<C-T>` must be able to return from it.
 */
class CtrlClickTagStackTest : VimJavaTestCase() {
  private val text = """
    class Foo {
        void target() {
        }

        void caller() {
            ${c}target();
        }
    }
  """.trimIndent()

  private val implementationText = """
    interface Runner {
        void run();
    }

    class Impl implements Runner {
        public void run() {
        }
    }

    class Foo {
        void caller(Runner r) {
            r.${c}run();
        }
    }
  """.trimIndent()

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t returns from ctrl-bracket`() {
    configureByJavaText(text)

    typeText("<C-]>")
    assertPosition(1, 9)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(5, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t returns from ctrl-click`() {
    configureByJavaText(text)

    ctrlClickGotoDeclaration()
    assertPosition(1, 9)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(5, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t after ctrl-click does not return to an older tag jump`() {
    configureByJavaText(
      """
      class Foo {
          void target() {
          }

          void other() {
          }

          void caller() {
              ${c}other();
              target();
          }
      }
      """.trimIndent(),
    )

    // An older tag jump that leaves an entry for line 8 on the stack
    typeText("<C-]>")
    assertPosition(4, 9)
    typeText("10G^")
    assertPosition(9, 8)

    ctrlClickGotoDeclaration()
    assertPosition(1, 9)

    // Back to where the Ctrl+Click was made, not to where the older <C-]> started
    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(9, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test navigating back does not push onto the tag stack`() {
    configureByJavaText(text)

    ctrlClickGotoDeclaration()
    assertPosition(1, 9)

    performIdeAction(IdeActions.ACTION_GOTO_BACK)
    assertPosition(5, 8)

    // Only the Ctrl+Click is on the stack, so the first <C-T> stays here and the second finds the bottom
    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(5, 8)

    typeText("<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E555: At bottom of tag stack")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test navigating forward does not push onto the tag stack`() {
    configureByJavaText(text)

    ctrlClickGotoDeclaration()
    assertPosition(1, 9)

    performIdeAction(IdeActions.ACTION_GOTO_BACK)
    assertPosition(5, 8)

    performIdeAction(IdeActions.ACTION_GOTO_FORWARD)
    assertPosition(1, 9)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(5, 8)

    typeText("<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E555: At bottom of tag stack")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-click from a place already on the tag stack is pushed again`() {
    configureByJavaText(
      """
      class Foo {
          void target() {
          }

          void other() {
          }

          void caller() {
              ${c}target();
              other();
          }
      }
      """.trimIndent(),
    )

    ctrlClickGotoDeclaration()
    assertPosition(1, 9)

    typeText("10G^")
    ctrlClickGotoDeclaration()
    assertPosition(4, 9)

    // The same place and the same tag as the first entry, but a new tag jump all the same
    typeText("9G^")
    ctrlClickGotoDeclaration()
    assertPosition(1, 9)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(8, 8)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(9, 8)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(8, 8)
  }

  // Ctrl+Click has two siblings that are also jumps to a definition: Ctrl+Shift+Click and Ctrl+Alt+Click

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t returns from ctrl-shift-click to the type declaration`() {
    configureByJavaText(
      """
      class Foo {
          void caller() {
              Bar bar = new Bar();
              ${c}bar.run();
          }
      }

      class Bar {
          void run() {
          }
      }
      """.trimIndent(),
    )

    clickWithShortcut(IdeActions.ACTION_GOTO_TYPE_DECLARATION, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)
    assertPosition(7, 6)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(3, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t returns from ctrl-alt-click to the implementation`() {
    configureByJavaText(implementationText)

    clickWithShortcut(IdeActions.ACTION_GOTO_IMPLEMENTATION, InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK)
    assertPosition(5, 16)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(11, 10)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t returns from goto declaration only`() {
    configureByJavaText(text)

    performIdeAction("GotoDeclarationOnly")
    assertPosition(1, 9)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(5, 8)
  }

  // A mapping to an IDE action is a user's own way of making the same jump, so it must be recorded too

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t returns from a mapping to goto declaration`() {
    configureByJavaText(text)
    enterCommand("nmap gb <Action>(GotoDeclaration)")

    typeText("gb")
    assertPosition(1, 9)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(5, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t returns from a mapping to goto implementation`() {
    configureByJavaText(implementationText)
    enterCommand("nmap gi <Action>(GotoImplementation)")

    typeText("gi")
    assertPosition(5, 16)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(11, 10)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t returns from the action command`() {
    configureByJavaText(text)

    enterCommand("action GotoDeclaration")
    assertPosition(1, 9)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(5, 8)
  }

  // The Vim commands that run the same IDE action must still push exactly what they pushed before

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-bracket pushes once`() {
    configureByJavaText(text)

    typeText("<C-]>")
    assertPosition(1, 9)

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(5, 8)

    typeText("<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E555: At bottom of tag stack")
  }

  /**
   * Runs Go to Declaration the way a Ctrl+Click does: from a mouse shortcut, not from IdeaVim
   */
  private fun ctrlClickGotoDeclaration() {
    clickWithShortcut(IdeActions.ACTION_GOTO_DECLARATION, InputEvent.CTRL_DOWN_MASK)
  }

  /**
   * Runs an IDE action the way a click with [modifiers] held does: from a mouse shortcut, not from IdeaVim
   */
  private fun clickWithShortcut(actionId: String, modifiers: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor
      val component = editor.contentComponent
      val point = editor.visualPositionToXY(editor.caretModel.visualPosition)
      @Suppress("MagicConstant")
      val click = MouseEvent(
        component,
        MouseEvent.MOUSE_RELEASED,
        System.currentTimeMillis(),
        modifiers or InputEvent.BUTTON1_DOWN_MASK,
        point.x,
        point.y,
        1,
        false,
        MouseEvent.BUTTON1,
      )
      execute(actionId, click, ActionPlaces.MOUSE_SHORTCUT)
    }
  }

  /**
   * Runs an IDE action the way its keyboard shortcut does, not from IdeaVim
   */
  private fun performIdeAction(actionId: String) {
    ApplicationManager.getApplication().invokeAndWait {
      execute(actionId, null, ActionPlaces.KEYBOARD_SHORTCUT)
    }
  }

  private fun execute(actionId: String, inputEvent: InputEvent?, place: String) {
    val action = ActionManager.getInstance().getAction(actionId)
    ActionManager.getInstance()
      .tryToExecute(action, inputEvent, fixture.editor.contentComponent, place, true)
      .waitFor(5_000)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
  }
}
