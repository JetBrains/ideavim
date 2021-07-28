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
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.JButtonFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.Keyboard
import ui.pages.DialogFixture
import ui.pages.DialogFixture.Companion.byTitle
import ui.pages.IdeaFrame
import ui.pages.dialog
import ui.pages.idea

class JavaExampleSteps(private val remoteRobot: RemoteRobot) {
  @Suppress("unused")
  private val keyboard: Keyboard = Keyboard(remoteRobot)

  fun closeIdeaVimDialog() = optionalStep("Close Idea Vim dialog if it appears") {
    remoteRobot.idea {
      dialog("IdeaVim") { button("Yes").click() }
    }
  }

  fun closeTipOfTheDay() = optionalStep("Close Tip of the Day if it appears") {
    val idea: IdeaFrame = remoteRobot.find(IdeaFrame::class.java)
    idea.dumbAware {
      idea.find(DialogFixture::class.java, byTitle("Tip of the Day")).button("Close").click()
    }
    closeAllGotIt()
  }

  fun closeAllGotIt() = step("Close Got It") {
    remoteRobot.findAll<JButtonFixture>(byXpath("//div[@accessiblename='Got It']")).forEach {
      it.click()
    }
  }

  fun closeAllTabs() = step("Close all existing tabs") {
    remoteRobot.findAll<CommonContainerFixture>(byXpath("//div[@class='EditorTabs']//div[@class='SingleHeightLabel']")).forEach {
      it.find<ComponentFixture>(byXpath("//div[@class='InplaceButton']")).click()
    }
  }

  private fun optionalStep(stepName: String, code: () -> Unit) = step(stepName) {
    try {
      code()
    } catch (ignore: Throwable) {
      println("$stepName ignored")
    }
  }
}
