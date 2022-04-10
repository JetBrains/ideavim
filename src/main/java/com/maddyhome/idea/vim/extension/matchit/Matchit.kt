/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.MappingMode
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
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import java.util.*
import java.util.regex.Pattern

/**
 * Port of matchit.vim (https://github.com/chrisbra/matchit)
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
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Motion {
      return getMatchitOffset(editor.ij, caret.ij, operatorArguments.count0, isInOpPending, reverse).toMotionOrError()
    }

    override fun process(cmd: Command) {
      motionType = if (cmd.rawCount != 0) MotionType.LINE_WISE else MotionType.INCLUSIVE
    }

    override var motionType: MotionType = MotionType.INCLUSIVE
  }

  private class MatchitHandler(private val reverse: Boolean) : VimExtensionHandler {

    override fun execute(editor: Editor, context: DataContext) {
      val commandState = editor.vim.commandState
      val count = commandState.commandBuilder.count

      // Reset the command count so it doesn't transfer onto subsequent commands.
      editor.getTopLevelEditor().vim.commandState.commandBuilder.resetCount()

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
          VimPlugin.getMark().saveJumpLocation(editor.vim)
          MotionGroup.moveCaret(editor, caret, getMatchitOffset(editor, caret, count, isInOpPending, reverse))
        }
      }
    }
  }
}

/**
 * To find a match, we need two patterns that describe what the opening and closing pairs look like, and we need a
 * pattern to describe the valid starting points for the jump. A PatternsTable maps valid starting points to the pair of
 * patterns needed for the search.
 *
 * We pass around strings instead of compiled Java Patterns since a pattern may require back references to be added
 * before the search can proceed. E.g. for HTML, we use a general pattern to check if the cursor is inside a tag. The
 * pattern captures the tag's name as a back reference so we can later search for something more specific like "</div>"
 */
private typealias PatternsTable = Map<String, Pair<String, String>>

/**
 * A language can have many different matching pairs. We divide the patterns into four PatternsTables. `openings` and
 * `closings` handle the % motion while `reversedOpenings` and `reversedClosings` handle the g% motion.
 */
private data class LanguagePatterns(
  val openings: PatternsTable,
  val closings: PatternsTable,
  val reversedOpenings: PatternsTable,
  val reversedClosings: PatternsTable,
) {
  // Helper constructor for languages that don't need reversed patterns.
  constructor(openings: PatternsTable, closings: PatternsTable) : this(openings, closings, openings, closings)

  operator fun plus(newLanguagePatterns: LanguagePatterns): LanguagePatterns {
    return LanguagePatterns(
      this.openings + newLanguagePatterns.openings,
      this.closings + newLanguagePatterns.closings,
      this.reversedOpenings + newLanguagePatterns.reversedOpenings,
      this.reversedClosings + newLanguagePatterns.reversedClosings
    )
  }

  // Helper constructors for the most common language patterns. More complicated structures, i.e. those that require
  // back references, should be built with the default constructor.
  companion object {
    operator fun invoke(openingPattern: String, closingPattern: String): LanguagePatterns {
      val openingPatternsTable = mapOf(openingPattern to Pair(openingPattern, closingPattern))
      val closingPatternsTable = mapOf(closingPattern to Pair(openingPattern, closingPattern))
      return LanguagePatterns(openingPatternsTable, closingPatternsTable)
    }

    operator fun invoke(openingPattern: String, middlePattern: String, closingPattern: String): LanguagePatterns {
      val openingAndMiddlePatterns = "(?:$openingPattern)|(?:$middlePattern)"
      val middleAndClosingPatterns = "(?:$middlePattern)|(?:$closingPattern)"

      val openings = mapOf(openingAndMiddlePatterns to Pair(openingAndMiddlePatterns, middleAndClosingPatterns))
      val closings = mapOf(closingPattern to Pair(openingPattern, closingPattern))

      // Supporting the g% motion is just a matter of rearranging the patterns table.
      // This particular arrangement relies on our checking if the cursor is on a closing pattern first.
      val reversedOpenings = mapOf(
        openingPattern to Pair(openingPattern, closingPattern),
        middlePattern to Pair(openingAndMiddlePatterns, middlePattern)
      )
      val reversedClosings = mapOf(middleAndClosingPatterns to Pair(openingAndMiddlePatterns, middleAndClosingPatterns))

      return LanguagePatterns(openings, closings, reversedOpenings, reversedClosings)
    }
  }
}

/**
 * All the information we need to find a match.
 */
