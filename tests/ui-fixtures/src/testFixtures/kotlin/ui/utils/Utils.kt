/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.utils

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.Fixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import org.assertj.swing.core.MouseButton
import ui.pages.Editor
import java.awt.Point

fun RemoteText.doubleClickOnRight(shiftX: Int, fixture: Fixture, button: MouseButton = MouseButton.LEFT_BUTTON) {
  fixture.runJs("""
    const updatedPoint = new Point(${this.point.x + shiftX}, ${this.point.y});
    robot.click(component, updatedPoint, MouseButton.$button, 2)
  """.trimIndent())
}

fun RemoteText.tripleClickOnRight(shiftX: Int, fixture: Fixture, button: MouseButton = MouseButton.LEFT_BUTTON) {
  fixture.runJs("""
    const updatedPoint = new Point(${this.point.x + shiftX}, ${this.point.y});
    robot.click(component, updatedPoint, MouseButton.$button, 3)
  """.trimIndent())
}

fun RemoteText.moveMouseTo(goal: RemoteText, editor: Editor): Boolean {
  this.moveMouse()
  editor.runJs("robot.pressMouse(MouseButton.LEFT_BUTTON)")
  goal.moveMouse()
  val caretDuringDragging = false/*editor.isBlockCursor*/
  editor.runJs("robot.releaseMouse(MouseButton.LEFT_BUTTON)")
//  waitFor { editor.isBlockCursor }
  return caretDuringDragging
}

fun RemoteText.moveMouseWithDelayTo(goal: RemoteText, editor: Editor, delay: Long = 1000): Boolean {
  this.moveMouse()
  editor.runJs("robot.pressMouse(MouseButton.LEFT_BUTTON)")
  goal.moveMouse()
  Thread.sleep(delay)
  val caretDuringDragging = false/*editor.isBlockCursor*/
  editor.runJs("robot.releaseMouse(MouseButton.LEFT_BUTTON)")
//  waitFor { editor.isBlockCursor }
  return caretDuringDragging
}

fun RemoteText.moveMouseInGutterTo(goal: RemoteText, fixture: Fixture) {
  this.moveMouse()
  val goalPoint = goal.point

  fixture.runJs(
    """
    const point = new java.awt.Point(${goalPoint.x}, ${goalPoint.y});
    robot.pressMouseWhileRunning(MouseButton.LEFT_BUTTON, () => {
      robot.moveMouse(component, point)
    })
    """,
  )
}

fun Point.moveMouseTo(point: Point, fixture: Fixture) {
  val _point = this
  fixture.runJs(
    """
    const point = new java.awt.Point(${_point.x}, ${_point.y});
    robot.moveMouse(component, point)
  """.trimIndent()
  )

  fixture.runJs(
    """
    const point = new java.awt.Point(${point.x}, ${point.y});
    robot.pressMouseWhileRunning(MouseButton.LEFT_BUTTON, () => {
      robot.moveMouse(component, point)
    })
    """,
  )
}

fun RemoteText.moveMouseForthAndBack(middle: RemoteText, editor: Editor) {
  this.moveMouse()
  val initialPoint = this.point
  val middlePoint = middle.point

  editor.runJs(
    """
    const initialPoint = new Point(${initialPoint.x}, ${initialPoint.y});
    const point = new Point(${middlePoint.x}, ${middlePoint.y});
    robot.pressMouseWhileRunning(MouseButton.LEFT_BUTTON, () => {
      robot.moveMouse(component, point)
      robot.moveMouse(component, initialPoint)
    })
    """,
  )
//  waitFor { editor.isBlockCursor }
}

fun String.escape(): String = this.replace("\n", "\\n")

fun RemoteRobot.invokeActionJs(actionId: String) {
  runJs(
    """
            const actionId = "$actionId";
            const actionManager = com.intellij.openapi.actionSystem.ActionManager.getInstance();
            const action = actionManager.getAction(actionId);
            actionManager.tryToExecute(action, com.intellij.openapi.ui.playback.commands.ActionCommand.getInputEvent(actionId), null, null, true);
        """,
    runInEdt = true,
  )
}
