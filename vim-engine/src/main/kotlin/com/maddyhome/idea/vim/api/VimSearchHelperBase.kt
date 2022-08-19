package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import kotlin.math.abs
import kotlin.math.min

abstract class VimSearchHelperBase : VimSearchHelper {
  override fun findNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean): Long {
    return findNextWord(editor.charsSequence(), searchFrom.toLong(), editor.fileSize(), count, bigWord, false)
  }

  companion object {
    fun findNextWord(
      chars: CharSequence,
      pos: Long,
      size: Long,
      count: Int,
      bigWord: Boolean,
      spaceWords: Boolean,
    ): Long {
      var _count = count
      val step = if (_count >= 0) 1 else -1
      _count = abs(_count)
      var res = pos
      for (i in 0 until _count) {
        res = findNextWordOne(chars, res, size, step, bigWord, spaceWords)
        if (res == pos || res == 0L || res == size - 1) {
          break
        }
      }
      return res
    }

    // TODO: 18.08.2022 Make private
    fun findNextWordOne(
      chars: CharSequence,
      pos: Long,
      size: Long,
      step: Int,
      bigWord: Boolean,
      spaceWords: Boolean,
    ): Long {
      var found = false
      var _pos = if (pos < size) pos else min(size, (chars.length - 1).toLong())
      // For back searches, skip any current whitespace so we start at the end of a word
      if (step < 0 && _pos > 0) {
        if (charType(chars[_pos - 1], bigWord) === CharacterHelper.CharacterType.WHITESPACE && !spaceWords) {
          _pos = skipSpace(chars, _pos - 1, step, size) + 1
        }
        if (_pos > 0 && charType(chars[_pos], bigWord) !== charType(chars[_pos - 1], bigWord)) {
          _pos += step
        }
      }
      var res = _pos
      if (_pos < 0 || _pos >= size) {
        return _pos
      }
      var type = charType(chars[_pos], bigWord)
      if (type === CharacterHelper.CharacterType.WHITESPACE && step < 0 && _pos > 0 && !spaceWords) {
        type = charType(chars[_pos - 1], bigWord)
      }
      _pos += step
      while (_pos in 0 until size && !found) {
        val newType = charType(chars[_pos], bigWord)
        if (newType !== type) {
          if (newType === CharacterHelper.CharacterType.WHITESPACE && step >= 0 && !spaceWords) {
            _pos = skipSpace(chars, _pos, step, size)
            res = _pos
          } else if (step < 0) {
            res = _pos + 1
          } else {
            res = _pos
          }
          type = charType(chars[res], bigWord)
          found = true
        }
        _pos += step
      }
      if (found) {
        if (res < 0) { // (pos <= 0)
          res = 0
        } else if (res >= size) { // (pos >= size)
          res = size - 1
        }
      } else if (_pos <= 0) {
        res = 0
      } else if (_pos >= size) {
        res = size
      }
      return res
    }

    /**
     * Skip whitespace starting with the supplied position.
     *
     * An empty line is considered a whitespace break.
     */
    // TODO: 18.08.2022 Make private
    fun skipSpace(chars: CharSequence, offset: Long, step: Int, size: Long): Long {
      var _offset = offset
      var prev = 0.toChar()
      while (_offset in 0 until size) {
        val c = chars[_offset.toInt()]
        if (c == '\n' && c == prev) break
        if (charType(c, false) !== CharacterHelper.CharacterType.WHITESPACE) break
        prev = c
        _offset += step
      }
      return if (_offset < size) _offset else size - 1
    }

    operator fun CharSequence.get(index: Long): Char {
      return this[index.toInt()]
    }
  }
}
