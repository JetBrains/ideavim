/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.statistic

import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionRuleValidator
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

internal class ActionTracker : CounterUsagesCollector() {
  object Util {
    fun logTrackedAction(actionId: String) {
      TRACKED_ACTIONS.log(actionId)
    }

    fun logCopiedAction(actionId: String) {
      COPIED_ACTIONS.log(actionId)
    }
  }

  override fun getGroup(): EventLogGroup = GROUP
}

private val GROUP = EventLogGroup("vim.actions", 1, "FUS", description = "Group: Information about action id tracker")
private val TRACKED_ACTIONS = GROUP.registerEvent(
  "tracked",
  EventFields.StringValidatedByCustomRule("action_id", ActionRuleValidator::class.java),
  "The action id was detected during the action tracking process",
)
private val COPIED_ACTIONS = GROUP.registerEvent(
  "copied",
  EventFields.StringValidatedByCustomRule("action_id", ActionRuleValidator::class.java),
  "The action id was copied during the action tracking process",
)

