/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*

public object GuiCursorOptionHelper {
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

    return GuiCursorEntry(modes, type, thickness, highlightGroup, lmapHighlightGroup, blinkModes)
  }

  public fun getAttributes(mode: GuiCursorMode): GuiCursorAttributes {
    val attributes = injector.optionGroup.getParsedEffectiveOptionValue(Options.guicursor, OptionScope.GLOBAL, ::parseGuicursor)
    return attributes[mode] ?: GuiCursorAttributes.DEFAULT
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
      GuiCursorMode.values().filter { it != GuiCursorMode.ALL }.forEach {
        updateMode(it, entry)
      }
    }

    fun build() = builders.map { it.key to it.value.build() }.toMap()
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
  public val modes: EnumSet<GuiCursorMode>,
  public val type: GuiCursorType?,
  public val thickness: Int?,
  public val highlightGroup: String,
  public val lmapHighlightGroup: String,
  public val blinkModes: List<String>,
)

public data class GuiCursorAttributes(
  val type: GuiCursorType,
  val thickness: Int,
  val highlightGroup: String,
  val lmapHighlightGroup: String,
  val blinkModes: List<String>,
) {
  public companion object {
    public val DEFAULT: GuiCursorAttributes = GuiCursorAttributes(GuiCursorType.BLOCK,
      thickness = 0,
      highlightGroup = "",
      lmapHighlightGroup = "",
      blinkModes = emptyList()
    )
  }
}

