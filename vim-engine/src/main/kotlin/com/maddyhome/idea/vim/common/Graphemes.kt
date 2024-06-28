package com.maddyhome.idea.vim.common

//
// RATIONALE:
//
// As an alternative to implementing this ourselves, we could make use of the ICU4J — the implementation
// of the unicode related properties and algorithms maintained by the unicode organisation.
//
// The reason why it wasn't done so is twofold:
//   1. ICU4J is a fairly big library, that provides the complete support for the Unicode specification,
//      while at this moment all we need is a small subset of that functionality, namely the grapheme
//      cluster boundaries search.
//
//   2. The exposed API is a little awkward to use and would require adapters to be efficient.
//      To iterate over the grapheme cluster boundaries using ICU4J, one could employ the implementation
//      of the `java.text.BreakIterator` provided by the library.
//
//      Given the specifics of the memory access patterns in the vim-engine with respect to grapheme
//      cluster boundaries search (random access, only certain commands will ever care about the boundaries),
//      using the sequential `BreakIterator` isn't very efficient. It also does quite a bit of extra
//      work to enable faster random access of the areas that were visited before by maintaining a partial
//      index.
//
//      JDK21 exposes a similar implementation of the `java.text.BreakIterator` interface, except it's
//      even less efficent: upon the iterator creation, the full traversal of the given text is performed
//      to build a full index over all the grapheme cluster boundaries, making the use of the said iterator
//      O(n) both by time and memory.
//
//      We could still consider that option just for the sake of reducing the amount of code that we have
//      to maintain, once JDK21 is released. The ineffiency can be reduced by taking a small fragment of
//      the text and increase the size if no boundaries were found, although the amount of extra work
//      is still substantial.
//
//      Ironically, one of the packages of the JDK21 has almost exactly what we need (a very similar API
//      to the one implemented here), but it is an internal package (`jdk.internal.util.regex`).
//

/**
 * Move over unicode extended grapheme cluster boundaries.
 *
 * https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries
 */
object Graphemes {
  /**
   * Returns the next extended grapheme cluster boundary or `null` if the end of text has been reached.
   */
  fun next(charSeq: CharSequence, start: Int): Int? {
    require(start >= 0) { "'start' is out of bounds." }

    if (start >= charSeq.length) return null

    return charSeq.nextBoundary(
      start,
      next = Int::plus,
      nextCode = { if (it < length) Character.codePointAt(this, it) else null },
      prevCode = { current, _ -> if (current > 0) Character.codePointBefore(this, current) else null },
      swap = false,
    )
  }

  /**
   * Returns the previous extended grapheme cluster boundary or `null` if the start of text has been reached.
   */
  fun prev(charSeq: CharSequence, start: Int): Int? {
    require(start <= charSeq.length) { "'start' is out of bounds" }

    if (start <= 0) return null

    return charSeq.nextBoundary(
      start,
      next = Int::minus,
      nextCode = { if (it > 0) Character.codePointBefore(this, it) else null },
      prevCode = { current, charCount ->
        if (current - charCount > 0) Character.codePointBefore(this, current - charCount)
        else null
      },
      swap = true,
    )
  }
}

