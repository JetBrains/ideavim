/*
 * Copyright 2003-2026 The IdeaVim authors
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
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.contains
import kotlin.math.max
import kotlin.math.min

class MatchAction : MotionActionHandler.ForEachCaret() {
  var reverse = false
  var isInOpPending = false

  /** The biggest count that repeats the jump rather than going to a percentage of the file, see [isPercentJump]. */
  var maxRepeatCount: Int? = null

  /** The count given to the motion, e.g. the 2 of `d2%`, which the handler takes off the command. */
  var count0: Int? = null

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override var supportsLinewiseDeletePromotion: Boolean = false

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return getMatchupOffset(
      editor.ij,
      caret.ij,
      count0 ?: operatorArguments.count0,
      isInOpPending,
      reverse,
      maxRepeatCount = maxRepeatCount,
    ).toMotionOrError()
  }

  override fun process(cmd: Command) {
    val isPercentJump = isPercentJump(count0 ?: cmd.rawCount, reverse, maxRepeatCount)
    motionType = if (isPercentJump) MotionType.LINE_WISE else MotionType.INCLUSIVE
  }

  override var motionType: MotionType = MotionType.INCLUSIVE
}

/**
 * A count turns % into the "jump to a percentage of the file" motion. Matchit doesn't extend that, while matchup only
 * does so for a count above [maxRepeatCount], and repeats the jump for a smaller one. Its g% always repeats.
 */
private fun isPercentJump(count0: Int, reverse: Boolean, maxRepeatCount: Int?): Boolean {
  if (count0 <= 0) return false
  if (maxRepeatCount == null) return true
  return !reverse && count0 > maxRepeatCount
}

private fun getMatchupOffset(
  editor: Editor,
  caret: Caret,
  count0: Int,
  isInOpPending: Boolean,
  reverse: Boolean,
  isCursorAtEnd: Boolean = false,
  maxRepeatCount: Int? = null,
): Int {
  if (isPercentJump(count0, reverse, maxRepeatCount)) {
    return VimPlugin.getMotion().moveCaretToLinePercent(editor.vim, caret.vim, count0)
  }

  var offset = caret.offset
  repeat(max(count0, 1)) { jump ->
    val jumpOffset = findPairOffset(editor, caret.vim, offset, isInOpPending, reverse, isCursorAtEnd)
    if (jumpOffset == null || jumpOffset < 0) return if (jump == 0) jumpOffset ?: -1 else offset
    offset = editor.vim.normalizeOffset(jumpOffset, false)
  }
  return offset
}

private fun findPairOffset(
  editor: Editor,
  caret: ImmutableVimCaret,
  offset: Int,
  isInOpPending: Boolean,
  reverse: Boolean,
  isCursorAtEnd: Boolean,
): Int? {
  val offsetWithinLine = offsetWithinLine(editor, offset)
  return findExtendedPairOffset(editor, offsetWithinLine, isInOpPending, reverse, isCursorAtEnd)
    ?: getMotionOffset(VimPlugin.getMotion().moveCaretToMatchingPair(editor.vim, CaretAtOffset(caret, offset)))
}

/**
 * [caret] moved to [offset], for searches that start from a caret.
 */
private class CaretAtOffset(caret: ImmutableVimCaret, override val offset: Int) : ImmutableVimCaret by caret {
  override fun getBufferPosition(): BufferPosition = editor.offsetToBufferPosition(offset)
}

/**
 * With [isMatchup], the cursor lands on the end of a middle or closing word it jumps forward to, unless
 * `g:matchup_motion_cursor_end` is 0, and `d%` over whole lines deletes the lines.
 */
class MatchHandler(private val reverse: Boolean, private val isMatchup: Boolean = false) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val keyHandler = KeyHandler.getInstance()
    val keyState = keyHandler.keyHandlerState

    // Reset the command count so it doesn't transfer onto subsequent commands.
    keyState.commandBuilder.resetCount()

    // Normally we want to jump to the start of the matching pair. But when moving forward in operator
    // pending mode, we want to include the entire match. isInOpPending makes that distinction.
    if (editor.mode is Mode.OP_PENDING) {
      val forcedMotion = keyState.commandBuilder.forcedMotion
      val matchitAction = MatchAction()
      matchitAction.reverse = reverse
      // A forced characterwise motion turns this into an exclusive one, which for matchup leaves out the whole word.
      matchitAction.isInOpPending = !(isMatchup && forcedMotion == SelectionType.CHARACTER_WISE)
      matchitAction.supportsLinewiseDeletePromotion = isMatchup && forcedMotion == null
      if (isMatchup) {
        matchitAction.maxRepeatCount = maxRepeatCount(editor)
        matchitAction.count0 = operatorArguments.count0
      }

      keyState.commandBuilder.addAction(matchitAction)
    } else {
      val isCursorAtEnd = isMatchup && getGlobalIntVariable(editor, "matchup_motion_cursor_end", 1) != 0
      val maxRepeatCount = if (isMatchup) maxRepeatCount(editor) else null
      editor.sortedCarets().forEach { caret ->
        injector.jumpService.saveJumpLocation(editor)
        caret.moveToOffset(
          getMatchupOffset(
            editor.ij,
            caret.ij,
            operatorArguments.count0,
            isInOpPending = false,
            reverse,
            isCursorAtEnd,
            maxRepeatCount,
          )
        )
      }
    }
  }

  /**
   * `g:matchup_motion_override_Npercent`, where 0 always goes to a percentage, and 100 or more never does.
   */
  private fun maxRepeatCount(editor: VimEditor): Int {
    val maxRepeatCount = getGlobalIntVariable(editor, "matchup_motion_override_Npercent", 6)
    return if (maxRepeatCount >= 100) Int.MAX_VALUE else maxRepeatCount
  }
}

