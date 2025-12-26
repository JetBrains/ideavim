/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.matchit

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import java.util.*
import java.util.regex.Pattern

/**
 * Port of matchit.vim (https://github.com/chrisbra/matchit)
 * @author Martin Yzeiri (@myzeiri)
 */
internal class Matchit : VimExtension {

  override fun getName(): String = "matchit"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys("<Plug>(MatchitMotion)"), owner, MatchitHandler(false), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys("<Plug>(MatchitMotion)"), owner, MatchitHandler(false), false)
    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys("%"), owner, injector.parser.parseKeys("<Plug>(MatchitMotion)"), true)

    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys("<Plug>(ReverseMatchitMotion)"), owner, MatchitHandler(true), false)
    VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, injector.parser.parseKeys("<Plug>(ReverseMatchitMotion)"), owner, MatchitHandler(true), false)
    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys("g%"), owner, injector.parser.parseKeys("<Plug>(ReverseMatchitMotion)"), true)
  }

  private class MatchitAction : MotionActionHandler.ForEachCaret() {
    var reverse = false
    var isInOpPending = false

    override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

    override fun getOffset(
      editor: VimEditor,
      caret: ImmutableVimCaret,
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

  private class MatchitHandler(private val reverse: Boolean) : ExtensionHandler {

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val keyHandler = KeyHandler.getInstance()
      val keyState = keyHandler.keyHandlerState

      // Reset the command count so it doesn't transfer onto subsequent commands.
      keyState.commandBuilder.resetCount()

      // Normally we want to jump to the start of the matching pair. But when moving forward in operator
      // pending mode, we want to include the entire match. isInOpPending makes that distinction.
      if (editor.mode is Mode.OP_PENDING) {
        val matchitAction = MatchitAction()
        matchitAction.reverse = reverse
        matchitAction.isInOpPending = true

        keyState.commandBuilder.addAction(matchitAction)
      } else {
        editor.sortedCarets().forEach { caret ->
          injector.jumpService.saveJumpLocation(editor)
          caret.moveToOffset(
            getMatchitOffset(
              editor.ij,
              caret.ij,
              operatorArguments.count0,
              isInOpPending = false,
              reverse
            ))
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
      this.reversedClosings + newLanguagePatterns.reversedClosings,
    )
  }

  // Helper constructors for the most common language patterns. More complicated structures, i.e. those that require
  // back references, should be built with the default constructor.
  companion object {
    operator fun invoke(openingPattern: String, closingPattern: String): LanguagePatterns {
      val openingPatternsTable = linkedMapOf(openingPattern to Pair(openingPattern, closingPattern))
      val closingPatternsTable = linkedMapOf(closingPattern to Pair(openingPattern, closingPattern))
      return LanguagePatterns(openingPatternsTable, closingPatternsTable)
    }

    operator fun invoke(openingPattern: String, middlePattern: String, closingPattern: String): LanguagePatterns {
      val openingAndMiddlePatterns = "(?:$openingPattern)|(?:$middlePattern)"
      val middleAndClosingPatterns = "(?:$middlePattern)|(?:$closingPattern)"

      val openings = linkedMapOf(openingAndMiddlePatterns to Pair(openingAndMiddlePatterns, middleAndClosingPatterns))
      val closings = linkedMapOf(closingPattern to Pair(openingPattern, closingPattern))

      // Supporting the g% motion is just a matter of rearranging the patterns table.
      // This particular arrangement relies on our checking if the cursor is on a closing pattern first.
      val reversedOpenings = linkedMapOf(
        openingPattern to Pair(openingPattern, closingPattern),
        middlePattern to Pair(openingAndMiddlePatterns, middlePattern),
      )
      val reversedClosings = linkedMapOf(middleAndClosingPatterns to Pair(openingAndMiddlePatterns, middleAndClosingPatterns))

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
  val skipStrings: Boolean,
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
    } else if (fileTypeName == "JAVA" || fileExtension == "java") {
      this.javaPatterns
    } else if (fileTypeName == "Ruby" || fileExtension == "rb") {
      this.rubyPatterns
    } else if (fileTypeName == "RHTML" || fileExtension == "erb") {
      this.rubyAndHtmlPatterns
    } else if (fileTypeName == "PHP" || fileExtension == "php") {
      this.phpPatterns
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
    "HTML", "XML", "XHTML", "JSP", "JavaScript", "JSX Harmony",
    "TypeScript", "TypeScript JSX", "Vue.js", "Handlebars/Mustache",
    "Asp", "Razor", "UXML", "Xaml",
  )

  private val htmlPatterns = createHtmlPatterns()
  private val javaPatterns = createJavaPatterns()
  private val rubyPatterns = createRubyPatterns()
  private val rubyAndHtmlPatterns = rubyPatterns + htmlPatterns
  private val phpPatterns = createPhpPatterns()
  private val cPatterns = createCPatterns()
  private val gnuMakePatterns = createGnuMakePatterns()
  private val cMakePatterns = createCMakePatterns()

  private fun createHtmlPatterns(tagNamePattern: String = "[^/\\s><]+"): LanguagePatterns {
    // By default, a tag name may contain any characters except slashes, whitespace, and angle brackets.
    // A custom tagNamePattern can be provided if we need to interop with other languages.

    // An opening tag consists of "<" followed by a tag name and optionally some additional text after whitespace.
    // We surround the tag name in a capture group so that we can use it when searching for a match later.
    // Note the assertion on "<" to not match on that character. If the cursor is on an angle bracket, then we want to
    // match angle brackets, not HTML tags.
    val openingTagPattern = String.format("(?<=<)(%s)(?:\\s[^<>]*(\".*\")?)?", tagNamePattern)

    // A closing tag consists of a "<" followed by a slash, the tag name, and a ">".
    val closingTagPattern = String.format("(?<=<)/(%s)(?=>)", tagNamePattern)

    // The tag name is left as %s so we can substitute the back reference we captured.
    val htmlSearchPair = Pair("(?<=<)%s(?:\\s[^<>]*(\".*\")?)?(?=>)", "(?<=<)/%s>")

    return (
      LanguagePatterns("<", ">") +
        LanguagePatterns(linkedMapOf(openingTagPattern to htmlSearchPair), linkedMapOf(closingTagPattern to htmlSearchPair))
      )
  }
  
  private fun createJavaPatterns(): LanguagePatterns {
    return (
        LanguagePatterns("\\b(?<!else\\s+)if\\b", "\\belse\\s+if\\b", "\\belse(?!\\s+if)\\b") +
          LanguagePatterns("\\bdo\\b", "\\bwhile\\b") +
          LanguagePatterns("\\btry\\b", "\\bcatch\\b", "\\bfinally\\b")
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

  private fun createPhpPatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/php.vim
    val loopOpenings = "(?:\\b(?:for|do|foreach|switch)\\b)|(?:\\bwhile \\(.*?\\)\\s*:)"
    val loopClosings = "(?:\\bend(?:for|foreach|while|switch)\\b)|(?:\\bwhile \\(.*\\)\\s*;)"

    val openingDoc = "(?<=<<<)\\s*'?(\\w+)'?"
    val closingDoc = "^\\s*(\\w+)\\s*[,;]"
    val docSearchPair = Pair("(?<=<<<)\\s*'?%s'?", "%s") // %s for the captured doc string name.
    val docPatterns = LanguagePatterns(linkedMapOf(openingDoc to docSearchPair), linkedMapOf(closingDoc to docSearchPair))

    return (
      LanguagePatterns("(?<=<)\\?(?:php|=)?", "\\?>") +
        LanguagePatterns("<(?=\\?(?:php|=)?)", "\\?>") +
        LanguagePatterns("\\bif\\b", "\\b(?:else|elseif)\\b", "\\bendif\\b") +
        LanguagePatterns(loopOpenings, "\\b(?:case|break|continue)\\b", loopClosings) +
        docPatterns +
        createHtmlPatterns("[^/\\s><?]+") // Exclude question marks from tag names.
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

/**
 * Reads and parses the b:match_words buffer variable to create custom matching patterns.
 * The format is "opening:closing,opening2:closing2" or "opening:middle:closing".
 */
private fun getCustomMatchWords(editor: Editor): LanguagePatterns? {
  val vimEditor = editor.vim

  // Try to read b:match_words variable
  val matchWordsVar = try {
    val scope = com.maddyhome.idea.vim.vimscript.model.expressions.Scope.BUFFER_VARIABLE
    val variable = com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression(scope, "match_words")

    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val vimContext = com.maddyhome.idea.vim.vimscript.model.Script(mutableListOf())

    injector.variableService.getNullableVariableValue(variable, vimEditor, context, vimContext)
  } catch (e: Exception) {
    null
  }

  // Extract the string value
  val matchWordsString = when (matchWordsVar) {
    is com.maddyhome.idea.vim.vimscript.model.datatypes.VimString -> matchWordsVar.value
    else -> return null
  }

  if (matchWordsString.isBlank()) {
    return null
  }

  // Parse the match_words string
  // Format: "opening:closing,opening2:closing2" or "opening:middle:closing"
  val pairGroups = matchWordsString.split(',')

  var combinedPatterns: LanguagePatterns? = null

  for (pairGroup in pairGroups) {
    val parts = pairGroup.trim().split(':')

    when (parts.size) {
      2 -> {
        // Simple opening:closing pair
        val opening = parts[0].trim()
        val closing = parts[1].trim()
        if (opening.isNotEmpty() && closing.isNotEmpty()) {
          val newPattern = LanguagePatterns(opening, closing)
          combinedPatterns = if (combinedPatterns == null) {
            newPattern
          } else {
            combinedPatterns + newPattern
          }
        }
      }
      3 -> {
        // opening:middle:closing pattern
        val opening = parts[0].trim()
        val middle = parts[1].trim()
        val closing = parts[2].trim()
        if (opening.isNotEmpty() && middle.isNotEmpty() && closing.isNotEmpty()) {
          val newPattern = LanguagePatterns(opening, middle, closing)
          combinedPatterns = if (combinedPatterns == null) {
            newPattern
          } else {
            combinedPatterns + newPattern
          }
        }
      }
      // Ignore invalid patterns
    }
  }

  return combinedPatterns
}

private val DEFAULT_PAIRS = setOf('(', ')', '[', ']', '{', '}')

private fun getMatchitOffset(editor: Editor, caret: Caret, count0: Int, isInOpPending: Boolean, reverse: Boolean): Int {
  val virtualFile = EditorHelper.getVirtualFile(editor)
  var caretOffset = caret.offset

  // Handle the case where visual mode has brought the cursor past the end of the line.
  val lineEndOffset = editor.vim.getLineEndOffset(caret.logicalPosition.line, true)
  if (caretOffset > 0 && caretOffset == lineEndOffset) {
    caretOffset--
  }

  val currentChar = editor.document.charsSequence[caretOffset]
  var motionOffset: Int? = null

  if (count0 > 0) {
    // Matchit doesn't affect the percent motion, so we fall back to the default behavior.
    motionOffset = VimPlugin.getMotion().moveCaretToLinePercent(editor.vim, caret.vim, count0)
  } else {
    // Check the simplest case first.
    if (DEFAULT_PAIRS.contains(currentChar)) {
      motionOffset = getMotionOffset(VimPlugin.getMotion().moveCaretToMatchingPair(editor.vim, caret.vim))
    } else {
      // Combine file-type patterns with custom b:match_words patterns
      val fileTypePatterns = FileTypePatterns.getMatchitPatterns(virtualFile)
      val customPatterns = getCustomMatchWords(editor)
      val matchitPatterns = when {
        fileTypePatterns != null && customPatterns != null -> fileTypePatterns + customPatterns
        fileTypePatterns != null -> fileTypePatterns
        customPatterns != null -> customPatterns
        else -> null
      }

      if (matchitPatterns != null) {
        motionOffset = if (reverse) {
          findMatchingPair(editor, caretOffset, isInOpPending, matchitPatterns.reversedOpenings, matchitPatterns.reversedClosings)
        } else {
          findMatchingPair(editor, caretOffset, isInOpPending, matchitPatterns.openings, matchitPatterns.closings)
        }
      }

      if (motionOffset == null || motionOffset < 0) {
        // Use default motion if the file type isn't supported or we didn't find any extended pairs.
        motionOffset = getMotionOffset(VimPlugin.getMotion().moveCaretToMatchingPair(editor.vim, caret.vim))
      }
    }
  }

  if (motionOffset != null && motionOffset >= 0) {
    motionOffset = editor.vim.normalizeOffset(motionOffset, false)
  }

  return motionOffset ?: -1
}

private fun getMotionOffset(motion: Motion): Int? {
  return when (motion) {
    is Motion.AdjustedOffset, is Motion.AbsoluteOffset -> motion.offset
    is Motion.Error, is Motion.NoMotion -> null
  }
}

private fun findMatchingPair(
  editor: Editor,
  caretOffset: Int,
  isInOpPending: Boolean,
  openings: PatternsTable,
  closings: PatternsTable,
): Int {
  // For better performance, we limit our search to the current line. This way we don't have to scan the entire file
  // to determine if we're on a pattern or not. The original plugin behaves the same way.
  val currentLineStart = IjVimEditor(editor).getLineStartForOffset(caretOffset)
  val currentLineEnd = IjVimEditor(editor).getLineEndForOffset(caretOffset)
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
    val initialPatternStart = currentLineStart + closestMatchStart
    val initialPatternEnd = currentLineStart + closestMatchEnd

    val initialPsiElement = PsiHelper.getFile(editor)!!.findElementAt(initialPatternStart)
    if (isGlobalSkippedElement(initialPsiElement)) {
      // Check if the element is one to always ignore, regardless of the cursor position.
      // In Ruby, however, we still want to jump if the cursor is on e.g. a "do" after an "if", but that "do" should be
      // ignored when the cursor is on "if".
      return -1
    }

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

    val skipComments = !isComment(initialPsiElement)
    val skipQuotes = !isQuoted(initialPsiElement)
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

private fun getElementType(psiElement: PsiElement?): String? {
  return psiElement?.node?.elementType?.debugName
}

private fun matchShouldBeSkipped(editor: Editor, offset: Int, skipComments: Boolean, skipStrings: Boolean): Boolean {
  val psiFile = PsiHelper.getFile(editor)
  val psiElement = psiFile!!.findElementAt(offset)

  if (isSkippedRubyElement(psiElement) || isGlobalSkippedElement(psiElement)) {
    return true
  }

  val insideComment = isComment(psiElement)
  val insideQuotes = isQuoted(psiElement)
  return (skipComments && insideComment) || (!skipComments && !insideComment) ||
    (skipStrings && insideQuotes) || (!skipStrings && !insideQuotes)
}

private fun isSkippedRubyElement(psiElement: PsiElement?): Boolean {
  // We want to ignore "do" keywords after conditions, any inline "if" or "unless" expressions, regex strings,
  // and identifiers like "Foo.class",
  val type = getElementType(psiElement)
  return type == "do_cond" || type == "if modifier" || type == "unless modifier" ||
    type == "regexp content" || type == "identifier"
}

private fun isGlobalSkippedElement(psiElement: PsiElement?): Boolean {
  val type = getElementType(psiElement)
  val parentType = getElementType(psiElement?.parent)

  // JavaScript: Ignore regex strings, arrow functions, and angle brackets used for comparisons.
  return type == "REGEXP_LITERAL" || type == "EQGT" || parentType == "BINARY_EXPRESSION" ||
    // PHP: Ignore arrow functions and comparison brackets.
    type == "arrow" || parentType == "Relational expression"
}

private fun isComment(psiElement: PsiElement?): Boolean {
  return PsiTreeUtil.getParentOfType(psiElement, PsiComment::class.java, false) != null
}

private fun isQuoted(psiElement: PsiElement?): Boolean {
  val type = getElementType(psiElement)
  return type == "STRING_LITERAL" || type == "XML_ATTRIBUTE_VALUE_TOKEN" ||
    type == "string content" // Ruby specific.
}
