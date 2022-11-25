/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*

object GuiCursorOptionHelper {

  private val effectiveValues = mutableMapOf<GuiCursorMode, GuiCursorAttributes>()

  fun convertToken(token: String): GuiCursorEntry {
    val split = token.split(':')
    if (split.size == 1) {
      throw exExceptionMessage("E545", token)
    }
    if (split.size != 2) {
      throw exExceptionMessage("E546", token)
    }
    val modeList = split[0]
    val argumentList = split[1]

    val modes = enumSetOf<GuiCursorMode>()
    modes.addAll(
      modeList.split('-').map {
        GuiCursorMode.fromString(it) ?: throw exExceptionMessage("E546", token)
      }
    )

    var type: GuiCursorType? = null
    var thickness: Int? = null
    var highlightGroup = ""
    var lmapHighlightGroup = ""
    val blinkModes = mutableListOf<String>()
    argumentList.split('-').forEach {
      when {
        it == "block" -> type = GuiCursorType.BLOCK
        it.startsWith("ver") -> {
          type = GuiCursorType.VER
          thickness = it.slice(3 until it.length).toIntOrNull() ?: throw exExceptionMessage("E548", token)
          if (thickness == 0) {
            throw exExceptionMessage("E549", token)
          }
        }
        it.startsWith("hor") -> {
          type = GuiCursorType.HOR
          thickness = it.slice(3 until it.length).toIntOrNull() ?: throw exExceptionMessage("E548", token)
          if (thickness == 0) {
            throw exExceptionMessage("E549", token)
          }
        }
        it.startsWith("blink") -> {
          // We don't do anything with blink...
          blinkModes.add(it)
        }
        it.contains('/') -> {
          val i = it.indexOf('/')
          highlightGroup = it.slice(0 until i)
          lmapHighlightGroup = it.slice(i + 1 until it.length)
        }
        else -> highlightGroup = it
      }
    }

    return GuiCursorEntry(token, modes, type, thickness, highlightGroup, lmapHighlightGroup, blinkModes)
  }

  fun getAttributes(mode: GuiCursorMode): GuiCursorAttributes {
    return effectiveValues.computeIfAbsent(mode) {
      var type = GuiCursorType.BLOCK
      var thickness = 0
      var highlightGroup = ""
      var lmapHighlightGroup = ""
      var blinkModes = emptyList<String>()
      (injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.guicursorName) as VimString).value
        .split(",")
        .map { convertToken(it) }
        .forEach { data ->
          if (data.modes.contains(mode) || data.modes.contains(GuiCursorMode.ALL)) {
            if (data.type != null) {
              type = data.type
            }
            if (data.thickness != null) {
              thickness = data.thickness
            }
            if (data.highlightGroup.isNotEmpty()) {
              highlightGroup = data.highlightGroup
            }
            if (data.lmapHighlightGroup.isNotEmpty()) {
              lmapHighlightGroup = data.lmapHighlightGroup
            }
            if (data.blinkModes.isNotEmpty()) {
              blinkModes = data.blinkModes
            }
          }
        }
      GuiCursorAttributes(type, thickness, highlightGroup, lmapHighlightGroup, blinkModes)
    }
  }

  fun clearEffectiveValues() {
    effectiveValues.clear()
  }
}

enum class GuiCursorMode(val token: String) {
  NORMAL("n"),
  VISUAL("v"),
  VISUAL_EXCLUSIVE("ve"),
  OP_PENDING("o"),
  INSERT("i"),
  REPLACE("r"),
  CMD_LINE("c"),
  CMD_LINE_INSERT("ci"),
  CMD_LINE_REPLACE("cr"),
  SHOW_MATCH("sm"),
  ALL("a");

  override fun toString() = token

  companion object {
    fun fromString(s: String) = values().firstOrNull { it.token == s }

    // Used in FleetVim
    @Suppress("unused")
    fun fromMode(mode: VimStateMachine.Mode, isReplaceCharacter: Boolean): GuiCursorMode {
      if (isReplaceCharacter) {
        // Can be true for NORMAL and VISUAL
        return REPLACE
      }

      return when (mode) {
        VimStateMachine.Mode.COMMAND -> NORMAL
        VimStateMachine.Mode.VISUAL -> VISUAL // TODO: VISUAL_EXCLUSIVE
        VimStateMachine.Mode.SELECT -> VISUAL
        VimStateMachine.Mode.INSERT -> INSERT
        VimStateMachine.Mode.OP_PENDING -> OP_PENDING
        VimStateMachine.Mode.REPLACE -> REPLACE
        // TODO: ci and cr
        VimStateMachine.Mode.CMD_LINE -> CMD_LINE
        VimStateMachine.Mode.INSERT_NORMAL -> NORMAL
        VimStateMachine.Mode.INSERT_VISUAL -> VISUAL
        VimStateMachine.Mode.INSERT_SELECT -> INSERT
      }
    }
  }
}

enum class GuiCursorType(val token: String) {
  BLOCK("block"),
  VER("ver"),
  HOR("hor")
}

class GuiCursorEntry(
  private val originalString: String,
  val modes: EnumSet<GuiCursorMode>,
  val type: GuiCursorType?,
  val thickness: Int?,
  val highlightGroup: String,
  val lmapHighlightGroup: String,
  val blinkModes: List<String>
) {
  override fun toString(): String {
    // We need to match the original string for output and remove purposes
    return originalString
  }
}

data class GuiCursorAttributes(
  val type: GuiCursorType,
  val thickness: Int,
  val highlightGroup: String,
  val lmapHighlightGroup: String,
  val blinkModes: List<String>
)