internal fun getGlobalIntVariable(editor: VimEditor, name: String, default: Int): Int {
  val value = injector.variableService.getNullableVariableValue(
    VariableExpression(Scope.GLOBAL_VARIABLE, name),
    editor,
    injector.executionContextManager.getEditorExecutionContext(editor),
    VimPluginContext
  ) ?: return default
  return try {
    value.toVimNumber().value
  } catch (e: ExException) {
    default
  }
}

/**
 * To find a match, we need two patterns that describe what the opening and closing pairs look like, and we need a
 * pattern to describe the valid starting points for the jump. A PatternsTable maps valid starting points to the pair of
 * patterns needed for the search.
 *
 * All patterns are written in Vim's regex dialect (see `:help pattern`), because they are handed to IdeaVim's own
 * regex engine via [VimRegex] rather than to [java.util.regex.Pattern].
 *
 * We pass around strings instead of compiled patterns since a pattern may require back references to be added
 * before the search can proceed. E.g. for HTML, we use a general pattern to check if the cursor is inside a tag. The
 * pattern captures the tag's name as a back reference so we can later search for something more specific like "</div>"
 */
private typealias PatternsTable = Map<String, Pair<String, String>>

/**
 * The placeholder that a search pair uses for a back reference captured by the pattern that the cursor is on.
 *
 * We substitute it with a plain [String.replace] rather than [String.format]: Vim patterns are full of `%` characters
 * (`\%(`, `\%[`, `\%d`, ...), which [String.format] would try to read as conversion specifiers.
 */
private const val BACK_REF_PLACEHOLDER = "%s"

/**
 * A language can have many different matching pairs. We divide the patterns into four PatternsTables. `openings` and
 * `closings` handle the % motion while `reversedOpenings` and `reversedClosings` handle the g% motion.
 */
data class LanguagePatterns(
  val openings: PatternsTable,
  val closings: PatternsTable,
  val reversedOpenings: PatternsTable,
  val reversedClosings: PatternsTable,
  val blocks: List<BlockPatterns> = emptyList(),
) {
  // Helper constructor for languages that don't need reversed patterns.
  constructor(openings: PatternsTable, closings: PatternsTable, blocks: List<BlockPatterns> = emptyList()) :
    this(openings, closings, openings, closings, blocks)

  operator fun plus(newLanguagePatterns: LanguagePatterns): LanguagePatterns {
    return LanguagePatterns(
      this.openings + newLanguagePatterns.openings,
      this.closings + newLanguagePatterns.closings,
      this.reversedOpenings + newLanguagePatterns.reversedOpenings,
      this.reversedClosings + newLanguagePatterns.reversedClosings,
      this.blocks + newLanguagePatterns.blocks,
    )
  }

  // Helper constructors for the most common language patterns. More complicated structures, i.e. those that require
  // back references, should be built with the default constructor.
  companion object {
    operator fun invoke(openingPattern: String, closingPattern: String): LanguagePatterns {
      val openingPatternsTable = linkedMapOf(openingPattern to Pair(openingPattern, closingPattern))
      val closingPatternsTable = linkedMapOf(closingPattern to Pair(openingPattern, closingPattern))
      return LanguagePatterns(
        openingPatternsTable,
        closingPatternsTable,
        listOf(BlockPatterns(openingPattern, closingPattern)),
      )
    }

    operator fun invoke(openingPattern: String, middlePattern: String, closingPattern: String): LanguagePatterns {
      val openingAndMiddlePatterns = """\%($openingPattern\)\|\%($middlePattern\)"""
      val middleAndClosingPatterns = """\%($middlePattern\)\|\%($closingPattern\)"""

      val openings = linkedMapOf(openingAndMiddlePatterns to Pair(openingAndMiddlePatterns, middleAndClosingPatterns))
      val closings = linkedMapOf(closingPattern to Pair(openingPattern, closingPattern))

      // Supporting the g% motion is just a matter of rearranging the patterns table.
      // This particular arrangement relies on our checking if the cursor is on a closing pattern first.
      val reversedOpenings = linkedMapOf(
        openingPattern to Pair(openingPattern, closingPattern),
        middlePattern to Pair(openingAndMiddlePatterns, middlePattern),
      )
      val reversedClosings =
        linkedMapOf(middleAndClosingPatterns to Pair(openingAndMiddlePatterns, middleAndClosingPatterns))

      return LanguagePatterns(
        openings,
        closings,
        reversedOpenings,
        reversedClosings,
        listOf(BlockPatterns(openingPattern, closingPattern, middlePattern = middlePattern)),
      )
    }
  }
}

