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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*

object GuiCursorOptionHelper {
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

    return GuiCursorEntry(modes, type, thickness, highlightGroup, lmapHighlightGroup, blinkModes)
  }

  fun getAttributes(mode: GuiCursorMode): GuiCursorAttributes {
    val attributes = injector.optionGroup.getParsedEffectiveOptionValue(Options.guicursor, null, ::parseGuicursor)

    // `ve` falls back to `v` if not specified
    return attributes[mode]
      ?: (if (mode == GuiCursorMode.VISUAL_EXCLUSIVE) attributes[GuiCursorMode.VISUAL] else null)
      ?: GuiCursorAttributes.DEFAULT
  }

  private fun parseGuicursor(guicursor: VimString) = GuiCursorAttributeBuilders().also { builders ->
    // Split into entries. Each entry has a list of modes and various attributes and adds to/overrides current values
    Options.guicursor.split(guicursor.asString()).map { convertToken(it) }
      .forEach { entry ->
        entry.modes.forEach {
          if (it == GuiCursorMode.ALL) {
            builders.updateAllModes(entry)
          } else {
            builders.updateMode(it, entry)
          }
        }
      }
  }.build()

  private class GuiCursorAttributeBuilders {
    private class GuiCursorAttributesBuilder {
      private var type: GuiCursorType = GuiCursorType.BLOCK
      private var thickness: Int = 0
      private var highlightGroup: String = ""
      private var lmapHighlightGroup: String = ""
      private var blinkModes: List<String> = emptyList()

      fun updateFrom(entry: GuiCursorEntry) {
        if (entry.type != null) {
          type = entry.type
        }
        if (entry.thickness != null) {
          thickness = entry.thickness
        }
        if (entry.highlightGroup.isNotEmpty()) {
          highlightGroup = entry.highlightGroup
        }
        if (entry.lmapHighlightGroup.isNotEmpty()) {
          lmapHighlightGroup = entry.lmapHighlightGroup
        }
        if (entry.blinkModes.isNotEmpty()) {
          blinkModes = entry.blinkModes
        }
      }

      fun build() = GuiCursorAttributes(type, thickness, highlightGroup, lmapHighlightGroup, blinkModes)
    }

    private val builders = mutableMapOf<GuiCursorMode, GuiCursorAttributesBuilder>()

    fun updateMode(mode: GuiCursorMode, entry: GuiCursorEntry) {
      builders.getOrPut(mode) { GuiCursorAttributesBuilder() }.updateFrom(entry)
    }

    fun updateAllModes(entry: GuiCursorEntry) {
      GuiCursorMode.entries.filter { it != GuiCursorMode.ALL }.forEach {
        updateMode(it, entry)
      }
    }

    fun build() = builders.map { it.key to it.value.build() }.toMap()
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
  ALL("a"),
  ;

  override fun toString(): String = token

  companion object {
    fun fromString(s: String): GuiCursorMode? = entries.firstOrNull { it.token == s }

    // Also used in FleetVim as direct call
    fun fromMode(mode: Mode, isReplaceCharacter: Boolean): GuiCursorMode {
      if (isReplaceCharacter) {
        // Can be true for NORMAL and VISUAL
        return REPLACE
      }

      // TODO: SELECT should behave the same as VISUAL
      // Note that IdeaVim incorrectly treats Select mode as exclusive at all times, regardless of the 'selection'
      // option. Previously, we would use the Insert cursor to try to make Select mode feel more intuitive, like a
      // "traditional" Windows-like selection, especially with 'idearefactormode' defaults and live template fields
      // during a refactoring. However, at some point, we need to fix the exclusive/inclusive nature, and then the caret
      // shape will be more important - if the selection is inclusive, a bar caret on the last (selected) character will
      // look weird
      return when (mode) {
        is Mode.NORMAL -> NORMAL
        is Mode.OP_PENDING -> OP_PENDING
        Mode.INSERT -> INSERT
        Mode.REPLACE -> REPLACE
        is Mode.SELECT -> VISUAL_EXCLUSIVE  // TODO: Should match VISUAL
        is Mode.VISUAL -> if (injector.globalOptions().selection == "exclusive") VISUAL_EXCLUSIVE else VISUAL
        // This doesn't handle ci and cr, but we don't care - our CMD_LINE will never call this
        is Mode.CMD_LINE -> CMD_LINE
      }
    }
  }
}

enum class GuiCursorType(val token: String) {
  BLOCK("block"),
  VER("ver"),
  HOR("hor"),
}

class GuiCursorEntry(
  val modes: EnumSet<GuiCursorMode>,
  val type: GuiCursorType?,
  val thickness: Int?,
  val highlightGroup: String,
  val lmapHighlightGroup: String,
  val blinkModes: List<String>,
)

data class GuiCursorAttributes(
  val type: GuiCursorType,
  val thickness: Int,
  val highlightGroup: String,
  val lmapHighlightGroup: String,
  val blinkModes: List<String>,
) {
  companion object {
    val DEFAULT: GuiCursorAttributes = GuiCursorAttributes(
      GuiCursorType.BLOCK,
      thickness = 0,
      highlightGroup = "",
      lmapHighlightGroup = "",
      blinkModes = emptyList()
    )
  }
}
