/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.StringListOption
import java.util.regex.Pattern

object KeywordOptionHelper {

  private const val allLettersRegex = "\\p{L}"
  private val validationPattern =
    Pattern.compile("(\\^?(([^0-9^]|[0-9]{1,3})-([^0-9]|[0-9]{1,3})|([^0-9^]|[0-9]{1,3})),)*\\^?(([^0-9^]|[0-9]{1,3})-([^0-9]|[0-9]{1,3})|([^0-9]|[0-9]{1,3})),?$")

  fun isValueInvalid(value: String): Boolean {
    val values = parseValues(value)
    val specs = valuesToValidatedAndReversedSpecs(values)
    return values == null || specs == null
  }

  fun isFilename(editor: VimEditor, c: Char): Boolean  = isMatchingChar(editor, c, Options.isfname)
  fun isKeyword(editor: VimEditor, c: Char) = isMatchingChar(editor, c, Options.iskeyword)

  private fun isMatchingChar(editor: VimEditor, c: Char, option: StringListOption): Boolean {
    if (c.code >= '\u0100'.code) {
      return true
    }

    val specs =
      injector.optionGroup.getParsedEffectiveOptionValue(option, editor) { optionValue ->
        valuesToValidatedAndReversedSpecs(parseValues(optionValue.asString()))!!
      }
    for (spec in specs) {
      if (spec.contains(c.code)) {
        return !spec.negate()
      }
    }
    return false
  }

  // TODO: Come up with a more friendly API for IdeaVim-EasyMotion
  // Perhaps pass in VimEditor, or allow retrieving the list of KeywordSpec
  @Deprecated("Only maintained for compatibility. Does not handle local-to-buffer iskeyword option")
  fun toRegex(): List<String> {
    // 'iskeyword' is a local-to-buffer option, but we're not passed an editor. We have to use the global value. We also
    // have to use the fallback window to avoid any asserts about accessing a non-global option as a global option.
    // This is not ideal and should not be replicated in non-deprecated code
    val isKeyword =
      injector.optionGroup.getOptionValue(Options.iskeyword, OptionAccessScope.GLOBAL(injector.fallbackWindow)).value
    val specs = valuesToValidatedAndReversedSpecs(parseValues(isKeyword)) ?: emptyList()
    return specs.map {
      it.initializeValues()
      if (it.isAllLetters) {
        allLettersRegex
      } else if (it.isRange) {
        "[" + it.rangeLow!!.toChar() + "-" + it.rangeHigh!!.toChar() + "]"
      } else {
        it.rangeLow!!.toChar().toString()
      }
    }
  }

  fun parseValues(content: String): List<String>? {
    if (!validationPattern.matcher(content).matches()) {
      return null
    }
    var index = 0
    var firstCharNumOfPart = true
    var inRange = false
    val vals: MutableList<String> = ArrayList()
    var option = StringBuilder()

    // We need to split the input string into parts. However, we can't just split on a comma
    // since a comma can either be a keyword or a separator depending on its location in the string.
    while (index <= content.length) {
      var curChar = 0.toChar()
      if (index < content.length) {
        curChar = content[index]
      }
      index++

      // If we either have a comma separator or are at the end of the content...
      if (curChar == ',' && !firstCharNumOfPart && !inRange || index == content.length + 1) {
        val part = option.toString()
        vals.add(part)
        option = StringBuilder()
        inRange = false
        firstCharNumOfPart = true
        continue
      }
      option.append(curChar)
      if (curChar == '^' && option.length == 1) {
        firstCharNumOfPart = true
        continue
      }
      if (curChar == '-' && !firstCharNumOfPart) {
        inRange = true
        continue
      }
      firstCharNumOfPart = false
      inRange = false
    }
    return vals
  }

  private fun valuesToValidatedAndReversedSpecs(values: List<String>?): List<KeywordSpec>? {
    val specs: MutableList<KeywordSpec> = mutableListOf()
    if (values != null) {
      for (value in values) {
        val spec = KeywordSpec(value)
        if (!spec.isValid) {
          return null
        }
        specs.add(spec)
      }
      specs.reverse()
    }
    return specs
  }

  private class KeywordSpec(private val part: String) {
    private var negate = false
    var isRange = false
    var isAllLetters = false
    var rangeLow: Int? = null
    var rangeHigh: Int? = null
    private var initialized = false

    fun initializeValues() {
      if (initialized) return
      initialized = true
      var part = part
      negate = part.matches(Regex("^\\^.+"))
      if (negate) {
        part = part.substring(1)
      }
      val keywords = part.split("(?<=.)-(?=.+)".toRegex()).toTypedArray()
      if (keywords.size > 1 || keywords[0] == "@") {
        isRange = true
        if (keywords.size > 1) {
          rangeLow = toUnicode(keywords[0])
          rangeHigh = toUnicode(keywords[1])
        } else {
          isAllLetters = true
        }
      } else {
        val keyword = toUnicode(keywords[0])
        rangeLow = keyword
        rangeHigh = keyword
      }
    }

    private fun toUnicode(str: String): Int {
      return str.toIntOrNull() // If we have a number, it represents the Unicode code point of a letter
        ?: str[0].code // If it's not a number we should only have strings consisting of one char
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || javaClass != other.javaClass) return false
      val that = other as KeywordSpec
      return part == that.part
    }

    override fun hashCode(): Int {
      return part.hashCode()
    }

    val isValid: Boolean
      get() {
        initializeValues()
        return !isRange || isAllLetters || rangeLow!! <= rangeHigh!!
      }

    fun negate(): Boolean {
      initializeValues()
      return negate
    }

    operator fun contains(code: Int): Boolean {
      initializeValues()
      if (isAllLetters) {
        return Character.isLetter(code)
      }
      return if (isRange) {
        code >= rangeLow!! && code <= rangeHigh!!
      } else {
        code == rangeLow
      }
    }
  }
}
