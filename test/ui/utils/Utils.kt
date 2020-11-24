/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

import com.intellij.remoterobot.fixtures.Fixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import org.assertj.swing.core.MouseButton
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
