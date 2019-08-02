package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.notification.EventLog
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import org.jetbrains.plugins.ideavim.*

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