/**
 * The patterns of a block, with its middle words kept apart from the opening and closing ones. When [hasBackReference]
 * is set, the opening and closing patterns capture a name in their first group, and an opening only pairs with a
 * closing of the same name.
 */
data class BlockPatterns(
  val openingPattern: String,
  val closingPattern: String,
  val hasBackReference: Boolean = false,
  val middlePattern: String? = null,
)

/**
 * All the information we need to find a match.
 */
data class MatchupSearchParams(
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
object FileTypePatterns {

  fun getMatchupPatterns(virtualFile: VirtualFile?, editor: VimEditor): LanguagePatterns? {
    // fileType is only populated for files supported by the user's IDE + language plugins.
    // Checking the file's name or extension is a simple fallback which also makes unit testing easier.
    val fileTypeName = virtualFile?.fileType?.name
    val fileName = virtualFile?.nameWithoutExtension
    val fileExtension = virtualFile?.extension

    val filePatterns = if (fileTypeName in htmlLikeFileTypes) {
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
      null
    }

    return listOfNotNull(filePatterns, getUserPatterns(editor)).reduceOrNull(LanguagePatterns::plus)
  }

  fun isHtmlLike(virtualFile: VirtualFile?): Boolean {
    val fileTypeName = virtualFile?.fileType?.name
    val fileExtension = virtualFile?.extension
    return fileTypeName in htmlLikeFileTypes || fileTypeName == "RHTML" || fileExtension == "erb" ||
      fileTypeName == "PHP" || fileExtension == "php"
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

  private fun createHtmlPatterns(tagNamePattern: String = """[^/[:space:]><]\+"""): LanguagePatterns {
    // The tag name is captured so that we can substitute it into the search pair as a back reference. The "<" is
    // matched with a look-behind rather than consumed: when the cursor is on an angle bracket we want to match angle
    // brackets, not HTML tags. A custom tagNamePattern lets other languages narrow what a tag name may contain.
    val openingTagPattern = """<\@1<=\($tagNamePattern\)\%(\s[^<>]*\%(".*"\)\=\)\="""
    val closingTagPattern = """<\@1<=/\($tagNamePattern\)>\@="""
    val htmlSearchPair = Pair("""<\@1<=%s\%(\s[^<>]*\%(".*"\)\=\)\=>\@=""", """<\@1<=/%s>""")
    // A block's opening tag runs up to its ">", so that the text inside the block starts after it.
    val blockOpeningTagPattern = """<\@1<=\($tagNamePattern\)[^>]*\%(>\|$\)"""

    return (
      LanguagePatterns("<", ">") +
        LanguagePatterns(
          linkedMapOf(openingTagPattern to htmlSearchPair),
          linkedMapOf(closingTagPattern to htmlSearchPair),
          listOf(BlockPatterns(blockOpeningTagPattern, closingTagPattern, hasBackReference = true)),
        )
      )
  }

  private fun createJavaPatterns(): LanguagePatterns {
    return (
      LanguagePatterns("""\%(else\s\+\)\@<!\<if\>""", """\<else\s\+if\>""", """\<else\>\%(\s\+if\)\@!""") +
        LanguagePatterns("""\<do\>""", """\<while\>""") +
        LanguagePatterns("""\<try\>""", """\<catch\>""", """\<finally\>""")
      )
  }

  private fun createRubyPatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/ruby.vim
    // On the class keyword we exclude a following equal sign, since it clashes with the HTML attribute.
    val openingKeywords =
      """\%(\<\%(do\|if\|unless\|case\|def\|for\|while\|until\|module\|begin\)\>\)\|\%(\<class\>[^=]\)"""
    val endKeyword = """\<end\>"""

    // A "middle" keyword is one that can act as both an opening or a closing pair. E.g. "elsif" can appear any number
    // of times between the opening "if" and the closing "end".
    val middleKeywords = """\%(\<\%(else\|elsif\|break\|when\|rescue\|ensure\|redo\|next\|retry\)\>\)"""

    // The cursor shouldn't jump to the equal sign on a block comment, so we exclude it with a look-behind assertion.
    val blockCommentStart = """=\@1<=begin\>"""
    val blockCommentEnd = """=\@1<=end\>"""

    return (
      LanguagePatterns(blockCommentStart, blockCommentEnd) +
        LanguagePatterns(openingKeywords, middleKeywords, endKeyword)
      )
  }

  private fun createPhpPatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/php.vim
    val loopOpenings = """\%(\<\%(for\|do\|foreach\|switch\)\>\)\|\%(\<while (.\{-})\s*:\)"""
    val loopClosings = """\%(\<end\%(for\|foreach\|while\|switch\)\>\)\|\%(\<while (.*)\s*;\)"""

    // The doc string name is captured and substituted into the search pair as a back reference.
    val openingDoc = """\%(<<<\)\@3<=\s*'\=\(\w\+\)'\="""
    val closingDoc = """^\s*\(\w\+\)\s*[,;]"""
    val docSearchPair = Pair("""\%(<<<\)\@3<=\s*'\=%s'\=""", "%s")
    val docPatterns = LanguagePatterns(
      linkedMapOf(openingDoc to docSearchPair),
      linkedMapOf(closingDoc to docSearchPair),
      listOf(BlockPatterns(openingDoc, closingDoc, hasBackReference = true)),
    )

    return (
      LanguagePatterns("""<\@1<=?\%(php\|=\)\=""", """?>""") +
        LanguagePatterns("""<\%(?\%(php\|=\)\=\)\@=""", """?>""") +
        LanguagePatterns("""\<if\>""", """\<\%(else\|elseif\)\>""", """\<endif\>""") +
        LanguagePatterns(loopOpenings, """\<\%(case\|break\|continue\)\>""", loopClosings) +
        docPatterns +
        createHtmlPatterns("""[^/[:space:]><?]\+""") // Exclude question marks from tag names.
      )
  }

  private fun createCPatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/c.vim
    return LanguagePatterns("""#\s*if\%(def\|ndef\)\=\>""", """#\s*\%(elif\|else\)\>""", """#\s*endif\>""")
  }

  private fun createGnuMakePatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/make.vim
    return (
      LanguagePatterns("""\<define\>""", """\<endef\>""") +
        LanguagePatterns(
          """\%(else \)\@5<!ifn\=\%(eq\|def\)\>""",
          """\<else\%(\s\+ifn\=\%(eq\|def\)\)\=\>""",
          """\<endif\>""",
        )
      )
  }

  private fun createCMakePatterns(): LanguagePatterns {
    // Original patterns: https://github.com/vim/vim/blob/master/runtime/ftplugin/cmake.vim
    return (
      LanguagePatterns("""\<if\>""", """\<else\%(if\)\=\>""", """\<endif\>""") +
        LanguagePatterns("""\<\%(foreach\|while\)\>""", """\<break\>""", """\<\%(endforeach\|endwhile\)\>""") +
        LanguagePatterns("""\<macro\>""", """\<endmacro\>""") +
        LanguagePatterns("""\<function\>""", """\<endfunction\>""")
      )
  }
}

