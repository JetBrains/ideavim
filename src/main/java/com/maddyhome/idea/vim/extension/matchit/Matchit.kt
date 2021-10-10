/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.extension.matchit

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState.Companion.getInstance
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.getTopLevelEditor
import com.maddyhome.idea.vim.helper.vimForEachCaret
import java.util.*
import java.util.regex.Pattern

/**
 * Port of matchit.vim (https://github.com/chrisbra/matchit)
 * Languages currently supported:
 *   Ruby, Embedded Ruby, XML, and HTML (including HTML in JavaScript/TypeScript, JSX/TSX, and JSPs)
 *
 * @author Martin Yzeiri (@myzeiri)
 */
class Matchit : VimExtension {

  override fun getName(): String = "matchit"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, parseKeys("<Plug>(MatchitMotion)"), owner, MatchitHandler(false), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, parseKeys("<Plug>(MatchitMotion)"), owner, MatchitHandler(false), false)
    putKeyMappingIfMissing(MappingMode.NXO, parseKeys("%"), owner, parseKeys("<Plug>(MatchitMotion)"), true)

    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, parseKeys("<Plug>(ReverseMatchitMotion)"), owner, MatchitHandler(true), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, parseKeys("<Plug>(ReverseMatchitMotion)"), owner, MatchitHandler(true), false)
    putKeyMappingIfMissing(MappingMode.NXO, parseKeys("g%"), owner, parseKeys("<Plug>(ReverseMatchitMotion)"), true)
  }

  private class MatchitAction : MotionActionHandler.ForEachCaret() {
    var reverse = false
    var isInOpPending = false

    override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

    override fun getOffset(
      editor: Editor,
      caret: Caret,
      context: DataContext,
      count: Int,
      rawCount: Int,
      argument: Argument?,
    ): Motion {
      return getMatchitOffset(editor, caret, rawCount, isInOpPending, reverse).toMotionOrError()
    }

    override fun process(cmd: Command) {
      motionType = if (cmd.rawCount != 0) MotionType.LINE_WISE else MotionType.INCLUSIVE
    }

    override var motionType: MotionType = MotionType.INCLUSIVE
  }

  private class MatchitHandler(private val reverse: Boolean) : VimExtensionHandler {

    override fun execute(editor: Editor, context: DataContext) {
      val commandState = getInstance(editor)
      val count = commandState.commandBuilder.count

      // Reset the command count so it doesn't transfer onto subsequent commands.
      editor.getTopLevelEditor().commandState.commandBuilder.resetCount()

      // Normally we want to jump to the start of the matching pair. But when moving forward in operator
      // pending mode, we want to include the entire match. isInOpPending makes that distinction.
      val isInOpPending = commandState.isOperatorPending

      if (isInOpPending) {
        val matchitAction = MatchitAction()
        matchitAction.reverse = reverse
        matchitAction.isInOpPending = true

        commandState.commandBuilder.completeCommandPart(
          Argument(
            Command(
              count,
              matchitAction, Command.Type.MOTION, EnumSet.noneOf(CommandFlags::class.java)
            )
          )
        )
      } else {
        editor.vimForEachCaret { caret ->
          VimPlugin.getMark().saveJumpLocation(editor)
          MotionGroup.moveCaret(editor, caret, getMatchitOffset(editor, caret, count, isInOpPending, reverse))
        }
      }
    }
  }
}

/**
 * To find a matching pair, we need patterns that describe what the opening and closing pairs look like.
 *
 * We pass around strings instead of compiled Java Patterns since a pattern may require back references to be added
 * before the search can proceed. E.g. for HTML, we use a general pattern to check if the cursor is inside a tag. The
 * pattern captures the tag's name as a back reference so we can later search for something more specific like "</div>"
 */
private typealias MatchitSearchPair = Pair<String, String>

