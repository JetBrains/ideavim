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
import com.intellij.notification.Notification
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.NotificationService
import org.jetbrains.plugins.ideavim.TestIjOptionConstants
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * @author Alex Plate
 */
class JoinNotificationTest : VimTestCase() {
  @Test
  fun `test notification shown for no ideajoin`() {
    val notification = newNotifications {
      val before = "I found${c} it\n in a legendary land"
      configureByText(before)
      appReadySetup(false)
      typeText(injector.parser.parseKeys("J"))
    }.last()

    try {
      kotlin.test.assertEquals(NotificationService.IDEAVIM_NOTIFICATION_TITLE, notification.title)
      assertTrue(TestIjOptionConstants.ideajoin in notification.content)
      kotlin.test.assertEquals(3, notification.actions.size)
    } finally {
      notification.expire()
    }
  }

  @Test
  fun `test notification not shown for ideajoin`() {
    val notifications = newNotifications {
      val before = "I found${c} it\n in a legendary land"
      configureByText(before)
      enterCommand("set ideajoin")
      appReadySetup(false)
      typeText(injector.parser.parseKeys("J"))
    }
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || TestIjOptionConstants.ideajoin !in notifications.last().content)
  }

  @Test
  fun `test notification not shown if was shown already`() {
    val notifications = newNotifications {
      val before = "I found${c} it\n in a legendary land"
      configureByText(before)
      appReadySetup(true)
      typeText(injector.parser.parseKeys("J"))
    }
    assertTrue(
      notifications.isEmpty() || notifications.last().isExpired || TestIjOptionConstants.ideajoin !in notifications.last().content,
      "$notifications"
    )
  }

  private fun appReadySetup(notifierEnabled: Boolean) {
    VimPlugin.getVimState().isIdeaJoinNotified = notifierEnabled
  }

  private fun newNotifications(action: () -> Unit): List<Notification> {
    val beforeIds = ActionCenter.getNotifications(fixture.project).map { it.id }.toSet()
    action()
    return ActionCenter.getNotifications(fixture.project).filter { it.id !in beforeIds }
  }
}
