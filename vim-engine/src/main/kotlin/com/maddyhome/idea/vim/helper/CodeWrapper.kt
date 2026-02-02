/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import java.util.regex.Pattern

/**
 * Code-aware text wrapper for Vim gq/gw commands.
 *
 * Wraps text to specified width while preserving comment markers, indentation,
 * and paragraph structure. Inspired by Emacs fill-paragraph and Nir Soffer's
 * codewrap library.
 */
class CodeWrapper(
  private val width: Int = 80,
  private val tabWidth: Int = 4,
) {
  private val inlineCommentRegex = "/\\*+|\\*/|\\*|\\.|#+|//+!?|;+|--|>".toRegex()
  private val docstringRegex = "\"\"\"|'''".toRegex()
  private val commentRegex = "($inlineCommentRegex)|($docstringRegex)".toRegex()
  private val newlineRegex = "(\\r?\\n)".toRegex()
  private val htmlSeparatorsRegex = "<[pP]>|<[bB][rR] ?/?>".toRegex()
  private val tabPlaceholder = "☃"
  private val emptyCommentRegex = "(\\s|($tabPlaceholder))*($commentRegex)?\\s*($htmlSeparatorsRegex)?".toRegex()

  private val paragraphSeparatorPattern: Pattern = Pattern.compile(
    "($newlineRegex)($emptyCommentRegex)($newlineRegex)"
  )
  private val emptyCommentLinePattern: Pattern = Pattern.compile("^($emptyCommentRegex)$", Pattern.MULTILINE)

  private val indentRegex = "^(\\s|$tabPlaceholder)*($inlineCommentRegex)?\\s*($htmlSeparatorsRegex)?"
  private val indentPattern: Pattern = Pattern.compile(indentRegex)

  private val lineSeparator = "\n"

  /**
   * Data about a line split into indent and content portions.
   */
  data class LineData(
    val indent: String,
    val rest: String,
  )

  /**
   * Wrap text to the configured width.
   *
   * Preserves paragraph structure (separated by empty lines) and handles
   * comment markers appropriately.
   *
   * @param text the text to wrap, which may contain multiple paragraphs
   * @return text wrapped to configured width
   */
  fun wrap(text: String?): String {
    if (text.isNullOrEmpty()) return ""

    val expandedTabPlaceholder = tabPlaceholder.repeat(tabWidth)
    val textWithTabPlaceholders = text.replace("\t", expandedTabPlaceholder)
    val result = StringBuilder()
    val paragraphMatcher = paragraphSeparatorPattern.matcher(textWithTabPlaceholders)
    val textLength = textWithTabPlaceholders.length
    var location = 0

    while (paragraphMatcher.find()) {
      val paragraph = textWithTabPlaceholders.substring(location, paragraphMatcher.start())
      result.append(wrapParagraph(paragraph))
      result.append(paragraphMatcher.group())
      location = paragraphMatcher.end()
    }

    if (location < textLength) {
      result.append(wrapParagraph(textWithTabPlaceholders.substring(location, textLength)))
      if (textWithTabPlaceholders.endsWith(lineSeparator)) {
        result.append(lineSeparator)
      }
    }

    return result.toString().replace(expandedTabPlaceholder, "\t")
  }

  /**
   * Wrap a single paragraph of text.
   */
  private fun wrapParagraph(paragraph: String): String {
    val resultBuilder = StringBuilder()
    val emptyCommentMatcher = emptyCommentLinePattern.matcher(paragraph)
    val paragraphLength = paragraph.length
    var location = 0

    while (emptyCommentMatcher.find()) {
      val match = emptyCommentMatcher.group()
      if (match.isEmpty()) continue

      val otherText = paragraph.substring(location, emptyCommentMatcher.start())
      val wrappedLines = breakToLinesOfChosenWidth(otherText)

      if (paragraph.startsWith(match)) {
        resultBuilder.append(match + lineSeparator)
      }

      for (wrappedLine in wrappedLines) {
        resultBuilder.append(wrappedLine + lineSeparator)
      }

      if (paragraph.endsWith(match)) {
        resultBuilder.append(match)
      }

      location = emptyCommentMatcher.end()
    }

    if (location < paragraphLength) {
      val otherText = paragraph.substring(location, paragraphLength)
      val wrappedLines = breakToLinesOfChosenWidth(otherText)
      for (wrappedLine in wrappedLines) {
        resultBuilder.append(wrappedLine + lineSeparator)
      }
    }

    var result = resultBuilder.toString()
    if (result.endsWith(lineSeparator)) {
      result = result.substring(0, result.length - 1)
    }

    return result
  }

  /**
   * Check if a line has code before an inline comment marker.
   */
  private fun isCodeWithInlineComment(line: String): Boolean {
    val codeInlineCommentRegex = "//+!?|#+".toRegex()
    val inlineMatch = codeInlineCommentRegex.find(line) ?: return false
    val beforeComment = line.substring(0, inlineMatch.range.first)
    return !beforeComment.matches("^\\s*|$tabPlaceholder*$".toRegex())
  }

  /**
   * Break text into lines of the configured width.
   */
  private fun breakToLinesOfChosenWidth(text: String): MutableList<String> {
    val firstLine = text.split("[\\r\\n]+".toRegex()).firstOrNull() ?: text

    // Don't wrap lines with code before inline comments
    if (isCodeWithInlineComment(firstLine)) {
      return text.split("[\\r\\n]+".toRegex())
        .dropLastWhile(String::isEmpty)
        .toMutableList()
    }

    val firstLineIndent = splitOnIndent(text).indent
    val firstLineIsCommentOpener = firstLineIndent.matches("\\s*/\\*+\\s*".toRegex())

    val effectiveWidth = width - firstLineIndent.length
    val unwrappedText = unwrap(text)

    val lines = wrapGreedy(unwrappedText, effectiveWidth)
      .split(lineSeparator.toRegex())
      .dropLastWhile(String::isEmpty)
      .toTypedArray()

    val result = mutableListOf<String>()
    var whitespaceBeforeOpener = ""

    if (firstLineIsCommentOpener) {
      val whitespaceMatcher = Pattern.compile("^\\s*").matcher(firstLineIndent)
      if (whitespaceMatcher.find()) {
        whitespaceBeforeOpener = whitespaceMatcher.group()
      }
    }

    for (i in lines.indices) {
      val line = lines[i]
      var lineIndent = firstLineIndent

      if (i > 0 && firstLineIsCommentOpener) {
        lineIndent = "$whitespaceBeforeOpener * "
      }

      result.add(lineIndent + line)
    }

    return result
  }

  /**
   * Convert a hard-wrapped paragraph to one line.
   */
  private fun unwrap(text: String): String {
    if (text.isEmpty()) return text

    val lines = text.split("[\\r\\n]+".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
    val result = StringBuilder()
    var lastLineWasCarriageReturn = false

    for (i in lines.indices) {
      val line = lines[i]
      val unindentedLine = splitOnIndent(line).rest.trim()

      if (line.isEmpty()) {
        lastLineWasCarriageReturn = true
        continue
      }

      if (lastLineWasCarriageReturn || lines.size == 1 || i == 0) {
        result.append(unindentedLine)
      } else {
        result.append(" ").append(unindentedLine)
      }

      lastLineWasCarriageReturn = false
    }

    return result.toString()
  }

  /**
   * Split text on indent, including comment characters.
   */
  fun splitOnIndent(text: String): LineData {
    val indentMatcher = indentPattern.matcher(text)

    if (indentMatcher.find()) {
      var indent = indentMatcher.group()
      indent = indent.replace("[\\r\\n]+".toRegex(), "")
      val rest = text.substring(indentMatcher.end()).trim()
      return LineData(indent, rest)
    }

    return LineData("", text)
  }

  /**
   * Greedy word wrapping algorithm.
   */
  private fun wrapGreedy(text: String, width: Int): String {
    if (width <= 0) return text

    val words = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (words.isEmpty()) return ""

    val result = StringBuilder()
    var lineLength = 0

    for (word in words) {
      if (lineLength == 0) {
        result.append(word)
        lineLength = word.length
      } else if (lineLength + 1 + word.length <= width) {
        result.append(" ").append(word)
        lineLength += 1 + word.length
      } else {
        result.append(lineSeparator).append(word)
        lineLength = word.length
      }
    }

    return result.toString()
  }
}
