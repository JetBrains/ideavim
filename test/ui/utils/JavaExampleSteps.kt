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
package org.intellij.examples.simple.plugin.steps

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.Keyboard
import ui.pages.DialogFixture
import ui.pages.DialogFixture.Companion.byTitle
import ui.pages.IdeaFrame

class JavaExampleSteps(private val remoteRobot: RemoteRobot) {
  private val keyboard: Keyboard

  fun closeTipOfTheDay() {
    step("Close Tip of the Day if it appears", Runnable {
      val idea: IdeaFrame = remoteRobot.find(IdeaFrame::class.java)
      idea.dumbAware {
        try {
          idea.find(DialogFixture::class.java, byTitle("Tip of the Day")).button("Close").click()
        } catch (ignore: Throwable) {
        }
      }
    })
  }

  init {
    keyboard = Keyboard(remoteRobot)
  }
}