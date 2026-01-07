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
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.fixtures.JButtonFixture
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
    // Find text area by accessible name using ComponentFixture, which properly supports xpath queries
    // The textFields() helper doesn't correctly handle xpath with accessiblename attributes
    find(ComponentFixture::class.java, byXpath("//div[@accessiblename='Activation code' and contains(@classhierarchy, 'JTextArea')]")).apply {
      runJs("component.setText('${System.getenv("RIDER_LICENSE")}');")
    }
    button("Activate").click()
    button(JButtonFixture.byText("Close"), timeout = Duration.ofSeconds(20)).click()
  }
}
