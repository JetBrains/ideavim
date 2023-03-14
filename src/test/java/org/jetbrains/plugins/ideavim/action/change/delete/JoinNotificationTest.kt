/*
 * Copyright 2003-2023 The IdeaVim authors
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
import org.jetbrains.plugins.ideavim.TestIjOptionConstants
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption

/**
 * @author Alex Plate
 */
@TraceOptions(TestIjOptionConstants.ideajoin)
class JoinNotificationTest : VimTestCase() {
  @OptionTest(VimOption(TestIjOptionConstants.ideajoin, limitedValues = ["false"]))
  fun `test notification shown for no ideajoin`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(false)
    typeText(injector.parser.parseKeys("J"))

    val notification = ActionCenter.getNotifications(fixture.project, true).last()
    try {
      kotlin.test.assertEquals(NotificationService.IDEAVIM_NOTIFICATION_TITLE, notification.title)
      kotlin.test.assertTrue(TestIjOptionConstants.ideajoin in notification.content)
      kotlin.test.assertEquals(3, notification.actions.size)
    } finally {
      notification.expire()
    }
  }

  @OptionTest(VimOption(TestIjOptionConstants.ideajoin, limitedValues = ["true"]))
  fun `test notification not shown for ideajoin`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(false)
    typeText(injector.parser.parseKeys("J"))

    val notifications = ActionCenter.getNotifications(fixture.project, true)
    kotlin.test.assertTrue(notifications.isEmpty() || notifications.last().isExpired || TestIjOptionConstants.ideajoin !in notifications.last().content)
  }

  @OptionTest(VimOption(TestIjOptionConstants.ideajoin, limitedValues = ["false"]))
  fun `test notification not shown if was shown already`() {
    val before = "I found${c} it\n in a legendary land"
    configureByText(before)
    appReadySetup(true)
    typeText(injector.parser.parseKeys("J"))

    val notifications = EventLog.getLogModel(fixture.project).notifications
    kotlin.test.assertTrue(notifications.isEmpty() || notifications.last().isExpired || TestIjOptionConstants.ideajoin !in notifications.last().content)
  }

  private fun appReadySetup(notifierEnabled: Boolean) {
    EventLog.markAllAsRead(fixture.project)
    VimPlugin.getVimState().isIdeaJoinNotified = notifierEnabled
  }
}
