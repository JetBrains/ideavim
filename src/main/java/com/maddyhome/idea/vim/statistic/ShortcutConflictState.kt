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

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.eventLog.events.StringListEventField
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.SHIFT_DOWN_MASK
import javax.swing.KeyStroke

internal class ShortcutConflictState : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    return setOf(
      HANDLERS.metric(
        CTRL_1 withKeyStroke KeyStroke.getKeyStroke('1'.toInt(), CTRL_DOWN_MASK),
        CTRL_2 withKeyStroke KeyStroke.getKeyStroke('2'.toInt(), CTRL_DOWN_MASK),
        CTRL_3 withKeyStroke KeyStroke.getKeyStroke('3'.toInt(), CTRL_DOWN_MASK),
        CTRL_4 withKeyStroke KeyStroke.getKeyStroke('4'.toInt(), CTRL_DOWN_MASK),
        CTRL_5 withKeyStroke KeyStroke.getKeyStroke('5'.toInt(), CTRL_DOWN_MASK),
        CTRL_6 withKeyStroke KeyStroke.getKeyStroke('6'.toInt(), CTRL_DOWN_MASK),
        CTRL_7 withKeyStroke KeyStroke.getKeyStroke('7'.toInt(), CTRL_DOWN_MASK),
        CTRL_8 withKeyStroke KeyStroke.getKeyStroke('8'.toInt(), CTRL_DOWN_MASK),
        CTRL_9 withKeyStroke KeyStroke.getKeyStroke('9'.toInt(), CTRL_DOWN_MASK),
        CTRL_0 withKeyStroke KeyStroke.getKeyStroke('0'.toInt(), CTRL_DOWN_MASK),

        CTRL_SHIFT_1 withKeyStroke KeyStroke.getKeyStroke('1'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_2 withKeyStroke KeyStroke.getKeyStroke('2'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_3 withKeyStroke KeyStroke.getKeyStroke('3'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_4 withKeyStroke KeyStroke.getKeyStroke('4'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_5 withKeyStroke KeyStroke.getKeyStroke('5'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_6 withKeyStroke KeyStroke.getKeyStroke('6'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_7 withKeyStroke KeyStroke.getKeyStroke('7'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_8 withKeyStroke KeyStroke.getKeyStroke('8'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_9 withKeyStroke KeyStroke.getKeyStroke('9'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_0 withKeyStroke KeyStroke.getKeyStroke('0'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),

        CTRL_A withKeyStroke KeyStroke.getKeyStroke('A'.toInt(), CTRL_DOWN_MASK),
        CTRL_B withKeyStroke KeyStroke.getKeyStroke('B'.toInt(), CTRL_DOWN_MASK),
        CTRL_C withKeyStroke KeyStroke.getKeyStroke('C'.toInt(), CTRL_DOWN_MASK),
        CTRL_D withKeyStroke KeyStroke.getKeyStroke('D'.toInt(), CTRL_DOWN_MASK),
        CTRL_E withKeyStroke KeyStroke.getKeyStroke('E'.toInt(), CTRL_DOWN_MASK),
        CTRL_F withKeyStroke KeyStroke.getKeyStroke('F'.toInt(), CTRL_DOWN_MASK),
        CTRL_G withKeyStroke KeyStroke.getKeyStroke('G'.toInt(), CTRL_DOWN_MASK),
        CTRL_H withKeyStroke KeyStroke.getKeyStroke('H'.toInt(), CTRL_DOWN_MASK),
        CTRL_I withKeyStroke KeyStroke.getKeyStroke('I'.toInt(), CTRL_DOWN_MASK),
        CTRL_J withKeyStroke KeyStroke.getKeyStroke('J'.toInt(), CTRL_DOWN_MASK),
        CTRL_K withKeyStroke KeyStroke.getKeyStroke('K'.toInt(), CTRL_DOWN_MASK),
        CTRL_L withKeyStroke KeyStroke.getKeyStroke('L'.toInt(), CTRL_DOWN_MASK),
        CTRL_M withKeyStroke KeyStroke.getKeyStroke('M'.toInt(), CTRL_DOWN_MASK),
        CTRL_N withKeyStroke KeyStroke.getKeyStroke('N'.toInt(), CTRL_DOWN_MASK),
        CTRL_O withKeyStroke KeyStroke.getKeyStroke('O'.toInt(), CTRL_DOWN_MASK),
        CTRL_P withKeyStroke KeyStroke.getKeyStroke('P'.toInt(), CTRL_DOWN_MASK),
        CTRL_Q withKeyStroke KeyStroke.getKeyStroke('Q'.toInt(), CTRL_DOWN_MASK),
        CTRL_R withKeyStroke KeyStroke.getKeyStroke('R'.toInt(), CTRL_DOWN_MASK),
        CTRL_S withKeyStroke KeyStroke.getKeyStroke('S'.toInt(), CTRL_DOWN_MASK),
        CTRL_T withKeyStroke KeyStroke.getKeyStroke('T'.toInt(), CTRL_DOWN_MASK),
        CTRL_U withKeyStroke KeyStroke.getKeyStroke('U'.toInt(), CTRL_DOWN_MASK),
        CTRL_V withKeyStroke KeyStroke.getKeyStroke('V'.toInt(), CTRL_DOWN_MASK),
        CTRL_W withKeyStroke KeyStroke.getKeyStroke('W'.toInt(), CTRL_DOWN_MASK),
        CTRL_X withKeyStroke KeyStroke.getKeyStroke('X'.toInt(), CTRL_DOWN_MASK),
        CTRL_Y withKeyStroke KeyStroke.getKeyStroke('Y'.toInt(), CTRL_DOWN_MASK),
        CTRL_Z withKeyStroke KeyStroke.getKeyStroke('Z'.toInt(), CTRL_DOWN_MASK),
        CTRL_BR1 withKeyStroke KeyStroke.getKeyStroke('['.toInt(), CTRL_DOWN_MASK),
        CTRL_BR2 withKeyStroke KeyStroke.getKeyStroke(']'.toInt(), CTRL_DOWN_MASK),

        CTRL_SHIFT_A withKeyStroke KeyStroke.getKeyStroke('A'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_B withKeyStroke KeyStroke.getKeyStroke('B'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_C withKeyStroke KeyStroke.getKeyStroke('C'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_D withKeyStroke KeyStroke.getKeyStroke('D'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_E withKeyStroke KeyStroke.getKeyStroke('E'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_F withKeyStroke KeyStroke.getKeyStroke('F'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_G withKeyStroke KeyStroke.getKeyStroke('G'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_H withKeyStroke KeyStroke.getKeyStroke('H'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_I withKeyStroke KeyStroke.getKeyStroke('I'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_J withKeyStroke KeyStroke.getKeyStroke('J'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_K withKeyStroke KeyStroke.getKeyStroke('K'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_L withKeyStroke KeyStroke.getKeyStroke('L'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_M withKeyStroke KeyStroke.getKeyStroke('M'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_N withKeyStroke KeyStroke.getKeyStroke('N'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_O withKeyStroke KeyStroke.getKeyStroke('O'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_P withKeyStroke KeyStroke.getKeyStroke('P'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_Q withKeyStroke KeyStroke.getKeyStroke('Q'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_R withKeyStroke KeyStroke.getKeyStroke('R'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_S withKeyStroke KeyStroke.getKeyStroke('S'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_T withKeyStroke KeyStroke.getKeyStroke('T'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_U withKeyStroke KeyStroke.getKeyStroke('U'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_V withKeyStroke KeyStroke.getKeyStroke('V'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_W withKeyStroke KeyStroke.getKeyStroke('W'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_X withKeyStroke KeyStroke.getKeyStroke('X'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_Y withKeyStroke KeyStroke.getKeyStroke('Y'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_Z withKeyStroke KeyStroke.getKeyStroke('Z'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_BR1 withKeyStroke KeyStroke.getKeyStroke('['.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
        CTRL_SHIFT_BR2 withKeyStroke KeyStroke.getKeyStroke(']'.toInt(), CTRL_DOWN_MASK + SHIFT_DOWN_MASK),
      )
    )
  }

  private infix fun StringListEventField.withKeyStroke(ks: KeyStroke): EventPair<List<String>> {
    return this.with(getHandlersForShortcut(ks).map { it.name })
  }

  private fun getHandlersForShortcut(shortcut: KeyStroke): List<HandledModes> {
    val modes = VimPlugin.getKey().shortcutConflicts[shortcut] ?: return listOf(HandledModes.NORMAL_UNDEFINED, HandledModes.INSERT_UNDEFINED, HandledModes.VISUAL_AND_SELECT_UNDEFINED)

    return when (modes) {
      is ShortcutOwnerInfo.AllModes -> {
        when (modes.owner) {
          ShortcutOwner.IDE -> listOf(HandledModes.NORMAL_IDE, HandledModes.INSERT_IDE, HandledModes.VISUAL_AND_SELECT_IDE)
          ShortcutOwner.VIM -> listOf(HandledModes.NORMAL_VIM, HandledModes.INSERT_VIM, HandledModes.VISUAL_AND_SELECT_VIM)
          ShortcutOwner.UNDEFINED -> listOf(HandledModes.NORMAL_UNDEFINED, HandledModes.INSERT_UNDEFINED, HandledModes.VISUAL_AND_SELECT_UNDEFINED)
        }
      }

      is ShortcutOwnerInfo.PerMode -> {
        val result = mutableListOf<HandledModes>()
        when (modes.normal) {
          ShortcutOwner.IDE -> result.add(HandledModes.NORMAL_IDE)
          ShortcutOwner.VIM -> result.add(HandledModes.NORMAL_VIM)
          ShortcutOwner.UNDEFINED -> result.add(HandledModes.NORMAL_UNDEFINED)
        }
        when (modes.insert) {
          ShortcutOwner.IDE -> result.add(HandledModes.INSERT_IDE)
          ShortcutOwner.VIM -> result.add(HandledModes.INSERT_VIM)
          ShortcutOwner.UNDEFINED -> result.add(HandledModes.INSERT_UNDEFINED)
        }
        when (modes.visual) {
          ShortcutOwner.IDE -> result.add(HandledModes.VISUAL_AND_SELECT_IDE)
          ShortcutOwner.VIM -> result.add(HandledModes.VISUAL_AND_SELECT_VIM)
          ShortcutOwner.UNDEFINED -> result.add(HandledModes.VISUAL_AND_SELECT_UNDEFINED)
        }
        result
      }
    }
  }

  companion object {
    private val GROUP = EventLogGroup("vim.handlers", 1)
    private val values = HandledModes.values().map { it.name }

    private val CTRL_1 = EventFields.StringList("Ctrl-1", values)
    private val CTRL_2 = EventFields.StringList("Ctrl-2", values)
    private val CTRL_3 = EventFields.StringList("Ctrl-3", values)
    private val CTRL_4 = EventFields.StringList("Ctrl-4", values)
    private val CTRL_5 = EventFields.StringList("Ctrl-5", values)
    private val CTRL_6 = EventFields.StringList("Ctrl-6", values)
    private val CTRL_7 = EventFields.StringList("Ctrl-7", values)
    private val CTRL_8 = EventFields.StringList("Ctrl-8", values)
    private val CTRL_9 = EventFields.StringList("Ctrl-9", values)
    private val CTRL_0 = EventFields.StringList("Ctrl-0", values)

    private val CTRL_SHIFT_1 = EventFields.StringList("Ctrl-Shift-1", values)
    private val CTRL_SHIFT_2 = EventFields.StringList("Ctrl-Shift-2", values)
    private val CTRL_SHIFT_3 = EventFields.StringList("Ctrl-Shift-3", values)
    private val CTRL_SHIFT_4 = EventFields.StringList("Ctrl-Shift-4", values)
    private val CTRL_SHIFT_5 = EventFields.StringList("Ctrl-Shift-5", values)
    private val CTRL_SHIFT_6 = EventFields.StringList("Ctrl-Shift-6", values)
    private val CTRL_SHIFT_7 = EventFields.StringList("Ctrl-Shift-7", values)
    private val CTRL_SHIFT_8 = EventFields.StringList("Ctrl-Shift-8", values)
    private val CTRL_SHIFT_9 = EventFields.StringList("Ctrl-Shift-9", values)
    private val CTRL_SHIFT_0 = EventFields.StringList("Ctrl-Shift-0", values)

    private val CTRL_A = EventFields.StringList("Ctrl-A", values)
    private val CTRL_B = EventFields.StringList("Ctrl-B", values)
    private val CTRL_C = EventFields.StringList("Ctrl-C", values)
    private val CTRL_D = EventFields.StringList("Ctrl-D", values)
    private val CTRL_E = EventFields.StringList("Ctrl-E", values)
    private val CTRL_F = EventFields.StringList("Ctrl-F", values)
    private val CTRL_G = EventFields.StringList("Ctrl-G", values)
    private val CTRL_H = EventFields.StringList("Ctrl-H", values)
    private val CTRL_I = EventFields.StringList("Ctrl-I", values)
    private val CTRL_J = EventFields.StringList("Ctrl-J", values)
    private val CTRL_K = EventFields.StringList("Ctrl-K", values)
    private val CTRL_L = EventFields.StringList("Ctrl-L", values)
    private val CTRL_M = EventFields.StringList("Ctrl-M", values)
    private val CTRL_N = EventFields.StringList("Ctrl-N", values)
    private val CTRL_O = EventFields.StringList("Ctrl-O", values)
    private val CTRL_P = EventFields.StringList("Ctrl-P", values)
    private val CTRL_Q = EventFields.StringList("Ctrl-Q", values)
    private val CTRL_R = EventFields.StringList("Ctrl-R", values)
    private val CTRL_S = EventFields.StringList("Ctrl-S", values)
    private val CTRL_T = EventFields.StringList("Ctrl-T", values)
    private val CTRL_U = EventFields.StringList("Ctrl-U", values)
    private val CTRL_V = EventFields.StringList("Ctrl-V", values)
    private val CTRL_W = EventFields.StringList("Ctrl-W", values)
    private val CTRL_X = EventFields.StringList("Ctrl-X", values)
    private val CTRL_Y = EventFields.StringList("Ctrl-Y", values)
    private val CTRL_Z = EventFields.StringList("Ctrl-Z", values)
    private val CTRL_BR1 = EventFields.StringList("Ctrl-[", values)
    private val CTRL_BR2 = EventFields.StringList("Ctrl-]", values)

    private val CTRL_SHIFT_A = EventFields.StringList("Ctrl-Shift-A", values)
    private val CTRL_SHIFT_B = EventFields.StringList("Ctrl-Shift-B", values)
    private val CTRL_SHIFT_C = EventFields.StringList("Ctrl-Shift-C", values)
    private val CTRL_SHIFT_D = EventFields.StringList("Ctrl-Shift-D", values)
    private val CTRL_SHIFT_E = EventFields.StringList("Ctrl-Shift-E", values)
    private val CTRL_SHIFT_F = EventFields.StringList("Ctrl-Shift-F", values)
    private val CTRL_SHIFT_G = EventFields.StringList("Ctrl-Shift-G", values)
    private val CTRL_SHIFT_H = EventFields.StringList("Ctrl-Shift-H", values)
    private val CTRL_SHIFT_I = EventFields.StringList("Ctrl-Shift-I", values)
    private val CTRL_SHIFT_J = EventFields.StringList("Ctrl-Shift-J", values)
    private val CTRL_SHIFT_K = EventFields.StringList("Ctrl-Shift-K", values)
    private val CTRL_SHIFT_L = EventFields.StringList("Ctrl-Shift-L", values)
    private val CTRL_SHIFT_M = EventFields.StringList("Ctrl-Shift-M", values)
    private val CTRL_SHIFT_N = EventFields.StringList("Ctrl-Shift-N", values)
    private val CTRL_SHIFT_O = EventFields.StringList("Ctrl-Shift-O", values)
    private val CTRL_SHIFT_P = EventFields.StringList("Ctrl-Shift-P", values)
    private val CTRL_SHIFT_Q = EventFields.StringList("Ctrl-Shift-Q", values)
    private val CTRL_SHIFT_R = EventFields.StringList("Ctrl-Shift-R", values)
    private val CTRL_SHIFT_S = EventFields.StringList("Ctrl-Shift-S", values)
    private val CTRL_SHIFT_T = EventFields.StringList("Ctrl-Shift-T", values)
    private val CTRL_SHIFT_U = EventFields.StringList("Ctrl-Shift-U", values)
    private val CTRL_SHIFT_V = EventFields.StringList("Ctrl-Shift-V", values)
    private val CTRL_SHIFT_W = EventFields.StringList("Ctrl-Shift-W", values)
    private val CTRL_SHIFT_X = EventFields.StringList("Ctrl-Shift-X", values)
    private val CTRL_SHIFT_Y = EventFields.StringList("Ctrl-Shift-Y", values)
    private val CTRL_SHIFT_Z = EventFields.StringList("Ctrl-Shift-Z", values)
    private val CTRL_SHIFT_BR1 = EventFields.StringList("Ctrl-Shift-[", values)
    private val CTRL_SHIFT_BR2 = EventFields.StringList("Ctrl-Shift-]", values)

    private val HANDLERS: VarargEventId = GROUP.registerVarargEvent(
      "vim.handlers",
      CTRL_1,
      CTRL_2,
      CTRL_3,
      CTRL_4,
      CTRL_5,
      CTRL_6,
      CTRL_7,
      CTRL_8,
      CTRL_9,
      CTRL_0,

      CTRL_SHIFT_1,
      CTRL_SHIFT_2,
      CTRL_SHIFT_3,
      CTRL_SHIFT_4,
      CTRL_SHIFT_5,
      CTRL_SHIFT_6,
      CTRL_SHIFT_7,
      CTRL_SHIFT_8,
      CTRL_SHIFT_9,
      CTRL_SHIFT_0,

      CTRL_A,
      CTRL_B,
      CTRL_C,
      CTRL_D,
      CTRL_E,
      CTRL_F,
      CTRL_G,
      CTRL_H,
      CTRL_I,
      CTRL_J,
      CTRL_K,
      CTRL_L,
      CTRL_M,
      CTRL_N,
      CTRL_O,
      CTRL_P,
      CTRL_Q,
      CTRL_R,
      CTRL_S,
      CTRL_T,
      CTRL_U,
      CTRL_V,
      CTRL_W,
      CTRL_X,
      CTRL_Y,
      CTRL_Z,
      CTRL_BR1,
      CTRL_BR2,

      CTRL_SHIFT_A,
      CTRL_SHIFT_B,
      CTRL_SHIFT_C,
      CTRL_SHIFT_D,
      CTRL_SHIFT_E,
      CTRL_SHIFT_F,
      CTRL_SHIFT_G,
      CTRL_SHIFT_H,
      CTRL_SHIFT_I,
      CTRL_SHIFT_J,
      CTRL_SHIFT_K,
      CTRL_SHIFT_L,
      CTRL_SHIFT_M,
      CTRL_SHIFT_N,
      CTRL_SHIFT_O,
      CTRL_SHIFT_P,
      CTRL_SHIFT_Q,
      CTRL_SHIFT_R,
      CTRL_SHIFT_S,
      CTRL_SHIFT_T,
      CTRL_SHIFT_U,
      CTRL_SHIFT_V,
      CTRL_SHIFT_W,
      CTRL_SHIFT_X,
      CTRL_SHIFT_Y,
      CTRL_SHIFT_Z,
      CTRL_SHIFT_BR1,
      CTRL_SHIFT_BR2,
    )
  }
}

private enum class HandledModes {
  NORMAL_UNDEFINED,
  NORMAL_IDE,
  NORMAL_VIM,
  INSERT_UNDEFINED,
  INSERT_IDE,
  INSERT_VIM,
  VISUAL_AND_SELECT_UNDEFINED,
  VISUAL_AND_SELECT_IDE,
  VISUAL_AND_SELECT_VIM,
}
