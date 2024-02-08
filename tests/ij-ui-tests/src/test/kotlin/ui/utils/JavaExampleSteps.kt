/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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

class JavaExampleSteps(private val remoteRobot: RemoteRobot) {
  @Suppress("unused")
  private val keyboard: Keyboard = Keyboard(remoteRobot)

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