/**
 * A language can have many patterns that describe its matching pairs. We divide the patterns into two maps, one for
 * the opening pairs (e.g. "if" or "def" in Ruby) and one for the closing pairs.
 * For each pair, we take the pattern that describes the text the cursor can be on and map it to the MatchitSearchPair
 * we need to find the match.
 *
 * Simple example: if we wanted the cursor to jump from an opening angle bracket to a closing angle bracket even if the
 * cursor was on whitespace, then we would add "\\s*<" -> MatchitSearchPair("<", ">") to the openingPatterns map.
 */
private data class MatchitPatternsTable(
  val openingPatterns: Map<String, MatchitSearchPair>,
  val closingPatterns: Map<String, MatchitSearchPair>
)

/**
 * All the information we need to find a match.
 */
private data class MatchitSearchParams(
  val caretOffset: Int,
  val endOfCurrentPattern: Int,
  val targetOpeningPattern: String,
  val targetClosingPattern: String,

  // If the cursor is not in a comment, then we want to ignore any matches found in comments.
  // But if we are in comment, then we only want to match on things in comments. The same goes for quotes.
  val skipComments: Boolean,
  val skipStrings: Boolean
)

/**
 * Patterns for the supported file types are stored in this object.
 */
private object MatchitPatterns {

  fun getPatternsForFile(virtualFile: VirtualFile?, reverse: Boolean): MatchitPatternsTable? {
    val fileExtension = virtualFile?.extension
    val fileTypeName = virtualFile?.fileType?.name

    // Ruby file types are only detected if the user is running RubyMine or the Ruby plugin.
    // Checking the extension is a simple fallback that also lets us unit test the Ruby patterns.
    return if (fileTypeName == "Ruby" || fileExtension == "rb") {
      if (reverse) this.reverseRubyPatternsTable else this.rubyPatternsTable
    } else if (fileTypeName == "RHTML" || fileExtension == "erb") {
      if (reverse) this.reverseRubyAndHtmlPatternsTable else this.rubyAndHtmlPatternsTable
    } else if (fileTypeName in htmlLikeFileTypes) {
      this.htmlPatternsTable
    } else {
      return null
    }
  }

  // Files that can contain HTML or HTML-like content.
  // These are just the file types that have HTML Matchit support enabled by default in their Vim ftplugin files.
  private val htmlLikeFileTypes = arrayOf("HTML", "XML", "XHTML", "JSP", "JavaScript", "JSX Harmony", "TypeScript", "TypeScript JSX")

  private val htmlPatternsTable = createHtmlPatternsTable()
  private val rubyPatternsTable = createRubyPatternsTable()
  private val reverseRubyPatternsTable = createRubyPatternsTable(true)
  private val rubyAndHtmlPatternsTable = MatchitPatternsTable(
    rubyPatternsTable.openingPatterns + htmlPatternsTable.openingPatterns,
    rubyPatternsTable.closingPatterns + htmlPatternsTable.closingPatterns
  )
  private val reverseRubyAndHtmlPatternsTable = MatchitPatternsTable(
    reverseRubyPatternsTable.openingPatterns + htmlPatternsTable.openingPatterns,
    reverseRubyPatternsTable.closingPatterns + htmlPatternsTable.closingPatterns
  )

  private fun createHtmlPatternsTable(): MatchitPatternsTable {
    // A tag name may contain any characters except slashes, whitespace, and angle brackets.
    // We surround the tag name in a capture group so that we can use it when searching for a match later.
    val tagNamePattern = "([^/\\s><]+)"

    // An opening tag consists of "<" followed by a tag name and optionally some additional text after whitespace.
    // Note the assertion on "<" to not match on that character. If the cursor is on an angle bracket, then we want to
    // match angle brackets, not HTML tags.
    val openingTagPattern = String.format("(?<=<)%s(?:\\s[^<>]*(\".*\")?)?", tagNamePattern)

    // A closing tag consists of a "<" followed by a slash, the tag name, and a ">".
    val closingTagPattern = String.format("(?<=<)/%s(?=>)", tagNamePattern)

    // The tag name is left as %s so we can substitute the back reference we captured.
    val htmlSearchPair = Pair("(?<=<)%s(?:\\s[^<>]*(\".*\")?)?", "(?<=<)/%s>")

    // Along with being on a "<", we also want to jump to a matching ">" if the cursor is anywhere before a "<" on the
    // same line. We exclude the other bracket types since the closest bracket to the cursor should take precedence.
    val openingAngleBracketPattern = "[^<>(){}\\[\\]]*<"
    val closingAngleBracketPattern = ">"
    val angleBracketSearchPair = Pair("<", ">")

    return MatchitPatternsTable(
      mapOf(
        openingTagPattern to htmlSearchPair,
        openingAngleBracketPattern to angleBracketSearchPair
      ),
      mapOf(
        closingTagPattern to htmlSearchPair,
        closingAngleBracketPattern to angleBracketSearchPair
      )
    )
  }