/**
 * Reads the user's own pairs from the `b:match_words` buffer variable.
 *
 * See `:help b:match_words`. The value is a comma separated list of groups, each group being a colon separated list
 * of Vim patterns: `ini:mid:...:fin`. Commas and colons that belong to a pattern are escaped with a backslash.
 */private fun getUserPatterns(editor: VimEditor): LanguagePatterns? {
  val matchWords = injector.variableService.getNullableVariableValue(
    VariableExpression(Scope.BUFFER_VARIABLE, "match_words"),
    editor,
    injector.executionContextManager.getEditorExecutionContext(editor),
    VimPluginContext
  )?.toVimString()?.value ?: return null

  return splitUnescaped(matchWords, ',')
    .mapNotNull { group -> toLanguagePatterns(splitUnescaped(group, ':').filter(String::isNotEmpty)) }
    .reduceOrNull(LanguagePatterns::plus)
}

/**
 * Turns a single `ini:mid:...:fin` group into a [LanguagePatterns]. A group may list any number of middle patterns,
 * which are folded into one alternation.
 */
private fun toLanguagePatterns(patterns: List<String>): LanguagePatterns? {
  return when {
    patterns.size < 2 -> null
    patterns.size == 2 -> LanguagePatterns(patterns.first(), patterns.last())
    else -> {
      val middlePattern = patterns.subList(1, patterns.size - 1).joinToString("""\|""") { """\%($it\)""" }
      LanguagePatterns(patterns.first(), middlePattern, patterns.last())
    }
  }
}

/**
 * Splits [text] on every [delimiter] that isn't escaped with a backslash. The escapes themselves are left in place,
 * since they are part of the Vim pattern.
 */
private fun splitUnescaped(text: String, delimiter: Char): List<String> {
  val parts = mutableListOf<String>()
  val currentPart = StringBuilder()
  var index = 0
  while (index < text.length) {
    val char = text[index]
    when {
      char == '\\' && index + 1 < text.length -> {
        currentPart.append(char).append(text[index + 1])
        index++
      }

      char == delimiter -> {
        parts.add(currentPart.toString())
        currentPart.clear()
      }

      else -> currentPart.append(char)
    }
    index++
  }
  parts.add(currentPart.toString())
  return parts
}

private val DEFAULT_PAIRS = setOf('(', ')', '[', ']', '{', '}')

/**
 * Compiling a Vim pattern parses it and builds an NFA, which is far too expensive to redo for every pattern of a
 * language on every press of `%`. The set of patterns is small and fixed - the built-in tables plus whatever the user
 * put in `b:match_words` - so we simply keep them all.
 */
private val compiledPatterns = ConcurrentHashMap<String, Optional<VimRegex>>()

private fun compilePattern(pattern: String): VimRegex? {
  return compiledPatterns.computeIfAbsent(pattern) {
    try {
      Optional.of(VimRegex(it))
    } catch (e: VimRegexException) {
      // An invalid pattern, most likely one the user wrote in b:match_words, shouldn't break the % motion.
      Optional.empty()
    }
  }.orElse(null)
}

/**
 * Finds every match of [pattern] that lies fully inside `[startOffset, endOffset)`.
 *
 * We search the editor itself rather than a copy of its text, so that `^`, `$` and look-behinds see the real line
 * boundaries around the search range.
 */
