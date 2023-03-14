/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

public object GuiCursorOptionHelper {

  private val effectiveValues = mutableMapOf<GuiCursorMode, GuiCursorAttributes>()

  public fun convertToken(token: String): GuiCursorEntry {
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
      },
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

  public fun getAttributes(mode: GuiCursorMode): GuiCursorAttributes {
    return effectiveValues.computeIfAbsent(mode) {
      var type = GuiCursorType.BLOCK
      var thickness = 0
      var highlightGroup = ""
      var lmapHighlightGroup = ""
      var blinkModes = emptyList<String>()
      injector.globalOptions().getStringListValues(Options.guicursor)
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

  public fun clearEffectiveValues() {
    effectiveValues.clear()
  }
}

public enum class GuiCursorMode(public val token: String) {
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
  ALL("a"),
  ;

  public override fun toString(): String = token

  public companion object {
    public fun fromString(s: String): GuiCursorMode? = values().firstOrNull { it.token == s }

    // Used in FleetVim
    @Suppress("unused")
    public fun fromMode(mode: VimStateMachine.Mode, isReplaceCharacter: Boolean): GuiCursorMode {
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

public enum class GuiCursorType(public val token: String) {
  BLOCK("block"),
  VER("ver"),
  HOR("hor"),
}

public class GuiCursorEntry(
  private val originalString: String,
  public val modes: EnumSet<GuiCursorMode>,
  public val type: GuiCursorType?,
  public val thickness: Int?,
  public val highlightGroup: String,
  public val lmapHighlightGroup: String,
  public val blinkModes: List<String>,
) {
  public override fun toString(): String {
    // We need to match the original string for output and remove purposes
    return originalString
  }
}

public data class GuiCursorAttributes(
  val type: GuiCursorType,
  val thickness: Int,
  val highlightGroup: String,
  val lmapHighlightGroup: String,
  val blinkModes: List<String>,
)
