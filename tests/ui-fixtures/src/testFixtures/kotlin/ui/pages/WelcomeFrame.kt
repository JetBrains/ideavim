/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

fun RemoteRobot.welcomeFrame(function: WelcomeFrame.() -> Unit) {
  find(WelcomeFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Welcome Frame")
@DefaultXpath("type", "//div[@class='FlatWelcomeFrame']")
class WelcomeFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
  CommonContainerFixture(remoteRobot, remoteComponent) {
  val createNewProjectLink
    get() = actionLink(
      byXpath(
        "New Project",
        "//div[(@class='MainButton' and @text='New Project') or (@accessiblename='New Project' and @class='JButton')]",
      ),
    )

  val createNewSolutionLink
    get() = actionLink(
      byXpath(
        "New Solution",
        "//div[(@class='MainButton' and @text='New Solution') or (@accessiblename='New Solution' and @class='JButton')]",
      ),
    )

  @Suppress("unused")
  val moreActions
    get() = button(byXpath("More Action", "//div[@accessiblename='More Actions' and @class='ActionButton']"))

  @Suppress("unused")
  val heavyWeightPopup
    get() = remoteRobot.find(ComponentFixture::class.java, byXpath("//div[@class='HeavyWeightWindow']"))
}