fun findMatches(
  editor: VimEditor,
  pattern: String,
  startOffset: Int,
  endOffset: Int,
): List<VimMatchResult.Success> {
  if (startOffset >= endOffset) return emptyList()
  val regex = compilePattern(pattern) ?: return emptyList()
  return regex.findAll(editor, startOffset, endOffset).filter { it.range.endOffset <= endOffset }
}

/**
 * Returns the text captured by the first capture group of [pattern] at [matchStart], or null if it captured nothing.
 *
 * This re-runs the match instead of reading the groups off the results of [findMatches]: every match returned by
 * [VimRegex.findAll] shares the one group collection that the engine reuses across simulations, so once the search
 * is over only the last simulation's captures are left in it. Groups are trustworthy only while nothing else has
 * been matched since, which is why we read them right here.
 */
fun findBackReference(editor: VimEditor, pattern: String, matchStart: Int): String? {
  val regex = compilePattern(pattern) ?: return null
  val line = editor.offsetToBufferPosition(matchStart).line
  val match = regex.findInLine(editor, line, matchStart - editor.getLineStartOffset(line))
  return (match as? VimMatchResult.Success)?.groups?.get(1)?.value
}

fun String.withBackReference(backReference: String?): String {
  return if (backReference == null) this else replace(BACK_REF_PLACEHOLDER, backReference)
}


/**
 * Visual mode can leave the cursor one past the end of the line, where there is no character to match on.
 */
private fun offsetWithinLine(editor: Editor, offset: Int): Int {
  val lineEndOffset = editor.vim.getLineEndForOffset(offset)
  return if (offset > 0 && offset == lineEndOffset) offset - 1 else offset
}

/**
 * Returns null when the file type has no patterns, or when none of them applies, leaving the jump to the default
 * `%` motion.
 *
 * The extended pairs are tried before the plain brackets even when the cursor sits on one of the [DEFAULT_PAIRS]:
 * matchit appends 'matchpairs' after b:match_words when it builds its search pattern, so a pattern that starts on
 * the cursor beats the bracket under it, which is what makes template tags such as "{% ... %}" usable. A bracket the
 * cursor is on still wins over a pattern that merely spans it, e.g. the "[" of an attribute inside an HTML tag.
 */
private fun findExtendedPairOffset(
  editor: Editor,
  caretOffset: Int,
  isInOpPending: Boolean,
  reverse: Boolean,
  isCursorAtEnd: Boolean,
): Int? {
  val virtualFile = EditorHelper.getVirtualFile(editor)
  val patterns = FileTypePatterns.getMatchupPatterns(virtualFile, editor.vim) ?: return null
  val caretIsOnDefaultPair = editor.document.charsSequence[caretOffset] in DEFAULT_PAIRS

  val offset = if (reverse) {
    findMatchingPair(
      editor,
      caretOffset,
      isInOpPending,
      patterns.reversedOpenings,
      patterns.reversedClosings,
      caretIsOnDefaultPair,
      isCursorAtEnd,
    )
  } else {
    findMatchingPair(
      editor, caretOffset, isInOpPending, patterns.openings, patterns.closings, caretIsOnDefaultPair, isCursorAtEnd
    )
  }
  return offset.takeIf { it >= 0 }
}

private fun getMotionOffset(motion: Motion): Int? {
  return when (motion) {
    is Motion.AdjustedOffset, is Motion.AbsoluteOffset -> motion.offset
    is Motion.Error, is Motion.NoMotion -> null
  }
}

fun findMatchingPair(
  editor: Editor,
  caretOffset: Int,
  isInOpPending: Boolean,
  openings: PatternsTable,
  closings: PatternsTable,
  onlyMatchesStartingOnCaret: Boolean = false,
  isCursorAtEnd: Boolean = false,
): Int {
  val vimEditor = editor.vim
  val initialPattern =
    findInitialPattern(vimEditor, caretOffset, openings, closings, onlyMatchesStartingOnCaret) ?: return -1

  // Some elements are ignored no matter where the cursor is. In Ruby we still want to jump when the cursor is on a
  // "do" that follows an "if", but that same "do" must be ignored when the cursor is on the "if".
  val initialPsiElement = PsiHelper.getFile(editor)!!.findElementAt(initialPattern.startOffset)
  if (isGlobalSkippedElement(initialPsiElement)) return -1

  val backReference = findBackReference(vimEditor, initialPattern.pattern, initialPattern.startOffset)
  val searchParams = MatchupSearchParams(
    initialPattern.startOffset,
    initialPattern.endOffset,
    initialPattern.searchPair.first.withBackReference(backReference),
    initialPattern.searchPair.second.withBackReference(backReference),
    skipComments = !isComment(initialPsiElement),
    skipStrings = !isQuoted(initialPsiElement),
  )

  val matchingPairOffset = if (initialPattern.direction == Direction.FORWARDS) {
    val landing = when {
      isInOpPending -> Landing.END
      isCursorAtEnd -> Landing.END_OF_DELIMITER
      else -> Landing.START
    }
    findClosingPair(editor, landing, searchParams, openings.keys + closings.keys)
  } else {
    findOpeningPair(editor, searchParams)
  }

  // The cursor is on a valid pattern but there is nothing to jump to, so it stays where it is. Returning -1 would
  // instead mean that the cursor isn't on a pattern at all, and would hand the motion to the default % behaviour.
  return if (matchingPairOffset < 0) caretOffset else matchingPairOffset
}