private data class MatchitSearchParams(
  val initialPatternStart: Int, // Starting offset of the pattern containing the cursor.
  val initialPatternEnd: Int,
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
private object FileTypePatterns {

  fun getMatchitPatterns(virtualFile: VirtualFile?): LanguagePatterns? {
    // fileType is only populated for files supported by the user's IDE + language plugins.
    // Checking the file's name or extension is a simple fallback which also makes unit testing easier.
    val fileTypeName = virtualFile?.fileType?.name
    val fileName = virtualFile?.nameWithoutExtension
    val fileExtension = virtualFile?.extension

    return if (fileTypeName in htmlLikeFileTypes) {
      this.htmlPatterns
    } else if (fileTypeName == "Ruby" || fileExtension == "rb") {
      this.rubyPatterns
    } else if (fileTypeName == "RHTML" || fileExtension == "erb") {
      this.rubyAndHtmlPatterns
    } else if (fileTypeName == "C++" || fileTypeName == "C#" || fileTypeName == "ObjectiveC" || fileExtension == "c") {
      // "C++" also covers plain C.
      this.cPatterns
    } else if (fileTypeName == "Makefile" || fileName == "Makefile") {
      this.gnuMakePatterns
    } else if (fileTypeName == "CMakeLists.txt" || fileName == "CMakeLists") {
      this.cMakePatterns
    } else {
      return null
    }
  }

  private val htmlLikeFileTypes = setOf(
    "HTML", "XML", "XHTML", "JSP", "JavaScript", "JSX Harmony", "TypeScript", "TypeScript JSX", "Vue.js", "Handlebars/Mustache"
  )

  private val htmlPatterns = createHtmlPatterns()
  private val rubyPatterns = createRubyPatterns()
  private val rubyAndHtmlPatterns = rubyPatterns + htmlPatterns
  private val cPatterns = createCPatterns()
  private val gnuMakePatterns = createGnuMakePatterns()
  private val cMakePatterns = createCMakePatterns()

  private fun createHtmlPatterns(): LanguagePatterns {
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

    return (
      LanguagePatterns("<", ">") +
        LanguagePatterns(mapOf(openingTagPattern to htmlSearchPair), mapOf(closingTagPattern to htmlSearchPair))
      )
  }

  private fun createRubyPatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/ruby.vim
    // We use non-capturing groups (?:) since we don't need any back refs. The \\b marker takes care of word boundaries.
    // On the class keyword we exclude an equal sign from appearing afterwards since it clashes with the HTML attribute.
    val openingKeywords = "(?:\\b(?:do|if|unless|case|def|for|while|until|module|begin)\\b)|(?:\\bclass\\b[^=])"
    val endKeyword = "\\bend\\b"

    // A "middle" keyword is one that can act as both an opening or a closing pair. E.g. "elsif" can appear any number
    // of times between the opening "if" and the closing "end".
    val middleKeywords = "(?:\\b(?:else|elsif|break|when|rescue|ensure|redo|next|retry)\\b)"

    // The cursor shouldn't jump to the equal sign on a block comment, so we exclude it with a look-behind assertion.
    val blockCommentStart = "(?<==)begin\\b"
    val blockCommentEnd = "(?<==)end\\b"

    return (
      LanguagePatterns(blockCommentStart, blockCommentEnd) +
        LanguagePatterns(openingKeywords, middleKeywords, endKeyword)
      )
  }

  private fun createCPatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/c.vim
    return LanguagePatterns("#\\s*if(?:def|ndef)?\\b", "#\\s*(?:elif|else)\\b", "#\\s*endif\\b")
  }

  private fun createGnuMakePatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/make.vim
    return (
      LanguagePatterns("\\bdefine\\b", "\\bendef\\b") +
        LanguagePatterns("(?<!else )ifn?(?:eq|def)\\b", "\\belse(?:\\s+ifn?(?:eq|def))?\\b", "\\bendif\\b")
      )
  }

  private fun createCMakePatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/cmake.vim
    return (
      LanguagePatterns("\\bif\\b", "\\belse(?:if)?\\b", "\\bendif\\b") +
        LanguagePatterns("\\b(?:foreach)|(?:while)\\b", "\\bbreak\\b", "\\b(?:endforeach)|(?:endwhile)\\b") +
        LanguagePatterns("\\bmacro\\b", "\\bendmacro\\b") +
        LanguagePatterns("\\bfunction\\b", "\\bendfunction\\b")
      )
  }
}

/*
 * Helper search functions.
 */

private val DEFAULT_PAIRS = setOf('(', ')', '[', ']', '{', '}')

