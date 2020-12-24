/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.notification.EventLog
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType
import org.jetbrains.plugins.ideavim.rangeOf

/**
 * @author Alex Plate
 */
class IdeaPutNotificationsTest : VimOptionTestCase(ClipboardOptionsData.name) {
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, []))
  fun `test notification exists if no ideaput`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(false)
    VimPlugin.getRegister().storeText(myFixture.editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(StringHelper.parseKeys("p"))

    val notification = EventLog.getLogModel(myFixture.project).notifications.last()
    assertEquals(NotificationService.IDEAVIM_NOTIFICATION_TITLE, notification.title)
    assertTrue(ClipboardOptionsData.ideaput in notification.content)
    assertEquals(2, notification.actions.size)
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, [ClipboardOptionsData.ideaput]))
  fun `test no notification on ideaput`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(false)
    VimPlugin.getRegister().storeText(myFixture.editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(StringHelper.parseKeys("p"))

    val notifications = EventLog.getLogModel(myFixture.project).notifications
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || ClipboardOptionsData.ideaput !in notifications.last().content)
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, []))
  fun `test no notification if already was`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(true)
    VimPlugin.getRegister().storeText(myFixture.editor, before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(StringHelper.parseKeys("p"))

    val notifications = EventLog.getLogModel(myFixture.project).notifications
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || ClipboardOptionsData.ideaput !in notifications.last().content)
  }

  private fun appReadySetup(notifierEnabled: Boolean) {
    EventLog.markAllAsRead(myFixture.project)
    VimPlugin.getVimState().isIdeaPutNotified = notifierEnabled
  }
}