/**
 * The pattern the cursor is on, or the closest one after it, along with the pair of patterns to search with and the
 * direction to search in.
 */
data class InitialPattern(
  val pattern: String,
  val searchPair: Pair<String, String>,
  val startOffset: Int,
  val endOffset: Int,
  val direction: Direction,
)

fun findInitialPattern(
  editor: VimEditor,
  caretOffset: Int,
  openings: PatternsTable,
  closings: PatternsTable,
  onlyMatchesStartingOnCaret: Boolean,
): InitialPattern? {
  // For better performance we limit the search to the current line, so that we don't have to scan the entire file to
  // decide whether the cursor is on a pattern at all. The original plugin behaves the same way.
  val lineStart = editor.getLineStartForOffset(caretOffset)
  val lineEnd = editor.getLineEndForOffset(caretOffset)

  val candidates = (closings + openings).entries.flatMapIndexed { patternIndex, (pattern, searchPair) ->
    val direction = if (patternIndex < closings.size) Direction.BACKWARDS else Direction.FORWARDS
    findMatches(editor, pattern, lineStart, lineEnd)
      .filter { caretOffset < it.range.endOffset }
      .filter { !onlyMatchesStartingOnCaret || it.range.startOffset == caretOffset }
      .map { InitialPattern(pattern, searchPair, it.range.startOffset, it.range.endOffset, direction) }
  }

  // If the cursor is inside several patterns, the smallest one wins.
  val enclosingCaret = candidates.filter { caretOffset >= it.startOffset }
  if (enclosingCaret.isNotEmpty()) return enclosingCaret.minBy { it.endOffset - it.startOffset }

  // A default pair between the cursor and a match is preferred over that match, so such candidates are dropped.
  val text = editor.text()
  return candidates
    .filterNot { containsDefaultPairs(text.subSequence(caretOffset, it.startOffset)) }
    .minByOrNull { it.startOffset }
}

/**
 * A match of the opening or the closing pattern of a pair. A match reported by both patterns is a middle one, e.g.
 * Ruby's "elsif", which closes what came before it and opens what follows.
 */
private data class PairMatch(
  val startOffset: Int,
  val endOffset: Int,
  val isOpening: Boolean,
  val isClosing: Boolean,
) {
  val isMiddle: Boolean get() = isOpening && isClosing
}

/**
 * Collects the matches of both patterns of a pair within `[startOffset, endOffset)`, ordered by offset.
 *
 * Vim patterns have no named groups, so instead of one combined pattern we run the two patterns separately and merge
 * their matches. One pattern may match a strictly larger piece of text than the other, e.g. Java's "else if" contains
 * "if"; only the outermost match of such an overlapping pair describes the structure, so the nested one is dropped.
 */
private fun collectPairMatches(
  editor: VimEditor,
  openingPattern: String,
  closingPattern: String,
  startOffset: Int,
  endOffset: Int,
): List<PairMatch> {
  val openings = findMatchRanges(editor, openingPattern, startOffset, endOffset)
  val closings = findMatchRanges(editor, closingPattern, startOffset, endOffset)

  return (openings + closings)
    .filter { (it in openings && !it.isNestedIn(closings)) || (it in closings && !it.isNestedIn(openings)) }
    .map { PairMatch(it.first, it.second, isOpening = it in openings, isClosing = it in closings) }
    .sortedBy { it.startOffset }
}

private fun findMatchRanges(
  editor: VimEditor,
  pattern: String,
  startOffset: Int,
  endOffset: Int,
): Set<Pair<Int, Int>> {
  return findMatches(editor, pattern, startOffset, endOffset)
    .map { it.range.startOffset to it.range.endOffset }
    .toSet()
}

private fun Pair<Int, Int>.isNestedIn(ranges: Set<Pair<Int, Int>>): Boolean {
  return ranges.any { it != this && first >= it.first && second <= it.second }
}

/**
 * Where in the matching pattern a forward jump ends.
 */
private enum class Landing {
  START,

  /** The last character, for an operator to include the whole pattern. */
  END,

  /** The last character that belongs to the pattern rather than to another delimiter, e.g. not the ">" of "</div>". */
  END_OF_DELIMITER,
}

/**
 * Finds the first closing pattern that no opening pattern has claimed yet. As we come across opening patterns we push
 * their offsets onto a stack, and pop whenever we come across a closing one, crossing that item off the search.
 */
private fun findClosingPair(
  editor: Editor,
  landing: Landing,
  searchParams: MatchupSearchParams,
  languagePatterns: Collection<String>,
): Int {
  val matches = collectPairMatches(
    editor.vim,
    searchParams.targetOpeningPattern,
    searchParams.targetClosingPattern,
    searchParams.initialPatternEnd,
    editor.document.textLength,
  )

  val unmatchedOpeningPairs: Deque<Int> = ArrayDeque()
  for (match in matches) {
    val matchOffset = if (landing == Landing.END) match.endOffset - 1 else match.startOffset
    if (matchShouldBeSkipped(editor, matchOffset, searchParams.skipComments, searchParams.skipStrings)) continue

    when {
      // A middle pattern, e.g. "elsif", may appear any number of times between a strict opening and a strict closing.
      match.isMiddle -> {
        if (unmatchedOpeningPairs.isEmpty()) return landingOffset(editor, match, landing, languagePatterns)
        unmatchedOpeningPairs.pop()
        unmatchedOpeningPairs.push(matchOffset)
      }

      match.isOpening -> unmatchedOpeningPairs.push(matchOffset)
      unmatchedOpeningPairs.isEmpty() -> return landingOffset(editor, match, landing, languagePatterns)
      else -> unmatchedOpeningPairs.pop()
    }
  }

  return -1
}

