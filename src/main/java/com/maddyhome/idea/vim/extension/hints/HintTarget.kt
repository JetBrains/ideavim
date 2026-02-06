/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputEvent
import javax.accessibility.Accessible

internal enum class HintLabelPosition {
  TOP_LEFT_CORNER,
  CENTER,
}

internal data class HintTarget(val component: Accessible, val location: Point, val size: Dimension, val depth: Int) {
  var hint: String = ""
  var labelPosition: HintLabelPosition = HintLabelPosition.TOP_LEFT_CORNER

  val bounds: Rectangle get() = Rectangle(location, size)

  /**
   * The action to execute when the hint is selected.
   *
   * @return `true` if the action succeeded, `false` otherwise
   */
  var action: Function1<HintTarget, Boolean> = { false }
  fun action() = action(this)

  fun clickCenter(): Boolean {
    val robot = Robot()
    val locationOnScreen = component.accessibleContext?.accessibleComponent?.locationOnScreen ?: return false
    robot.mouseMove(locationOnScreen.x + bounds.width / 2, locationOnScreen.y + bounds.height / 2)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    return true
  }
}
