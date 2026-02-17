/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.windownavigation

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.ShortcutDispatcher
import com.maddyhome.idea.vim.group.WindowGroup
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

internal enum class NavDirection {
  LEFT, RIGHT, UP, DOWN;

  fun toNavigationParams(): Pair<Int, Boolean> = when (this) {
    LEFT -> -1 to false
    RIGHT -> 1 to false
    UP -> -1 to true
    DOWN -> 1 to true
  }
}

@Service
internal class ToolWindowNavDispatcher : ShortcutDispatcher<NavDirection>(
  PLUGIN_NAME,
  createMappings(),
  ToolWindowNavListener,
) {
  init {
    templatePresentation.isEnabledInModalContext = true
  }

  override fun update(e: AnActionEvent) {
    val keyEvent = e.inputEvent as? KeyEvent
    if (keyEvent == null) {
      e.presentation.isEnabled = false
      return
    }

    // Handle ESC - clear buffer
    if (keyEvent.keyCode == KeyEvent.VK_ESCAPE) {
      e.presentation.isEnabled = keyStrokes.isNotEmpty()
      keyStrokes.clear()
      return
    }

    if (keyStrokes.isEmpty()) {
      // Only activate for Ctrl+W to start a sequence.
      // Other registered keys (h, j, k, l, arrows) should pass through normally.
      e.presentation.isEnabled = isCtrlW(keyEvent)
    } else {
      // In the middle of a C-W sequence - accept the next key
      e.presentation.isEnabled = true
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    const val PLUGIN_NAME = "ToolWindowNav"

  }
}

private object ToolWindowNavListener : ShortcutDispatcher.Listener<NavDirection> {
  override fun onMatch(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>, data: NavDirection) {
    try {
      val project = e.project ?: return
      val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) ?: return

      val twComponent = findToolWindowComponent(component, project) ?: return
      if (!twComponent.isShowing) return

      val loc = twComponent.locationOnScreen
      val size = twComponent.size
      val currentBounds = Rectangle(loc, size)
      val refPoint = Point(loc.x + size.width / 2, loc.y + size.height / 2)

      val (relativePosition, vertical) = data.toNavigationParams()
      WindowGroup.navigateInDirection(project, refPoint, currentBounds, relativePosition, vertical)
    } finally {
      keyStrokes.clear()
    }
  }

  override fun onInvalid(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>) {
    keyStrokes.clear()
    injector.messages.indicateError()
  }

  private fun findToolWindowComponent(
    component: java.awt.Component,
    project: Project,
  ): JComponent? {
    val twm = ToolWindowManagerEx.getInstanceEx(project)
    for (id in twm.toolWindowIds) {
      val tw = twm.getToolWindow(id) ?: continue
      if (!tw.isVisible) continue
      if (tw.type == ToolWindowType.FLOATING || tw.type == ToolWindowType.WINDOWED) continue
      val twComp = tw.component ?: continue
      if (SwingUtilities.isDescendingFrom(component, twComp)) {
        return twComp
      }
    }
    return null
  }
}

private fun isCtrlW(keyEvent: KeyEvent): Boolean {
  return keyEvent.keyCode == KeyEvent.VK_W &&
    (keyEvent.modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0
}

private fun createMappings(): Map<List<KeyStroke>, NavDirection> {
  val mappings = mutableMapOf<List<KeyStroke>, NavDirection>()

  fun register(keys: String, direction: NavDirection) {
    mappings[injector.parser.parseKeys(keys)] = direction
  }

  register("<C-W>h", NavDirection.LEFT)
  register("<C-W><C-H>", NavDirection.LEFT)
  register("<C-W><Left>", NavDirection.LEFT)

  register("<C-W>j", NavDirection.DOWN)
  register("<C-W><C-J>", NavDirection.DOWN)
  register("<C-W><Down>", NavDirection.DOWN)

  register("<C-W>k", NavDirection.UP)
  register("<C-W><C-K>", NavDirection.UP)
  register("<C-W><Up>", NavDirection.UP)

  register("<C-W>l", NavDirection.RIGHT)
  register("<C-W><C-L>", NavDirection.RIGHT)
  register("<C-W><Right>", NavDirection.RIGHT)

  return mappings
}