/**
 * Walks the text before the cursor with the same stack as [findClosingPair]; whatever is left unmatched on the stack
 * at the end is the opening pattern the cursor belongs to.
 */
fun findOpeningPair(editor: Editor, searchParams: MatchupSearchParams): Int {
  val matches = collectPairMatches(
    editor.vim,
    searchParams.targetOpeningPattern,
    searchParams.targetClosingPattern,
    0,
    searchParams.initialPatternStart,
  )

  val unmatchedOpeningPairs: Deque<Int> = ArrayDeque()
  for (match in matches) {
    val matchOffset = match.startOffset
    if (matchShouldBeSkipped(editor, matchOffset, searchParams.skipComments, searchParams.skipStrings)) continue

    when {
      match.isMiddle -> {
        if (unmatchedOpeningPairs.isNotEmpty()) unmatchedOpeningPairs.pop()
        unmatchedOpeningPairs.push(matchOffset)
      }

      match.isOpening -> unmatchedOpeningPairs.push(matchOffset)
      // The stack can be empty on malformed code, where a closing pattern has no opening.
      unmatchedOpeningPairs.isNotEmpty() -> unmatchedOpeningPairs.pop()
    }
  }

  return if (unmatchedOpeningPairs.isEmpty()) -1 else unmatchedOpeningPairs.pop()
}

private fun landingOffset(editor: Editor, match: PairMatch, landing: Landing, languagePatterns: Collection<String>): Int {
  return when (landing) {
    Landing.START -> match.startOffset
    Landing.END -> match.endOffset - 1
    Landing.END_OF_DELIMITER -> {
      var offset = match.endOffset - 1
      while (offset > match.startOffset && isStartOfOtherDelimiter(editor, offset, languagePatterns)) offset--
      offset
    }
  }
}

/**
 * Whether another delimiter starts at [offset], e.g. the ">" that ends "</div>".
 */
private fun isStartOfOtherDelimiter(editor: Editor, offset: Int, languagePatterns: Collection<String>): Boolean {
  if (editor.document.charsSequence[offset] in DEFAULT_PAIRS) return true
  val vimEditor = editor.vim
  val lineStart = vimEditor.getLineStartForOffset(offset)
  val lineEnd = vimEditor.getLineEndForOffset(offset)
  return languagePatterns.any { pattern ->
    findMatches(vimEditor, pattern, lineStart, lineEnd).any { it.range.startOffset == offset }
  }
}

