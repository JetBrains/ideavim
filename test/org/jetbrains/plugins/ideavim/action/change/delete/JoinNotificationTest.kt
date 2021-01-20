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

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.intellij.notification.EventLog
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.IdeaJoinOptionsData
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

/**
 * @author Alex Plate
 */
class JoinNotificationTest : VimOptionTestCase(IdeaJoinOptionsData.name) {
  @VimOptionTestConfiguration(VimTestOption(IdeaJoinOptionsData.name, VimTestOptionType.TOGGLE, ["false"]))
  fun `test notification shown for no ideajoin`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(false)
    typeText(StringHelper.parseKeys("J"))

    val notification = EventLog.getLogModel(myFixture.project).notifications.last()
    assertEquals(NotificationService.IDEAVIM_NOTIFICATION_TITLE, notification.title)
    assertTrue(IdeaJoinOptionsData.name in notification.content)
    assertEquals(3, notification.actions.size)
  }

  @VimOptionTestConfiguration(VimTestOption(IdeaJoinOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  fun `test notification not shown for ideajoin`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(false)
    typeText(StringHelper.parseKeys("J"))

    val notifications = EventLog.getLogModel(myFixture.project).notifications
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || IdeaJoinOptionsData.name !in notifications.last().content)
  }

  @VimOptionTestConfiguration(VimTestOption(IdeaJoinOptionsData.name, VimTestOptionType.TOGGLE, ["false"]))
  fun `test notification not shown if was shown already`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(true)
    typeText(StringHelper.parseKeys("J"))

    val notifications = EventLog.getLogModel(myFixture.project).notifications
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || IdeaJoinOptionsData.name !in notifications.last().content)
  }

  private fun appReadySetup(notifierEnabled: Boolean) {
    EventLog.markAllAsRead(myFixture.project)
    VimPlugin.getVimState().isIdeaJoinNotified = notifierEnabled
  }
}