  private fun createRubyPatternsTable(reverse: Boolean = false): MatchitPatternsTable {
    // Original patterns are defined here: https://github.com/vim/vim/blob/master/runtime/ftplugin/ruby.vim
    // We use non-capturing groups (?:) since we don't need any back refs. The \\b marker takes care of word boundaries.
    // On the class keyword we exclude an equal sign from appearing afterwards since it clashes with the HTML attribute.
    val openingKeywords = "(?:\\b(?:do|if|unless|case|def|for|while|until|module|begin)\\b)|(?:\\bclass\\b[^=])"

    val endKeyword = "\\bend\\b"

    // A "middle" keyword is one that can act as both an opening or a closing pair. E.g. "elsif" can appear any number
    // of times between the opening "if" and the closing "end".
    val middleKeywords = "(?:\\b(?:else|elsif|break|when|rescue|ensure|redo|next|retry)\\b)"
    val openingAndMiddleKeywords = "$openingKeywords|$middleKeywords"
    val middleAndClosingKeywords = "$middleKeywords|(?:$endKeyword)"

    // We want the cursor to jump to an opening even if it's on whitespace before an "end".
    val prefixedEndKeyword = "\\s*$endKeyword"

    // For the openings, we want to jump if the cursor is on any non-bracket character before the keyword.
    val prefix = "[^(){}<>\\[\\]]*?"
    val prefixedOpeningKeywords = "$prefix(?:$openingKeywords)"
    val prefixedMiddleKeywords = "$prefix(?:$middleKeywords)"
    val prefixedOpeningAndMiddleKeywords = "$prefix(?:$openingAndMiddleKeywords)"
    val prefixedMiddleAndClosingKeywords = "(?:$prefix(?:$middleKeywords))|$prefixedEndKeyword"

    val blockCommentStart = "=begin\\b"
    val blockCommentEnd = "=end\\b"
    // The cursor shouldn't jump to the equal sign on a block comment, so we exclude it with a look-behind assertion.
    val blockCommentSearchPair = Pair("(?<==)begin\\b", "(?<==)end\\b")

    if (reverse) {
      // Supporting the g% motion is just a matter of rearranging the patterns table.
      // This particular arrangement relies on our checking if the cursor is on a closing pattern first.
      return MatchitPatternsTable(
        mapOf(
          blockCommentStart to blockCommentSearchPair,
          prefixedOpeningKeywords to Pair(openingKeywords, endKeyword),
          prefixedMiddleKeywords to Pair(openingAndMiddleKeywords, middleKeywords)
        ),
        mapOf(
          blockCommentEnd to blockCommentSearchPair,
          prefixedMiddleAndClosingKeywords to Pair(openingAndMiddleKeywords, middleAndClosingKeywords)
        )
      )
    } else {
      // Patterns for the regular % motion.
      return MatchitPatternsTable(
        mapOf(
          blockCommentStart to blockCommentSearchPair,
          prefixedOpeningAndMiddleKeywords to Pair(openingAndMiddleKeywords, middleAndClosingKeywords)
        ),
        mapOf(
          blockCommentEnd to blockCommentSearchPair,
          prefixedEndKeyword to Pair(openingKeywords, endKeyword)
        )
      )
    }
  }
}

/*
 * Helper search functions.
 */

