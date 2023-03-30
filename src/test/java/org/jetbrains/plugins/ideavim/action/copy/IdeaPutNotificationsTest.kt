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
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption
import org.jetbrains.plugins.ideavim.rangeOf

@TraceOptions(TestOptionConstants.clipboard)
class IdeaPutNotificationsTest : VimTestCase() {
  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [""]))
  fun `test notification exists if no ideaput`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(false)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("p"))

    val notification = ActionCenter.getNotifications(fixture.project, true).last()
    try {
      kotlin.test.assertEquals(NotificationService.IDEAVIM_NOTIFICATION_TITLE, notification.title)
      kotlin.test.assertTrue(OptionConstants.clipboard_ideaput in notification.content)
      kotlin.test.assertEquals(2, notification.actions.size)
    } finally {
      notification.expire()
    }
  }

  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [OptionConstants.clipboard_ideaput]))
  fun `test no notification on ideaput`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(false)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("p"))

    val notifications = ActionCenter.getNotifications(fixture.project, true)
    kotlin.test.assertTrue(notifications.isEmpty() || notifications.last().isExpired || OptionConstants.clipboard_ideaput !in notifications.last().content)
  }

  @OptionTest(VimOption(TestOptionConstants.clipboard, limitedValues = [""]))
  fun `test no notification if already was`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    appReadySetup(true)
    val vimEditor = fixture.editor.vim
    VimPlugin.getRegister()
      .storeText(vimEditor, vimEditor.primaryCaret(), before rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(injector.parser.parseKeys("p"))

    val notifications = EventLog.getLogModel(fixture.project).notifications
    kotlin.test.assertTrue(notifications.isEmpty() || notifications.last().isExpired || OptionConstants.clipboard_ideaput !in notifications.last().content)
  }

  private fun appReadySetup(notifierEnabled: Boolean) {
    EventLog.markAllAsRead(fixture.project)
    VimPlugin.getVimState().isIdeaPutNotified = notifierEnabled
  }
}