private fun getMatchitOffset(editor: Editor, caret: Caret, count: Int, isInOpPending: Boolean, reverse: Boolean): Int {
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
    motion = VimPlugin.getMotion().moveCaretToLinePercent(editor.vim, caret.vim, count)
  } else {
    // Check the simplest case first.
    if (DEFAULT_PAIRS.contains(currentChar)) {
      motion = VimPlugin.getMotion().moveCaretToMatchingPair(editor.vim, caret.vim)
    } else {
      val matchitPatterns = FileTypePatterns.getMatchitPatterns(virtualFile)
      if (matchitPatterns != null) {
        motion = if (reverse) {
          findMatchingPair(editor, caretOffset, isInOpPending, matchitPatterns.reversedOpenings, matchitPatterns.reversedClosings)
        } else {
          findMatchingPair(editor, caretOffset, isInOpPending, matchitPatterns.openings, matchitPatterns.closings)
        }
      }

      if (motion < 0) {
        // Use default motion if the file type isn't supported or we didn't find any extended pairs.
        motion = VimPlugin.getMotion().moveCaretToMatchingPair(editor.vim, caret.vim)
      }
    }
  }

  if (motion >= 0) {
    motion = EditorHelper.normalizeOffset(editor, motion, false)
  }

  return motion
}

private fun findMatchingPair(
  editor: Editor,
  caretOffset: Int,
  isInOpPending: Boolean,
  openings: PatternsTable,
  closings: PatternsTable
): Int {
  // For better performance, we limit our search to the current line. This way we don't have to scan the entire file
  // to determine if we're on a pattern or not. The original plugin behaves the same way.
  val currentLineStart = EditorHelper.getLineStartForOffset(editor, caretOffset)
  val currentLineEnd = EditorHelper.getLineEndForOffset(editor, caretOffset)
  val currentLineChars = editor.document.charsSequence.subSequence(currentLineStart, currentLineEnd)
  val offset = caretOffset - currentLineStart

  var closestSearchPair: Pair<String, String>? = null
  var closestMatchStart = Int.MAX_VALUE
  var closestMatchEnd = Int.MAX_VALUE
  var closestBackRef: String? = null
  var caretInClosestMatch = false
  var direction = Direction.FORWARDS

  // Find the closest pattern containing or after the caret offset, if any exist.
  var patternIndex = 0
  for ((pattern, searchPair) in closings + openings) {
    val matcher = Pattern.compile(pattern).matcher(currentLineChars)

    while (matcher.find()) {
      val matchStart = matcher.start()
      val matchEnd = matcher.end()

      if (offset >= matchEnd) continue

      // We prefer matches containing the cursor over matches after the cursor.
      // If the cursor in inside multiple patterns, pick the smaller one.
      var foundCloserMatch = false
      if (offset in matchStart until matchEnd) {
        if (!caretInClosestMatch || (matchEnd - matchStart < closestMatchEnd - closestMatchStart)) {
          caretInClosestMatch = true
          foundCloserMatch = true
        }
      } else if (!caretInClosestMatch && matchStart < closestMatchStart &&
        !containsDefaultPairs(currentLineChars.subSequence(offset, matchStart))
      ) {
        // A default pair after the cursor is preferred over any extended pairs after the cursor.
        foundCloserMatch = true
      }

      if (foundCloserMatch) {
        closestSearchPair = searchPair
        closestMatchStart = matchStart
        closestMatchEnd = matchEnd
        closestBackRef = if (matcher.groupCount() > 0) matcher.group(1) else null
        direction = if (closings.isNotEmpty() && patternIndex in 0 until closings.size) Direction.BACKWARDS else Direction.FORWARDS
      }
    }
    patternIndex++
  }

  if (closestSearchPair != null) {
    val targetOpeningPattern: String
    val targetClosingPattern: String

    // Substitute any captured back references to the search patterns, if necessary.
    if (closestBackRef != null) {
      targetOpeningPattern = String.format(closestSearchPair.first, closestBackRef)
      targetClosingPattern = String.format(closestSearchPair.second, closestBackRef)
    } else {
      targetOpeningPattern = closestSearchPair.first
      targetClosingPattern = closestSearchPair.second
    }

    // HTML attributes are a special case where the cursor is inside of quotes, but we want to jump as if we were
    // anywhere else inside the opening tag.
    val currentPsiElement = PsiHelper.getFile(editor)!!.findElementAt(caretOffset)
    val skipComments = !isComment(currentPsiElement)
    val skipQuotes = !isQuoted(currentPsiElement) || isHtmlAttribute(currentPsiElement)

    val initialPatternStart = currentLineStart + closestMatchStart
    val initialPatternEnd = currentLineStart + closestMatchEnd
    val searchParams = MatchitSearchParams(initialPatternStart, initialPatternEnd, targetOpeningPattern, targetClosingPattern, skipComments, skipQuotes)

    val matchingPairOffset = if (direction == Direction.FORWARDS) {
      findClosingPair(editor, isInOpPending, searchParams)
    } else {
      findOpeningPair(editor, searchParams)
    }

    // If the user is on a valid pattern, but we didn't find a matching pair, then the cursor shouldn't move.
    // We return the current caret offset to reflect that case, as opposed to -1 which means the cursor isn't on a
    // valid pattern at all.
    return if (matchingPairOffset < 0) caretOffset else matchingPairOffset
  }

  return -1
}

