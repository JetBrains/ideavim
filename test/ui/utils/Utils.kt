/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package ui.utils

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.Fixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.utils.waitFor
import org.assertj.swing.core.MouseButton
import ui.pages.Editor
import java.awt.Point

fun RemoteText.doubleClickOnRight(shiftX: Int, fixture: Fixture, button: MouseButton = MouseButton.LEFT_BUTTON) {
  val updatedPoint = Point(this.point.x + shiftX, this.point.y)
  fixture.remoteRobot.execute(fixture) {
    robot.click(component, updatedPoint, button, 2)
  }
}

fun RemoteText.tripleClickOnRight(shiftX: Int, fixture: Fixture, button: MouseButton = MouseButton.LEFT_BUTTON) {
  val updatedPoint = Point(this.point.x + shiftX, this.point.y)
  fixture.remoteRobot.execute(fixture) {
    robot.click(component, updatedPoint, button, 3)
  }
}

fun RemoteText.moveMouseTo(goal: RemoteText, editor: Editor): Boolean {
  this.moveMouse()
  editor.runJs("robot.pressMouse(MouseButton.LEFT_BUTTON)")
  goal.moveMouse()
  val caretDuringDragging = editor.isBlockCursor
  editor.runJs("robot.releaseMouse(MouseButton.LEFT_BUTTON)")
  waitFor { editor.isBlockCursor }
  return caretDuringDragging
}

fun RemoteText.moveMouseWithDelayTo(goal: RemoteText, editor: Editor, delay: Long = 1000): Boolean {
  this.moveMouse()
  editor.runJs("robot.pressMouse(MouseButton.LEFT_BUTTON)")
  goal.moveMouse()
  Thread.sleep(delay)
  val caretDuringDragging = editor.isBlockCursor
  editor.runJs("robot.releaseMouse(MouseButton.LEFT_BUTTON)")
  waitFor { editor.isBlockCursor }
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
    """
  )
}

fun Point.moveMouseTo(point: Point, fixture: Fixture) {
  val _point = this
  fixture.execute { robot.moveMouse(component, _point) }

  fixture.runJs(
    """
    const point = new java.awt.Point(${point.x}, ${point.y});
    robot.pressMouseWhileRunning(MouseButton.LEFT_BUTTON, () => {
      robot.moveMouse(component, point)
    })
    """
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
    """
  )
  waitFor { editor.isBlockCursor }
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
    runInEdt = true
  )
}