private inline fun CharSequence.nextBoundary(
  start: Int,
  crossinline next: (Int, Int) -> Int,
  crossinline nextCode: CharSequence.(Int) -> Int?,
  crossinline prevCode: CharSequence.(Int, Int) -> Int?,
  swap: Boolean,
): Int {
  var current = start
  while (true) {
    var codePoint = nextCode(current) ?: return current
    val charCount = Character.charCount(codePoint)
    var nextCodePoint = nextCode(next(current, charCount)) ?: return next(current, charCount)
    val nextCharCount = Character.charCount(nextCodePoint)

    // Below the two code points are inspected in the direct order, following the grapheme breaking rules.
    // To not duplicate the rules depending on the traversal direction, we ensure that the two code points
    // are inspected in the same order by swapping them when we are traversing backwards.
    if (swap) {
      val temp = codePoint
      codePoint = nextCodePoint
      nextCodePoint = temp
    }

    val type = classify(codePoint)
    val nextType = classify(nextCodePoint)

    // GB3 - do not break CR x LF.
    if (type == CodePointType.CR && nextType == CodePointType.LF) {
      current = next(current, charCount)
      continue
    }

    // GB4 - break after Control, CR or LF.
    if (type in arrayOf(CodePointType.CONTROL, CodePointType.CR, CodePointType.LF)) {
      return next(current, charCount)
    }

    // GB5 - break before Control, CR or LF.
    if (nextType in arrayOf(CodePointType.CONTROL, CodePointType.CR, CodePointType.LF)) {
      return next(current, charCount)
    }

    // GB6 - do not break Hangul syllable sequence.
    if (type == CodePointType.L && nextType in arrayOf(
        CodePointType.L,
        CodePointType.V,
        CodePointType.LV,
        CodePointType.LVT
      )
    ) {
      current = next(current, charCount)
      continue
    }

    // GB7 - ditto.
    if ((type == CodePointType.LV || type == CodePointType.V) && (nextType == CodePointType.V || nextType == CodePointType.T)) {
      current = next(current, charCount)
      continue
    }

    // GB8 - ditto.
    if ((type == CodePointType.LVT || type == CodePointType.T) && nextType == CodePointType.T) {
      current = next(current, charCount)
      continue
    }

    // GB9, GB9a, GB9b - do not break before extending characters or ZWJ.
    if (type == CodePointType.PREPEND || nextType in arrayOf(
        CodePointType.EXTEND,
        CodePointType.ZWJ,
        CodePointType.SPACING_MARK
      )
    ) {
      current = next(current, charCount)
      continue
    }

    // GB11 - do not break within emoji modifier sequneces or emoji ZWJ sequences.
    if (type == CodePointType.EXTENDED_PICTOGRAPHIC) {
      if (nextType in arrayOf(CodePointType.EXTEND, CodePointType.ZWJ)) {
        current = next(current, charCount)
        continue
      }
    }

    if (type == CodePointType.EXTEND && nextType in arrayOf(CodePointType.EXTEND, CodePointType.ZWJ)) {
      current = next(current, charCount)
      continue
    }

    if (type == CodePointType.ZWJ && nextType == CodePointType.EXTENDED_PICTOGRAPHIC) {
      // Unlike nextCode, which will return either the one to the right (in direct order) or the one to the left
      // (in reverse order) code point with respect to `current', prevCode will always return a code preceeding
      // the two currently inspected (the one to the left of both of them).
      val prevCodePoint = prevCode(current, charCount + nextCharCount)
      if (prevCodePoint != null) {
        val prevType = classify(prevCodePoint)

        if (prevType == CodePointType.EXTEND || prevType == CodePointType.EXTENDED_PICTOGRAPHIC) {
          current = next(current, charCount)
          continue
        }
      }
    }

    // GB12, GB13 - do not break within emoji flag sequences.
    if (type == CodePointType.REGIONAL_INDICATOR) {
      val count = countPrev(current) { classify(it) == CodePointType.REGIONAL_INDICATOR }
      if (nextType == CodePointType.REGIONAL_INDICATOR && count % 2 == 0) {
        current = next(current, charCount)
        continue
      }
    }

    // GB999 - otherwise, break everywhere.
    return next(current, charCount)
  }
}

private inline fun CharSequence.countPrev(start: Int, crossinline pred: (Int) -> Boolean): Int {
  var current = start
  var count = 0
  while (current > 0) {
    val codePoint = Character.codePointBefore(this, current)
    if (!pred(codePoint)) {
      break
    }
    current -= Character.charCount(codePoint)
    count++
  }
  return count
}

/**
 * Returns the grapheme cluster break property value for the given code point.
 *
 * https://www.unicode.org/Public/UCD/latest/ucd/auxiliary/GraphemeBreakProperty.txt
 */
