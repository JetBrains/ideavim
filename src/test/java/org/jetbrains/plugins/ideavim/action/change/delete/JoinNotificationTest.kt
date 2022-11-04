/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.intellij.notification.ActionCenter
import com.intellij.notification.EventLog
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService
import org.jetbrains.plugins.ideavim.OptionValueType
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption

/**
 * @author Alex Plate
 */
class JoinNotificationTest : VimOptionTestCase(IjVimOptionService.ideajoinName) {
  @VimOptionTestConfiguration(VimTestOption(IjVimOptionService.ideajoinName, OptionValueType.NUMBER, "0"))
  fun `test notification shown for no ideajoin`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(false)
    typeText(injector.parser.parseKeys("J"))

    val notification = ActionCenter.getNotifications(myFixture.project, true).last()
    try {
      assertEquals(NotificationService.IDEAVIM_NOTIFICATION_TITLE, notification.title)
      assertTrue(IjVimOptionService.ideajoinName in notification.content)
      assertEquals(3, notification.actions.size)
    } finally {
      notification.expire()
    }
  }

  @VimOptionTestConfiguration(VimTestOption(IjVimOptionService.ideajoinName, OptionValueType.NUMBER, "1"))
  fun `test notification not shown for ideajoin`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(false)
    typeText(injector.parser.parseKeys("J"))

    val notifications = ActionCenter.getNotifications(myFixture.project, true)
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || IjVimOptionService.ideajoinName !in notifications.last().content)
  }

  @VimOptionTestConfiguration(VimTestOption(IjVimOptionService.ideajoinName, OptionValueType.NUMBER, "0"))
  fun `test notification not shown if was shown already`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(true)
    typeText(injector.parser.parseKeys("J"))

    val notifications = EventLog.getLogModel(myFixture.project).notifications
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || IjVimOptionService.ideajoinName !in notifications.last().content)
  }

  private fun appReadySetup(notifierEnabled: Boolean) {
    EventLog.markAllAsRead(myFixture.project)
    VimPlugin.getVimState().isIdeaJoinNotified = notifierEnabled
  }
}
