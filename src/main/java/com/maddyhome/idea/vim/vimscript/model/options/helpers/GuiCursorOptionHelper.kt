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

package com.maddyhome.idea.vim.vimscript.model.options.helpers

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.services.OptionConstants
import com.maddyhome.idea.vim.vimscript.services.OptionService
import java.util.*

object GuiCursorOptionHelper {

  private val effectiveValues = mutableMapOf<GuiCursorMode, GuiCursorAttributes>()

  fun convertToken(token: String): GuiCursorEntry {
    val split = token.split(':')
    if (split.size == 1) {
      throw ExException.message("E545", token)
    }
    if (split.size != 2) {
      throw ExException.message("E546", token)
    }
    val modeList = split[0]
    val argumentList = split[1]

    val modes = enumSetOf<GuiCursorMode>()
    modes.addAll(
      modeList.split('-').map {
        GuiCursorMode.fromString(it) ?: throw ExException.message("E546", token)
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
          thickness = it.slice(3 until it.length).toIntOrNull() ?: throw ExException.message("E548", token)
          if (thickness == 0) {
            throw ExException.message("E549", token)
          }
        }
        it.startsWith("hor") -> {
          type = GuiCursorType.HOR
          thickness = it.slice(3 until it.length).toIntOrNull() ?: throw ExException.message("E548", token)
          if (thickness == 0) {
            throw ExException.message("E549", token)
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
      (VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, OptionConstants.guicursorName) as VimString).value
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
