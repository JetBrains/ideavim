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

  fun isFilename(editor: VimEditor, c: Char): Boolean {
    // Vim's vim_isfilec treats every multibyte character as a filename character
    if (c.code >= '\u0100'.code) return true
    return isMatchingChar(editor, c, Options.isfname)
  }

  fun isKeyword(editor: VimEditor, c: Char): Boolean {
    // 'iskeyword' only applies to Latin-1 characters. Vim classifies everything above with a fixed table of character
    // classes, and a character is a word character if its class is greater than punctuation (see vim_iswordc_buf and
    // utf_class_buf in mbyte.c)
    if (c.code >= '\u0100'.code) return isWordCharacterClass(c.code)
    return isMatchingChar(editor, c, Options.iskeyword)
  }

  /**
   * Intervals of characters above Latin-1 that Vim classifies as blank (class 0) or punctuation (class 1)
   *
   * Taken from the `classes` table in `utf_class_buf` (mbyte.c), which is the authority on what is a word character
   * above Latin-1. Every character that is not in one of these intervals is a word character. Stored as a flat,
   * sorted, non-overlapping list of (first, last) pairs.
   */
  private val nonWordIntervals = intArrayOf(
    0x037e, 0x037e, // Greek question mark
    0x0387, 0x0387, // Greek ano teleia
    0x055a, 0x055f, // Armenian punctuation
    0x0589, 0x0589, // Armenian full stop
    0x05be, 0x05be,
    0x05c0, 0x05c0,
    0x05c3, 0x05c3,
    0x05f3, 0x05f4,
    0x060c, 0x060c,
    0x061b, 0x061b,
    0x061f, 0x061f,
    0x066a, 0x066d,
    0x06d4, 0x06d4,
    0x0700, 0x070d, // Syriac punctuation
    0x0964, 0x0965,
    0x0970, 0x0970,
    0x0df4, 0x0df4,
    0x0e4f, 0x0e4f,
    0x0e5a, 0x0e5b,
    0x0f04, 0x0f12,
    0x0f3a, 0x0f3d,
    0x0f85, 0x0f85,
    0x104a, 0x104f, // Myanmar punctuation
    0x10fb, 0x10fb, // Georgian punctuation
    0x1361, 0x1368, // Ethiopic punctuation
    0x166d, 0x166e, // Canadian syllabics punctuation
    0x169b, 0x169c,
    0x16eb, 0x16ed,
    0x1735, 0x1736,
    0x17d4, 0x17dc, // Khmer punctuation
    0x1800, 0x180a, // Mongolian punctuation
    0x2000, 0x206f, // spaces and general punctuation. Superscript and subscript that follow are word characters
    0x20a0, 0x27ff, // all kinds of symbols. Braille that follows is a word character
    0x2900, 0x2998, // arrows, brackets, etc.
    0x29d8, 0x29db,
    0x29fc, 0x29fd,
    0x2e00, 0x2e7f, // supplemental punctuation
    0x3000, 0x3020, // ideographic space and punctuation
    0x3030, 0x3030,
    0x303d, 0x303d,
    0xfd3e, 0xfd3f,
    0xfe30, 0xfe6b, // punctuation forms
    0xff00, 0xff0f, // half and full width ASCII punctuation
    0xff1a, 0xff20,
    0xff3b, 0xff40,
    0xff5b, 0xff65,
  )

  private fun isWordCharacterClass(code: Int): Boolean {
    var low = 0
    var high = nonWordIntervals.size / 2 - 1
    while (low <= high) {
      val mid = (low + high) / 2
      when {
        code < nonWordIntervals[mid * 2] -> high = mid - 1
        code > nonWordIntervals[mid * 2 + 1] -> low = mid + 1
        else -> return false
      }
    }
    return true
  }

  private fun isMatchingChar(editor: VimEditor, c: Char, option: StringListOption): Boolean {
    val specs =
      injector.optionGroup.getParsedEffectiveOptionValue(option, editor) { optionValue ->
        valuesToValidatedAndReversedSpecs(parseValues(optionValue.value))!!
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
      if (it.matchAllLetters) {
        allLettersRegex
      } else if (it.rangeLow != null && it.rangeHigh != null) {
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

  fun isValueValid(value: String): Boolean {
    return KeywordSpec(value).isValid
  }

  private class KeywordSpec(private val part: String) {
    private var negate = false
    var matchAllLetters = false
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
        if (keywords.size > 1) {
          rangeLow = toUnicodeOrNull(keywords[0])
          rangeHigh = toUnicodeOrNull(keywords[1])
        } else {
          matchAllLetters = true
        }
      } else {
        toUnicodeOrNull(keywords[0])?.let {
          rangeLow = it
        }
      }
    }

    private fun toUnicodeOrNull(str: String): Int? {
      // If the string is a number, it's a Unicode code point of a letter. If it's not a number, it should be a single
      // character. Otherwise, it's invalid
      return str.toIntOrNull()
        ?: if (Character.codePointCount(str, 0, str.length) == 1) {
          Character.codePointAt(str, 0)
        } else {
          null
        }
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
        val matchSingleLetter = rangeLow != null && rangeHigh == null
        val matchRange = rangeLow != null && rangeHigh != null
        return matchAllLetters || matchSingleLetter || (matchRange && rangeLow!! <= rangeHigh!!)
      }

    fun negate(): Boolean {
      initializeValues()
      return negate
    }

    operator fun contains(code: Int): Boolean {
      initializeValues()
      if (matchAllLetters) {
        return Character.isLetter(code)
      }
      return if (rangeLow != null && rangeHigh != null) {
        code >= rangeLow!! && code <= rangeHigh!!
      } else {
        code == rangeLow
      }
    }
  }
}
