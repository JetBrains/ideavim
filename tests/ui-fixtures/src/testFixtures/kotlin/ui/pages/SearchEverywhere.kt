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
fun ContainerFixture.searchEverywhere(function: SearchEverywhere.() -> Unit = {}): SearchEverywhere {
  return find<SearchEverywhere>(
    byXpath("Search Everywhere", "//div[@accessiblename='Search everywhere' and @class='SearchEverywhereUI']"),
  )
    .apply { runJs("robot.moveMouse(component);") }
    .apply(function)
}

@FixtureName("SearchEverywhere")
class SearchEverywhere(
  remoteRobot: RemoteRobot,
  remoteComponent: RemoteComponent,
) : CommonContainerFixture(remoteRobot, remoteComponent)