private fun classify(codePoint: Int): CodePointType {
  when (codePoint) {
    0xD -> return CodePointType.CR
    0xA -> return CodePointType.LF
    in 0 until 0x20 -> return CodePointType.CONTROL
    in 0 until 0x80 -> return CodePointType.OTHER
  }

  if (isExtendedPictographic(codePoint)) {
    return CodePointType.EXTENDED_PICTOGRAPHIC
  }

  val type = Character.getType(codePoint).toByte()
  return when (type) {
    Character.UNASSIGNED -> when (codePoint) {
      in 0x2064..0x2069, in 0xFFF0..0xFFF8, 0xE0000, in 0xE0002..0xE001F, in 0xE0080..0xE00FF, in 0xE01F0..0xE0FFF -> CodePointType.CONTROL
      else -> CodePointType.OTHER
    }

    Character.MODIFIER_LETTER, Character.MODIFIER_SYMBOL -> when (codePoint) {
      0xFF9E, 0xFF9F, in 0x1F3FB..0x1F3FF -> CodePointType.EXTEND
      else -> CodePointType.OTHER
    }

    Character.FORMAT -> when (codePoint) {
      0x200D -> CodePointType.ZWJ
      in 0x0600..0x0605, 0x06DD, 0x070F, in 0x0890..0x0891, 0x08E2, 0x110BD, 0x110CD -> CodePointType.PREPEND
      0x200C, in 0xE0020..0xE007F -> CodePointType.EXTEND
      else -> CodePointType.CONTROL
    }

    Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR, Character.CONTROL -> CodePointType.CONTROL

    Character.OTHER_LETTER -> when (codePoint) {
      0x0D4E, in 0x111C2..0x111C3, 0x1193F, 0x11941, 0x11A3A, in 0x11A84..0x11A89, 0x11D46, 0x11F02 -> CodePointType.PREPEND
      0x0E33, 0x0EB3 -> CodePointType.SPACING_MARK
      in 0x1100..0x115F, in 0xA960..0xA97C -> CodePointType.L
      in 0x1160..0x11A7, in 0xD7B0..0xD7C6 -> CodePointType.V
      in 0x11A8..0x11FF, in 0xD7CB..0xD7FB -> CodePointType.T
      // LV is encountered every 28 characters, everything in-between is LVT. 
      in 0xAC00..0xD7A3 -> if ((codePoint - 0xAC00) % 28 == 0) CodePointType.LV else CodePointType.LVT
      else -> CodePointType.OTHER
    }

    Character.OTHER_SYMBOL -> when (codePoint) {
      in 0x1F1E6..0x1F1FF -> CodePointType.REGIONAL_INDICATOR
      else -> CodePointType.OTHER
    }

    Character.NON_SPACING_MARK, Character.ENCLOSING_MARK -> CodePointType.EXTEND

    Character.COMBINING_SPACING_MARK -> when (codePoint) {
      0x09BE, 0x09D7, 0x0b3E, 0x0B57, 0x0BBE, 0x0BD7, 0x0CC2, in 0x0CD5..0x0CD6, 0x0D3E, 0x0D57, 0x0DCF,
      0x0DDF, 0x1B35, in 0x302E..0x302F, 0x1133E, 0x11357, 0x114B0, 0x114BD, 0x115AF, 0x11930, 0x1D165,
      in 0x1D16E..0x1D172 -> CodePointType.EXTEND

      0x102B, 0x102C, 0x1038, in 0x1062..0x1064, in 0x1067..0x106D, 0x1083, in 0x1087..0x108C, 0x108F, in 0x109A..0x109C,
      0x1A61, 0x1A63, 0x1A64, 0xAA7B, 0xAA7D -> CodePointType.OTHER

      else -> CodePointType.SPACING_MARK
    }

    else -> CodePointType.OTHER
  }
}

private enum class CodePointType {
  CR,
  LF,
  CONTROL,
  EXTEND,
  ZWJ,
  REGIONAL_INDICATOR,
  PREPEND,
  SPACING_MARK,
  EXTENDED_PICTOGRAPHIC,
  L,
  V,
  T,
  LV,
  LVT,
  OTHER,
}

