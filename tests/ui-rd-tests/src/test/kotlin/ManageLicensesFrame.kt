/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

fun RemoteRobot.manageLicensesFrame(function: ManageLicensesFrame.() -> Unit) {
  find(ManageLicensesFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Manage Licenses Frame")
@DefaultXpath("type", "//div[@class='MyDialog' and @title='Manage Licenses']")
class ManageLicensesFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
  CommonContainerFixture(remoteRobot, remoteComponent) {

  fun enableFreeTier() {
    radioButton("Activation code").click()
    textFields(byXpath("//div[@class='X']")).first().text = System.getenv("RIDER_LICENSE")
    button("Activate").click()
    button("Close").click()
  }
}
