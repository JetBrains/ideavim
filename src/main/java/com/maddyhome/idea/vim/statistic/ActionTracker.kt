/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.statistic

import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionRuleValidator
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

internal class ActionTracker : CounterUsagesCollector() {
  companion object {
    private val GROUP = EventLogGroup("vim.actions", 1)
    private val TRACKED_ACTIONS = GROUP.registerEvent(
      "tracked",
      EventFields.StringValidatedByCustomRule("action_id", ActionRuleValidator::class.java)
    )
    private val COPIED_ACTIONS = GROUP.registerEvent(
      "copied",
      EventFields.StringValidatedByCustomRule("action_id", ActionRuleValidator::class.java)
    )

    fun logTrackedAction(actionId: String) {
      TRACKED_ACTIONS.log(actionId)
    }

    fun logCopiedAction(actionId: String) {
      COPIED_ACTIONS.log(actionId)
    }
  }

  override fun getGroup(): EventLogGroup = GROUP
}