private fun findClosingPair(editor: Editor, isInOpPending: Boolean, searchParams: MatchitSearchParams): Int {
  val (_, searchStartOffset, openingPattern, closingPattern, skipComments, skipStrings) = searchParams
  val chars = editor.document.charsSequence
  val searchSpace = chars.subSequence(searchStartOffset, chars.length)

  val compiledClosingPattern = Pattern.compile(closingPattern)
  val compiledSearchPattern = Pattern.compile(String.format("(?<opening>%s)|(?<closing>%s)", openingPattern, closingPattern))
  val matcher = compiledSearchPattern.matcher(searchSpace)

  // We're looking for the first closing pair that isn't already matched by an opening.
  // As we find opening patterns, we push their offsets to this stack and pop whenever we find a closing pattern,
  // effectively crossing off that item from our search.
  val unmatchedOpeningPairs: Deque<Int> = ArrayDeque()
  while (matcher.find()) {
    val matchOffset = if (isInOpPending) {
      searchStartOffset + matcher.end() - 1
    } else {
      searchStartOffset + matcher.start()
    }

    if (matchShouldBeSkipped(editor, matchOffset, skipComments, skipStrings)) {
      continue
    }

    val openingGroup = matcher.group("opening")
    val foundOpeningPattern = openingGroup != null
    val foundMiddlePattern = foundOpeningPattern && compiledClosingPattern.matcher(openingGroup).matches()

    if (foundMiddlePattern) {
      // Middle patterns e.g. "elsif" can appear any number of times between a strict opening and a strict closing.
      if (!unmatchedOpeningPairs.isEmpty()) {
        unmatchedOpeningPairs.pop()
        unmatchedOpeningPairs.push(matchOffset)
      } else {
        return matchOffset
      }
    } else if (foundOpeningPattern) {
      unmatchedOpeningPairs.push(matchOffset)
    } else {
      // Found a closing pattern
      if (!unmatchedOpeningPairs.isEmpty()) {
        unmatchedOpeningPairs.pop()
      } else {
        return matchOffset
      }
    }
  }

  return -1
}

private fun findOpeningPair(editor: Editor, searchParams: MatchitSearchParams): Int {
  val (searchEndOffset, _, openingPattern, closingPattern, skipComments, skipStrings) = searchParams
  val chars = editor.document.charsSequence
  val searchSpace = chars.subSequence(0, searchEndOffset)

  val compiledClosingPattern = Pattern.compile(closingPattern)
  val compiledSearchPattern = Pattern.compile(String.format("(?<opening>%s)|(?<closing>%s)", openingPattern, closingPattern))
  val matcher = compiledSearchPattern.matcher(searchSpace)

  val unmatchedOpeningPairs: Deque<Int> = ArrayDeque()
  while (matcher.find()) {
    val matchOffset = matcher.start()

    if (matchShouldBeSkipped(editor, matchOffset, skipComments, skipStrings)) {
      continue
    }

    val openingGroup = matcher.group("opening")
    val foundOpeningPattern = openingGroup != null
    val foundMiddlePattern = foundOpeningPattern && compiledClosingPattern.matcher(openingGroup).matches()

    if (foundMiddlePattern) {
      if (!unmatchedOpeningPairs.isEmpty()) {
        unmatchedOpeningPairs.pop()
        unmatchedOpeningPairs.push(matchOffset)
      } else {
        unmatchedOpeningPairs.push(matchOffset)
      }
    } else if (foundOpeningPattern) {
      unmatchedOpeningPairs.push(matchOffset)
    } else if (!unmatchedOpeningPairs.isEmpty()) {
      // Found a closing pattern. We check the stack isn't empty to handle malformed code.
      unmatchedOpeningPairs.pop()
    }
  }

  return if (!unmatchedOpeningPairs.isEmpty()) {
    unmatchedOpeningPairs.pop()
  } else {
    -1
  }
}

private fun containsDefaultPairs(chars: CharSequence): Boolean {
  for (c in chars) {
    if (c in DEFAULT_PAIRS) return true
  }
  return false
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