private fun getMatchitOffset(editor: Editor, caret: Caret, count: Int, isInOpPending: Boolean, reverse: Boolean): Int {
  val defaultPairs = arrayOf('(', ')', '[', ']', '{', '}')
  val virtualFile = EditorHelper.getVirtualFile(editor)
  var caretOffset = caret.offset

  // Handle the case where visual mode has brought the cursor past the end of the line.
  val lineEndOffset = EditorHelper.getLineEndOffset(editor, caret.logicalPosition.line, true)
  if (caretOffset > 0 && caretOffset == lineEndOffset) {
    caretOffset--
  }

  val currentChar = editor.document.charsSequence[caretOffset]
  var motion = -1

  if (count > 0) {
    // Matchit doesn't affect the percent motion, so we fall back to the default behavior.
    motion = VimPlugin.getMotion().moveCaretToLinePercent(editor, caret, count)
  } else {
    // Check the simplest case first.
    if (defaultPairs.contains(currentChar)) {
      motion = VimPlugin.getMotion().moveCaretToMatchingPair(editor, caret)
    } else {
      val patternsTable = MatchitPatterns.getPatternsForFile(virtualFile, reverse)
      if (patternsTable != null) {
        motion = findMatchingPair(editor, caretOffset, isInOpPending, patternsTable)
      }

      if (motion < 0) {
        // Use default motion if the file type isn't supported or we didn't find any extended pairs.
        motion = VimPlugin.getMotion().moveCaretToMatchingPair(editor, caret)
      }
    }
  }

  if (motion >= 0) {
    motion = EditorHelper.normalizeOffset(editor, motion, false)
  }

  return motion
}

private fun findMatchingPair(editor: Editor, caretOffset: Int, isInOpPending: Boolean, patternsTable: MatchitPatternsTable): Int {
  val openingPatternsTable = patternsTable.openingPatterns
  val closingPatternsTable = patternsTable.closingPatterns

  // Check if the cursor is on a closing pair.
  val offset: Int
  var searchParams = getMatchitSearchParams(editor, caretOffset, closingPatternsTable)
  if (searchParams != null) {
    offset = findOpeningPair(editor, isInOpPending, searchParams)
    // If the user is on a valid pattern, but we didn't find a matching pair, then the cursor shouldn't move.
    // We return the current caret offset to reflect that case, as opposed to -1 which means the cursor isn't on a
    // valid pattern at all.
    return if (offset < 0) caretOffset else offset
  }

  // Check if the cursor is on an opening pair.
  searchParams = getMatchitSearchParams(editor, caretOffset, openingPatternsTable)
  if (searchParams != null) {
    offset = findClosingPair(editor, isInOpPending, searchParams)
    return if (offset < 0) caretOffset else offset
  }

  return -1
}

/**
 * Checks if the cursor is on valid a pattern and returns the necessary search params if it is.
 * Returns null if the cursor is not on a pattern.
 */
private fun getMatchitSearchParams(editor: Editor, caretOffset: Int, patternsTable: Map<String, Pair<String, String>>): MatchitSearchParams? {
  // For better performance, we limit our search to the current line. This way we don't have to scan the entire file
  // to determine if we're on a pattern or not. The original plugin behaves the same way.
  val currentLineStart = EditorHelper.getLineStartForOffset(editor, caretOffset)
  val currentLineEnd = EditorHelper.getLineEndForOffset(editor, caretOffset)
  val currentLineChars = editor.document.charsSequence.subSequence(currentLineStart, currentLineEnd)
  val currentPsiElement = PsiHelper.getFile(editor)!!.findElementAt(caretOffset)

  val targetOpeningPattern: String
  val targetClosingPattern: String

  for ((pattern, searchPair) in patternsTable) {
    val (searchOffset, backRef) = parsePatternAtOffset(currentLineChars, caretOffset - currentLineStart, pattern)
    if (searchOffset >= 0) {
      // HTML attributes are a special case where the cursor is inside of quotes, but we want to jump as if we were
      // anywhere else inside the opening tag.
      val skipComments = !isComment(currentPsiElement)
      val skipQuotes = !isQuoted(currentPsiElement) || isHtmlAttribute(currentPsiElement)

      // Substitute any captured back references to the search patterns, if necessary.
      if (backRef != "") {
        targetOpeningPattern = String.format(searchPair.first, backRef)
        targetClosingPattern = String.format(searchPair.second, backRef)
      } else {
        targetOpeningPattern = searchPair.first
        targetClosingPattern = searchPair.second
      }

      val endOfCurrentPattern = currentLineStart + searchOffset
      return MatchitSearchParams(caretOffset, endOfCurrentPattern, targetOpeningPattern, targetClosingPattern, skipComments, skipQuotes)
    }
  }

  return null
}

