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
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath

@JvmOverloads
fun ContainerFixture.gutter(function: Gutter.() -> Unit = {}): ContainerFixture {
  return find<Gutter>(
    byXpath("//div[@class='EditorGutterComponentImpl']"),
  )
    .apply { runJs("robot.moveMouse(component);") }
    .apply(function)
}

@FixtureName("Gutter")
class Gutter(
  remoteRobot: RemoteRobot,
  remoteComponent: RemoteComponent,
) : CommonContainerFixture(remoteRobot, remoteComponent)
