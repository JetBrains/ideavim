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
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor
import java.time.Duration

fun RemoteRobot.actionMenu(text: String): ActionMenuFixture {
  val xpath = byXpath("text '$text'", "//div[@class='ActionMenu' and @text='$text']")
  waitFor(duration = Duration.ofMinutes(1)) {
    findAll<ActionMenuFixture>(xpath).isNotEmpty()
  }
  return findAll<ActionMenuFixture>(xpath).first()
}

fun RemoteRobot.actionMenuItem(text: String): ActionMenuItemFixture {
  val xpath = byXpath("text '$text'", "//div[@class='ActionMenuItem' and @text='$text']")
  waitFor(duration = Duration.ofMinutes(1)) {
    findAll<ActionMenuItemFixture>(xpath).isNotEmpty()
  }
  return findAll<ActionMenuItemFixture>(xpath).first()
}

@FixtureName("ActionMenu")
class ActionMenuFixture(
  remoteRobot: RemoteRobot,
  remoteComponent: RemoteComponent,
) : ComponentFixture(remoteRobot, remoteComponent)

@FixtureName("ActionMenuItem")
class ActionMenuItemFixture(
  remoteRobot: RemoteRobot,
  remoteComponent: RemoteComponent,
) : ComponentFixture(remoteRobot, remoteComponent)
