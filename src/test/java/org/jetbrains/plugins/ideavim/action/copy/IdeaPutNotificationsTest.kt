/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.notification.ActionCenter
import com.intellij.notification.EventLog
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import org.jetbrains.plugins.ideavim.OptionValueType
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.rangeOf

/**
 * @author Alex Plate
 */
class IdeaPutNotificationsTest : VimOptionTestCase(OptionConstants.clipboardName) {
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.clipboardName, OptionValueType.STRING, ""))
  fun `test notification exists if no ideaput`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(false)
    val vimEditor = myFixture.editor.vim
    VimPlugin.getRegister().storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("p"))

    val notification = ActionCenter.getNotifications(myFixture.project, true).last()
    try {
      assertEquals(NotificationService.IDEAVIM_NOTIFICATION_TITLE, notification.title)
      assertTrue(OptionConstants.clipboard_ideaput in notification.content)
      assertEquals(2, notification.actions.size)
    } finally {
      notification.expire()
    }
  }

  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.clipboardName,
      OptionValueType.STRING,
      OptionConstants.clipboard_ideaput
    )
  )
  fun `test no notification on ideaput`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(false)
    val vimEditor = myFixture.editor.vim
    VimPlugin.getRegister().storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("p"))

    val notifications = ActionCenter.getNotifications(myFixture.project, true)
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || OptionConstants.clipboard_ideaput !in notifications.last().content)
  }

  @VimOptionTestConfiguration(VimTestOption(OptionConstants.clipboardName, OptionValueType.STRING, ""))
  fun `test no notification if already was`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(true)
    val vimEditor = myFixture.editor.vim
    VimPlugin.getRegister().storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("p"))

    val notifications = EventLog.getLogModel(myFixture.project).notifications
    assertTrue(notifications.isEmpty() || notifications.last().isExpired || OptionConstants.clipboard_ideaput !in notifications.last().content)
  }

  private fun appReadySetup(notifierEnabled: Boolean) {
    EventLog.markAllAsRead(myFixture.project)
    VimPlugin.getVimState().isIdeaPutNotified = notifierEnabled
  }
}
