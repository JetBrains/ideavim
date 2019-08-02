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