private fun findClosingPair(editor: Editor, isInOpPending: Boolean, searchParams: MatchitSearchParams): Int {
  val (caretOffset, searchStartOffset, openingPattern, closingPattern, skipComments, skipStrings) = searchParams
  val chars = editor.document.charsSequence
  val searchSpace = chars.subSequence(searchStartOffset, chars.length)

  val compiledClosingPattern = Pattern.compile(closingPattern)
  val compiledSearchPattern = Pattern.compile(String.format("(?<opening>%s)|(?<closing>%s)", openingPattern, closingPattern))
  val matcher = compiledSearchPattern.matcher(searchSpace)

  // We're looking for the first closing pair that isn't already matched by an opening.
  // As we find opening patterns, we push their offsets to this stack and pop whenever we find a closing pattern,
  // effectively crossing off that item from our search. We have to track both the start and end of the pattern since
  // the motion depends on whether we're in op pending mode or not.
  var closingPairRange: Pair<Int, Int>? = null
  val unmatchedOpeningPairs: Deque<Pair<Int, Int>> = ArrayDeque()
  while (matcher.find()) {
    val matchStartOffset = searchStartOffset + matcher.start()
    val matchEndOffset = searchStartOffset + matcher.end()

    if (matchShouldBeSkipped(editor, matchStartOffset, skipComments, skipStrings)) {
      continue
    }

    val foundOpeningPattern = matcher.group("opening") != null
    // Middle patterns e.g. "elsif" can appear any number of times between a strict opening and a strict closing.
    val foundMiddlePattern = foundOpeningPattern && compiledClosingPattern.matcher(matcher.group("opening")).matches()

    if (foundMiddlePattern) {
      if (!unmatchedOpeningPairs.isEmpty()) {
        unmatchedOpeningPairs.pop()
        unmatchedOpeningPairs.push(Pair(matchStartOffset, matchEndOffset))
      } else {
        closingPairRange = Pair(matchStartOffset, matchEndOffset)
        break
      }
    } else if (foundOpeningPattern) {
      unmatchedOpeningPairs.push(Pair(matchStartOffset, matchEndOffset))
    } else {
      // Found a closing pattern
      if (!unmatchedOpeningPairs.isEmpty()) {
        unmatchedOpeningPairs.pop()
      } else {
        closingPairRange = Pair(matchStartOffset, matchEndOffset)
        break
      }
    }
  }

  if (closingPairRange != null) {
    return if (isInOpPending && caretOffset < closingPairRange.first) {
      closingPairRange.second - 1 // Jump to the last char of the match
    } else {
      closingPairRange.first
    }
  }

  return -1
}