fun containsDefaultPairs(chars: CharSequence): Boolean {
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

fun isQuoted(psiElement: PsiElement?): Boolean {
  val type = getElementType(psiElement)
  return type == "STRING_LITERAL" || type == "XML_ATTRIBUTE_VALUE_TOKEN" ||
    type == "string content" // Ruby specific.
}

/**
 * A match of a block's opening or closing pattern, covering `[startOffset, endOffset)`.
 */
internal data class Delimiter(val startOffset: Int, val endOffset: Int)

/**
 * A block from its opening to its closing delimiter, with the middle ones, such as "else", in between.
 */
internal data class Block(val opening: Delimiter, val closing: Delimiter, val middles: List<Delimiter> = emptyList()) {
  val delimiters: List<Delimiter> get() = listOf(opening) + middles + closing
}

private const val BLOCK_SEARCH_LINES = 1500

/**
 * The blocks containing [offset], innermost first. Brackets from 'matchpairs' are blocks too, so are the language's
 * opening and closing words, while middle words such as "else" are ignored.
 */
internal fun findSurroundingBlocks(editor: Editor, offset: Int): List<Block> {
  val vimEditor = editor.vim
  val skipContext = SkipContext(editor, offset)
  val searchStart = blockSearchStart(vimEditor, offset)
  val searchEnd = blockSearchEnd(vimEditor, offset)
  return blockPatterns(editor)
    .flatMap { findBlocks(vimEditor, it, searchStart, searchEnd, skipContext) }
    .filter { it.opening.startOffset <= offset && offset < it.closing.endOffset }
    .sortedWith(compareByDescending<Block> { it.opening.startOffset }.thenBy { it.closing.endOffset })
}

/**
 * The opening delimiter of any block that the cursor at [offset] is on.
 */
internal fun findOpeningDelimiterAt(editor: Editor, offset: Int): Delimiter? {
  val vimEditor = editor.vim
  val lineStart = vimEditor.getLineStartForOffset(offset)
  val lineEnd = vimEditor.getLineEndForOffset(offset)
  return blockPatterns(editor)
    .flatMap { findMatches(vimEditor, it.openingPattern, lineStart, lineEnd) }
    .filter { it.range.startOffset <= offset && offset < it.range.endOffset }
    .maxByOrNull { it.range.startOffset }
    ?.let { Delimiter(it.range.startOffset, it.range.endOffset) }
}

/**
 * The first opening delimiter of any block that starts at or after [offset]. Whether comments and strings are skipped
 * depends on where the cursor, at [caretOffset], is.
 */
internal fun findNextOpeningDelimiter(editor: Editor, offset: Int, caretOffset: Int): Delimiter? {
  val vimEditor = editor.vim
  val skipContext = SkipContext(editor, caretOffset)
  val patterns = blockPatterns(editor)
  val firstLine = vimEditor.offsetToBufferPosition(offset).line
  val lastLine = min(vimEditor.lineCount() - 1, firstLine + BLOCK_SEARCH_LINES)
  for (line in firstLine..lastLine) {
    val lineStart = max(offset, vimEditor.getLineStartOffset(line))
    val lineEnd = vimEditor.getLineEndOffset(line)
    val delimiter = patterns
      .flatMap { findMatches(vimEditor, it.openingPattern, lineStart, lineEnd) }
      .filter { it.range.startOffset >= offset && !skipContext.isSkipped(it.range.startOffset) }
      .minByOrNull { it.range.startOffset }
    if (delimiter != null) return Delimiter(delimiter.range.startOffset, delimiter.range.endOffset)
  }
  return null
}

private fun blockPatterns(editor: Editor): List<BlockPatterns> {
  val virtualFile = EditorHelper.getVirtualFile(editor)
  val languageBlocks = FileTypePatterns.getMatchupPatterns(virtualFile, editor.vim)?.blocks.orEmpty()
  return languageBlocks + matchPairBlocks(editor.vim)
}

private fun matchPairBlocks(editor: VimEditor): List<BlockPatterns> {
  return injector.options(editor).matchpairs
    .filter { it.length == 3 }
    .map { BlockPatterns(literalPattern(it[0]), literalPattern(it[2])) }
}

private fun literalPattern(char: Char): String = if (char == '\\') """\V\\""" else """\V$char"""

private fun blockSearchStart(editor: VimEditor, offset: Int): Int {
  val line = editor.offsetToBufferPosition(offset).line
  return editor.getLineStartOffset(max(0, line - BLOCK_SEARCH_LINES))
}

private fun blockSearchEnd(editor: VimEditor, offset: Int): Int {
  val line = editor.offsetToBufferPosition(offset).line
  return editor.getLineEndOffset(min(editor.lineCount() - 1, line + BLOCK_SEARCH_LINES))
}

/**
 * Pairs up the openings and closings of one kind of block with a stack. A closing that carries a name pairs with the
 * nearest opening of the same name, and the openings above it, e.g. HTML void elements such as `<br>`, are dropped.
 * A middle word belongs to the innermost opening that is still open.
 */
private fun findBlocks(
  editor: VimEditor,
  patterns: BlockPatterns,
  startOffset: Int,
  endOffset: Int,
  skipContext: SkipContext,
): List<Block> {
  val pairMatches = collectPairMatches(editor, patterns.openingPattern, patterns.closingPattern, startOffset, endOffset)
  val middleMatches = patterns.middlePattern?.let { findMatchRanges(editor, it, startOffset, endOffset) }.orEmpty()
    .map { PairMatch(it.first, it.second, isOpening = false, isClosing = false) }
  val matches = (pairMatches + middleMatches)
    .filterNot { skipContext.isSkipped(it.startOffset) }
    .sortedBy { it.startOffset }

  val openings = mutableListOf<OpenBlock>()
  val blocks = mutableListOf<Block>()
  for (match in matches) {
    val delimiter = Delimiter(match.startOffset, match.endOffset)
    if (!match.isOpening && !match.isClosing) {
      openings.lastOrNull()?.middles?.add(delimiter)
      continue
    }
    val name = if (patterns.hasBackReference) {
      val pattern = if (match.isClosing) patterns.closingPattern else patterns.openingPattern
      findBackReference(editor, pattern, match.startOffset)
    } else {
      null
    }

    if (match.isClosing) {
      val openingIndex = openings.indexOfLast { it.name == name }
      if (openingIndex >= 0) {
        val opening = openings[openingIndex]
        blocks.add(Block(opening.delimiter, delimiter, opening.middles))
        openings.subList(openingIndex, openings.size).clear()
        continue
      }
      if (!match.isOpening) continue
    }
    openings.add(OpenBlock(delimiter, name))
  }
  return blocks
}

private class OpenBlock(val delimiter: Delimiter, val name: String?) {
  val middles = mutableListOf<Delimiter>()
}

/**
 * Comments and strings are skipped unless the cursor is in one of them.
 */
private class SkipContext(editor: Editor, caretOffset: Int) {
  private val psiFile = PsiHelper.getFile(editor)
  private val skipCommentsAndStrings = !isCommentOrString(psiFile?.findElementAt(caretOffset))

  fun isSkipped(offset: Int): Boolean {
    val psiElement = psiFile?.findElementAt(offset)
    if (isSkippedRubyElement(psiElement) || isGlobalSkippedElement(psiElement)) return true
    return skipCommentsAndStrings && isCommentOrString(psiElement)
  }

  private fun isCommentOrString(psiElement: PsiElement?): Boolean = isComment(psiElement) || isQuoted(psiElement)
}
