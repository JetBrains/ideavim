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

package com.maddyhome.idea.vim.statistic

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.maddyhome.idea.vim.option.IdeaRefactorMode
import com.maddyhome.idea.vim.option.IdeaStatusIcon
import com.maddyhome.idea.vim.option.IdeaWriteData

class OptionsCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  companion object {
    val GROUP = EventLogGroup("ideavim.options", 1)

    val STATUS_BAR_ICON =
      GROUP.registerEvent(
        "status.bar.icon",
        EventFields.String("value", IdeaStatusIcon.allValues.toList()),
        EventFields.Enum("option_activation", OptionActivation::class.java)
      )
    val IDEA_REFACTOR_MODE =
      GROUP.registerEvent(
        "refactor.mode",
        EventFields.String("value", IdeaRefactorMode.availableValues.toList()),
        EventFields.Enum("option_activation", OptionActivation::class.java)
      )
    val IDEA_WRITE =
      GROUP.registerEvent(
        "write",
        EventFields.String("value", IdeaWriteData.allValues.toList()),
        EventFields.Enum("option_activation", OptionActivation::class.java)
      )

    var fileExecution = false;
  }
}

enum class OptionActivation {
  DEFAULT,
  IDEAVIMRC,
  EX_COMMAND,
}
