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
import javax.swing.JPanel

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

  fun createCover(containerSize: Dimension) = JPanel(null).apply {
    isOpaque = false
    val pill = RoundedHintLabel(hint)
    bounds = coverBounds(pill.preferredSize, containerSize)
    pill.bounds = pillBounds(pill.preferredSize, bounds.size)
    add(pill)
  }

  /**
   * The cover panel must be large enough to fit both the target highlight area
   * and the pill label without clipping.
   * Its position is clamped so the pill never exceeds the window.
   */
  private fun coverBounds(pillSize: Dimension, containerSize: Dimension): Rectangle {
    val w = maxOf(bounds.width, pillSize.width)
    val h = maxOf(bounds.height, pillSize.height)
    val x = bounds.x.coerceIn(0, maxOf(0, containerSize.width - w))
    val y = bounds.y.coerceIn(0, maxOf(0, containerSize.height - h))
    return Rectangle(x, y, w, h)
  }

  /**
   * Position the pill within the cover panel according to [labelPosition].
   * TOP_LEFT_CORNER anchors at (0, 0); CENTER places it in the middle.
   */
  private fun pillBounds(pillSize: Dimension, coverSize: Dimension): Rectangle = when (labelPosition) {
    HintLabelPosition.TOP_LEFT_CORNER -> Rectangle(0, 0, pillSize.width, pillSize.height)
    HintLabelPosition.CENTER -> Rectangle(
      (coverSize.width - pillSize.width) / 2,
      (coverSize.height - pillSize.height) / 2,
      pillSize.width,
      pillSize.height,
    )
  }

  fun clickCenter(): Boolean {
    val robot = Robot()
    val locationOnScreen = component.accessibleContext?.accessibleComponent?.locationOnScreen ?: return false
    robot.mouseMove(locationOnScreen.x + bounds.width / 2, locationOnScreen.y + bounds.height / 2)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    return true
  }
}