private fun findOpeningPair(editor: Editor, isInOpPending: Boolean, searchParams: MatchitSearchParams): Int {
  val (caretOffset, _, openingPattern, closingPattern, skipComments, skipStrings) = searchParams
  val chars = editor.document.charsSequence
  val searchSpace = chars.subSequence(0, caretOffset)

  val compiledClosingPattern = Pattern.compile(closingPattern)
  val compiledSearchPattern = Pattern.compile(String.format("(?<opening>%s)|(?<closing>%s)", openingPattern, closingPattern))
  val matcher = compiledSearchPattern.matcher(searchSpace)

  val unmatchedOpeningPairs: Deque<Pair<Int, Int>> = ArrayDeque()
  while (matcher.find()) {
    val matchStartOffset = matcher.start()
    val matchEndOffset = matcher.end()

    if (matchShouldBeSkipped(editor, matchStartOffset, skipComments, skipStrings)) {
      continue
    }

    val foundOpeningPattern = matcher.group("opening") != null
    val foundMiddlePattern = foundOpeningPattern && compiledClosingPattern.matcher(matcher.group("opening")).matches()

    if (foundMiddlePattern) {
      if (!unmatchedOpeningPairs.isEmpty()) {
        unmatchedOpeningPairs.pop()
        unmatchedOpeningPairs.push(Pair(matchStartOffset, matchEndOffset))
      } else {
        unmatchedOpeningPairs.push(Pair(matchStartOffset, matchEndOffset))
      }
    } else if (foundOpeningPattern) {
      unmatchedOpeningPairs.push(Pair(matchStartOffset, matchEndOffset))
    } else if (!unmatchedOpeningPairs.isEmpty()) {
      // Found a closing pattern. We check the stack isn't empty to handle malformed code.
      unmatchedOpeningPairs.pop()
    }
  }

  if (!unmatchedOpeningPairs.isEmpty()) {
    val openingPairRange = unmatchedOpeningPairs.pop()

    return if (isInOpPending && caretOffset < openingPairRange.first) {
      openingPairRange.second - 1
    } else {
      openingPairRange.first
    }
  }

  return -1
}

/**
 * If the char sequence at the given offset matches the pattern, this will return the final offset of the match
 * as well as any back references that were captured.
 */
private fun parsePatternAtOffset(chars: CharSequence, offset: Int, pattern: String): Pair<Int, String> {
  val matcher = Pattern.compile(pattern).matcher(chars)

  while (matcher.find()) {
    val matchStart = matcher.start()
    val matchEnd = matcher.end()

    if (offset in matchStart until matchEnd) {
      if (matcher.groupCount() > 0) {
        return Pair(matchEnd, matcher.group(1))
      }
      return Pair(matchEnd, "")
    } else if (offset < matchStart) {
      return Pair(-1, "")
    }
  }
  return Pair(-1, "")
}

private fun matchShouldBeSkipped(editor: Editor, offset: Int, skipComments: Boolean, skipStrings: Boolean): Boolean {
  val psiFile = PsiHelper.getFile(editor)
  val psiElement = psiFile!!.findElementAt(offset)

  // TODO: as we add support for more languages, we should store the ignored keywords for each language in its own
  //  data structure. The original plugin stores that information in strings called match_skip.
  if (isSkippedRubyKeyword(psiElement)) {
    return true
  }

  val insideComment = isComment(psiElement)
  val insideQuotes = isQuoted(psiElement)
  return (skipComments && insideComment) || (!skipComments && !insideComment) ||
    (skipStrings && insideQuotes) || (!skipStrings && !insideQuotes)
}

private fun isSkippedRubyKeyword(psiElement: PsiElement?): Boolean {
  // In Ruby code, we want to ignore anything inside of a regular expression like "/ class /" and identifiers like
  // "Foo.class". Matchit also ignores any "do" keywords that follow a loop or an if condition, as well as any inline
  // "if" and "unless" expressions (a.k.a conditional modifiers).
  val elementType = psiElement?.node?.elementType?.debugName

  return elementType == "do_cond" || elementType == "if modifier" || elementType == "unless modifier" ||
    elementType == "regexp content" || elementType == "identifier"
}

private fun isComment(psiElement: PsiElement?): Boolean {
  // This works for languages other than Java.
  return PsiTreeUtil.getParentOfType(psiElement, PsiComment::class.java, false) != null
}

private fun isQuoted(psiElement: PsiElement?): Boolean {
  val elementType = psiElement?.elementType?.debugName
  return elementType == "STRING_LITERAL" || elementType == "XML_ATTRIBUTE_VALUE_TOKEN" ||
    elementType == "string content" // Ruby specific.
}

private fun isHtmlAttribute(psiElement: PsiElement?): Boolean {
  val elementType = psiElement?.elementType?.debugName
  return elementType == "XML_ATTRIBUTE_VALUE_TOKEN"
}
