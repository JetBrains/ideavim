package com.maddyhome.idea.vim.vimscript.model.options.helpers

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.option.GuiCursorAttributes
import com.maddyhome.idea.vim.option.GuiCursorEntry
import com.maddyhome.idea.vim.option.GuiCursorMode
import com.maddyhome.idea.vim.option.GuiCursorType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.services.OptionService

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

    var type = GuiCursorType.BLOCK
    var thickness = 0
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

    return GuiCursorEntry(token, modes, GuiCursorAttributes(type, thickness, highlightGroup, lmapHighlightGroup, blinkModes))
  }

  fun getAttributes(mode: GuiCursorMode): GuiCursorAttributes {
    return effectiveValues.computeIfAbsent(mode) {
      var type = GuiCursorType.BLOCK
      var thickness = 0
      var highlightGroup = ""
      var lmapHighlightGroup = ""
      var blinkModes = emptyList<String>()
      (VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, "guicursor", null) as VimString).value
        .split(",")
        .map { convertToken(it) }
        .forEach { state ->
          if (state.modes.contains(mode) || state.modes.contains(GuiCursorMode.ALL)) {
            type = state.attributes.type
            thickness = state.attributes.thickness
            if (state.attributes.highlightGroup.isNotEmpty()) {
              highlightGroup = state.attributes.highlightGroup
            }
            if (state.attributes.lmapHighlightGroup.isNotEmpty()) {
              lmapHighlightGroup = state.attributes.lmapHighlightGroup
            }
            if (state.attributes.blinkModes.isNotEmpty()) {
              blinkModes = state.attributes.blinkModes
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
