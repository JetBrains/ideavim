/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

/**
 * Text wrapper for gq/gw.
 *
 * Pipeline for each paragraph:
 *   1. Match the leader on the first line against [leaders].
 *   2. Grow the paragraph forward as long as `sameLeader` allows and the
 *      next line is not a paragraph boundary.
 *   3. Join: first line kept intact; subsequent lines contribute their
 *      content after their leader is stripped.
 *   4. Split the joined line at [width], re-inserting a continuation
 *      prefix derived from the first line's leader (s→m transition,
 *      f-flag space padding, or verbatim repeat).
 */
class CodeWrapper(
  private val width: Int = 80,
  private val tabWidth: Int = 4,
  private val leaders: List<CommentLeader>,
) {
  private val leadersLongestFirst = leaders.sortedByDescending { it.text.length }
  private val middleByStart: Map<CommentLeader, CommentLeader> = pairStartsWithMiddle()

  fun wrap(text: String?): String {
    if (text.isNullOrEmpty()) return ""
    val trailingNewline = text.endsWith("\n")
    val body = if (trailingNewline) text.dropLast(1) else text
    val formatted = formatLines(body.split("\n")).joinToString("\n")
    return if (trailingNewline) "$formatted\n" else formatted
  }

  private fun formatLines(lines: List<String>): List<String> {
    val out = mutableListOf<String>()
    var startLineIndex = 0
    while (startLineIndex < lines.size) {
      val firstLine = lines[startLineIndex]
      val firstMatch = matchLeader(firstLine)
      if (isEndPar(firstLine, firstMatch)) {
        out.add(firstLine.trimEnd())
        startLineIndex++
        continue
      }
      val endLineIndex = findParagraphEnd(lines, startLineIndex, firstLine, firstMatch)
      val joined = joinParagraph(lines, startLineIndex, endLineIndex)
      val prefix = continuationPrefix(firstLine, firstMatch)
      out.addAll(splitAtWidth(joined, prefix))
      startLineIndex = endLineIndex + 1
    }
    return out
  }

  private fun findParagraphEnd(
    lines: List<String>,
    start: Int,
    firstLine: String,
    firstMatch: MatchedLeader?,
  ): Int {
    var end = start
    while (end + 1 < lines.size) {
      val nextLine = lines[end + 1]
      val nextMatch = matchLeader(nextLine)
      if (isEndPar(nextLine, nextMatch)) break
      if (!sameLeader(firstLine, firstMatch, nextLine, nextMatch)) break
      end++
    }
    return end
  }

  private fun joinParagraph(lines: List<String>, from: Int, to: Int): String {
    if (from == to) return lines[from]
    val sb = StringBuilder(lines[from].trimEnd())
    for (lineIndex in (from + 1)..to) {
      val line = lines[lineIndex]
      val match = matchLeader(line)
      val content = (if (match != null) line.substring(match.trailingEnd) else line.trimStart()).trim()
      if (content.isEmpty()) continue
      sb.append(' ').append(content)
    }
    return sb.toString()
  }

  private fun matchLeader(line: String): MatchedLeader? {
    var indentEnd = 0
    while (indentEnd < line.length && isAsciiWhite(line[indentEnd])) indentEnd++
    if (indentEnd >= line.length) return null
    for (leader in leadersLongestFirst) {
      if (!line.regionMatches(indentEnd, leader.text, 0, leader.text.length)) continue
      val afterLeader = indentEnd + leader.text.length
      if (leader.requiresBlank && afterLeader < line.length && !isAsciiWhite(line[afterLeader])) continue
      var trailingEnd = afterLeader
      while (trailingEnd < line.length && isAsciiWhite(line[trailingEnd])) trailingEnd++
      return MatchedLeader(leader, indentEnd, afterLeader, trailingEnd)
    }
    return null
  }

  private fun isEndPar(line: String, match: MatchedLeader?): Boolean {
    if (match != null && match.leader.isEnd) return true
    val start = match?.trailingEnd ?: firstNonWhite(line)
    return start >= line.length
  }

  private fun sameLeader(
    line1: String,
    m1: MatchedLeader?,
    line2: String,
    m2: MatchedLeader?,
  ): Boolean {
    if (m1 == null) return m2 == null
    val leader = m1.leader
    if (leader.hasNoContinuation) return m2 == null
    if (leader.isEnd) return false
    if (leader.isStart) return m2 != null && m2.leader.isMiddle
    if (m2 == null) return false
    return line1.substring(m1.indentEnd, m1.leaderEnd) == line2.substring(m2.indentEnd, m2.leaderEnd)
  }

  private fun continuationPrefix(firstLine: String, match: MatchedLeader?): String {
    if (match == null) return firstLine.substring(0, firstNonWhite(firstLine))
    val leader = match.leader
    val leadingWs = firstLine.substring(0, match.indentEnd)
    if (leader.isStart) {
      val mid = middleByStart[leader]
      if (mid != null) return applyOffset(leadingWs, leader.offset) + mid.text + " "
    }
    if (leader.hasNoContinuation) return " ".repeat(match.trailingEnd)
    return firstLine.substring(0, match.trailingEnd)
  }

  private fun applyOffset(ws: String, offset: Int): String {
    if (offset >= 0) return ws + " ".repeat(offset)
    return ws.dropLast(minOf(-offset, ws.length))
  }

  private fun splitAtWidth(text: String, continuation: String): List<String> {
    if (width <= 0) return listOf(text.trimEnd())
    val out = mutableListOf<String>()
    var rest = text
    var first = true
    while (true) {
      val line = if (first) rest else continuation + rest
      if (visualWidth(line) <= width) {
        out.add(line.trimEnd())
        break
      }
      val leaderEnd = if (first) {
        matchLeader(line)?.trailingEnd ?: firstNonWhite(line)
      } else {
        continuation.length
      }
      val breakAt = findBreak(line, leaderEnd)
      if (breakAt < 0) {
        out.add(line.trimEnd())
        break
      }
      out.add(line.substring(0, breakAt).trimEnd())
      rest = line.substring(breakAt).trimStart()
      if (rest.isEmpty()) break
      first = false
    }
    return out
  }

  private fun findBreak(line: String, leaderEnd: Int): Int {
    var bestBefore = -1
    var firstAfter = -1
    var col = 0
    for (pos in line.indices) {
      val ch = line[pos]
      if (isAsciiWhite(ch) && pos >= leaderEnd) {
        if (col <= width) bestBefore = pos
        else if (firstAfter < 0) firstAfter = pos
      }
      col += if (ch == '\t') tabWidth - (col % tabWidth) else 1
    }
    return if (bestBefore >= 0) bestBefore else firstAfter
  }

  private fun visualWidth(text: String): Int {
    var col = 0
    for (ch in text) col += if (ch == '\t') tabWidth - (col % tabWidth) else 1
    return col
  }

  private fun isAsciiWhite(ch: Char): Boolean = ch == ' ' || ch == '\t'

  private fun firstNonWhite(s: String): Int {
    for (pos in s.indices) if (!isAsciiWhite(s[pos])) return pos
    return s.length
  }

  private fun pairStartsWithMiddle(): Map<CommentLeader, CommentLeader> {
    val out = mutableMapOf<CommentLeader, CommentLeader>()
    for ((startIndex, leader) in leaders.withIndex()) {
      if (!leader.isStart) continue
      for (candidate in (startIndex + 1) until leaders.size) {
        if (leaders[candidate].isMiddle) { out[leader] = leaders[candidate]; break }
      }
    }
    return out
  }

  private data class MatchedLeader(
    val leader: CommentLeader,
    val indentEnd: Int,
    val leaderEnd: Int,
    val trailingEnd: Int,
  )
}
