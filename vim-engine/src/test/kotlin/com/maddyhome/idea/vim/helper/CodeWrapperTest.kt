/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

data class WrapTestCase(
  val description: String,
  private val rawInput: String?,
  private val rawExpectedOutput: String,
  val trimIndent: Boolean = true,
  val width: Int = 80,
  val tabWidth: Int = 4,
  val visibleNewlines: Boolean = false,
  val leaders: String? = null,
) {
  val input: String? = rawInput?.let { if (trimIndent) it.trimIndent() else it }
  val expectedOutput: String = if (trimIndent) rawExpectedOutput.trimIndent() else rawExpectedOutput
}

class CodeWrapperTest {

  private val testDefaultLeaders =
    "s1:/**,s1:/*,mb:*,ex:*/,://!,://,b:#,:%,:XCOMM,n:>,fb:-,:;,:--,:."


  private val testCases = listOf(
    WrapTestCase(
      "Test trimIndent option in tests (false)",
      """            This text should not be trimmed """,
      """            This text should not be trimmed""",
      trimIndent = false
    ),
    WrapTestCase(
      "Test trimIndent option in tests (true)",
      """            This text should be trimmed """,
      """            This text should be trimmed""",
      trimIndent = true
    ),
    WrapTestCase(
      "combines two adjacent single-line comments",
      """
            // This is my text.
            // This is my text.
            """,
      """
            // This is my text. This is my text.
            """
    ),
    WrapTestCase(
      "Wraps to column width - comment",
      """
            // aa a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a
            """,
      """
            // aa a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a
            // a a a a a a a a a a a a a a a a a a a a a a
            """
    ),
    WrapTestCase(
      "Wraps to column width - C-style opening comment",
      """
            /** aa a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a aa a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a
            */
            """,
      """
            /** aa a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a aa
             * a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a
            */
            """
    ),
    WrapTestCase(
      "Wraps to column width - no comment",
      """
            aa a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a
            """,
      """
            aa a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a a
            a a a a a a a a a a a a a a a a a a a a
            """
    ),
    WrapTestCase(
      "Wraps one long line",
      """
            // This is my very long line of text. This is my very long line of text. This is my very long line of text.
            """,
      """
            // This is my very long line of text. This is my very long line of text. This is
            // my very long line of text.
            """
    ),
    WrapTestCase(
      "Wrap retains separate paragraphs",
      """
            // This is my very long line of text. This is my very long line of text. This is my very long line of text.

            // This is a second paragraph.
            """,
      """
            // This is my very long line of text. This is my very long line of text. This is
            // my very long line of text.

            // This is a second paragraph.
            """
    ),
    WrapTestCase(
      "Wrap wraps Python comments",
      """
            # This is my very long line of text. This is my very long line of text. This is my very long line of text.

            # This is a second paragraph.
            """,
      """
            # This is my very long line of text. This is my very long line of text. This is
            # my very long line of text.

            # This is a second paragraph.
            """
    ),
    WrapTestCase(
      "wraps single-line block comment opener into multiple lines with star continuations",
      """
            /** This is my text This is my long multi-line comment opener text. More text please. This is yet another bunch of text in my test comment, so I will get multiple lines in the comment.
            """,
      """
            /** This is my text This is my long multi-line comment opener text. More text
             * please. This is yet another bunch of text in my test comment, so I will get
             * multiple lines in the comment.
            """
    ),
    WrapTestCase(
      "preserves indent before block comment opener when wrapping with star continuations",
      """
              /* This is my text This is my long multi-line comment opener text. More text please. This is yet another bunch of text in my test comment, so I will get multiple lines in the comment. */
            """,
      """
              /* This is my text This is my long multi-line comment opener text. More text
               * please. This is yet another bunch of text in my test comment, so I will get
               * multiple lines in the comment. */
            """
    ),
    WrapTestCase(
      "Wrap preserves empty comment lines",
      """
            /*
             * This is my text. This is my long multi-line comment opener text. More text please. This is yet another bunch of text in my test comment, so I will get multiple lines in the comment.
             *
             * This is another line of text.
            */
            """,
      """
            /*
             * This is my text. This is my long multi-line comment opener text. More text
             * please. This is yet another bunch of text in my test comment, so I will get
             * multiple lines in the comment.
             *
             * This is another line of text.
            */
            """
    ),
    WrapTestCase(
      "Wrap multiple comment paragraphs",
      """
            /*
             * This is my text. This is my long multi-line comment opener text. More text please. This is yet another bunch of text in my test comment, so I will get multiple lines in the comment.
             *
             * This is another line of text.
             *
             * And yet another long line of text. Text going on and on endlessly, much longer than it really should.
            */
            """,
      """
            /*
             * This is my text. This is my long multi-line comment opener text. More text
             * please. This is yet another bunch of text in my test comment, so I will get
             * multiple lines in the comment.
             *
             * This is another line of text.
             *
             * And yet another long line of text. Text going on and on endlessly, much
             * longer than it really should.
            */
            """
    ),
    WrapTestCase(
      "Wrap retains space indent",
      "    This is my long indented string. It's too long to fit on one line, uh oh! What will happen?",
      "    This is my long indented string. It's too long to fit on one line, uh oh!\n    What will happen?",
      trimIndent = false
    ),
    WrapTestCase(
      "Wrap retains tab indent",
      "\tThis is my long indented string. It's too long to fit on one line, uh oh! What will happen?",
      "\tThis is my long indented string. It's too long to fit on one line, uh oh!\n\tWhat will happen?",
      trimIndent = false,
      tabWidth = 4
    ),
    WrapTestCase(
      "Wrap retains space indent on comment",
      "    // This is my long indented comment. It's too long to fit on one line, uh oh! What will happen?",
      "    // This is my long indented comment. It's too long to fit on one line, uh\n    // oh! What will happen?",
      trimIndent = false
    ),
    WrapTestCase(
      "Wrap retains tab indent on comment",
      "\t// This is my long indented comment. It's too long to fit on one line, uh oh! What will happen?",
      "\t// This is my long indented comment. It's too long to fit on one line, uh\n\t// oh! What will happen?",
      trimIndent = false,
      tabWidth = 4
    ),
    WrapTestCase(
      "Wrap handles lines within multiline comment",
      """
            * This is a long line in a multi-line comment block. Note the star at the beginning.
            * This is another line in a multi-line comment.
            """,
      """
            * This is a long line in a multi-line comment block. Note the star at the
            * beginning. This is another line in a multi-line comment.
            """,
    ),
    WrapTestCase(
      "Wrap preserves leading indent",
      """
            . My long bullet line. My long bullet line. My long bullet line. My long bullet line.
            """,
      """
            . My long bullet line. My long bullet line. My long bullet line. My long bullet
            . line.
            """
    ),
    WrapTestCase(
      "Ignores trailing spaces",
      """
            The quick brown fox
            jumps over the lazy
            dog
            """,
      """
            The quick brown fox jumps over the lazy dog
            """
    ),
    WrapTestCase(
      "Preserves comment symbols within text",
      """
            /**
             * Let's provide a javadoc comment that has a link to some method, e.g. {@link #m()}.
             */
            """,
      """
            /**
             * Let's provide a javadoc comment that has a link to some method, e.g. {@link
             * #m()}.
             */
            """
    ),
    WrapTestCase(
      "returns empty string when input is null",
      null,
      ""
    ),
    WrapTestCase(
      "returns empty string when input is empty",
      "",
      "",
      trimIndent = false
    ),
    WrapTestCase(
      "does not break a word longer than width",
      "supercalifragilisticexpialidocious",
      "supercalifragilisticexpialidocious",
      trimIndent = false,
      width = 5
    ),
    WrapTestCase(
      "returns single-line output when width is zero",
      "hello world foo bar",
      "hello world foo bar",
      trimIndent = false,
      width = 0
    ),
    WrapTestCase(
      "Accounts for tab width",
      "\t\t\t\tThis is my very long line of text. This is my very long line of text. This is my\t very long line of text.",
      "\t\t\t\tThis is my very long\n\t\t\t\tline of text. This\n\t\t\t\tis my very long line\n\t\t\t\tof text. This is my\n\t\t\t\tvery long line of\n\t\t\t\ttext.",
      trimIndent = false,
      width = 40,
      tabWidth = 5
    ),
    WrapTestCase(
      "Supports Chinese",
      """
            它是如何工作的呢？实际上，每个bundle在定义自己的服务配置都是跟目前为止你看到的是一样的。换句话说，一个bundle使用一个或者多个配置资源文件（通常是XML)来指定bundle所需要的参数和服务。然而，我们不直接在配置文件中使用 imports 命令导入它们，而是仅仅在bundle中调用一个服务容器扩展来为我们做同样的工作。一个服务容器扩展是 bundle 的作者创建的一个PHP类，它主要完成两件事情
            """,
      """
            它是如何工作的呢？实际上，每个bundle在定义自己的服务配置都是跟目前为止你看到的是一样的。换句话说，一个bundle使用一个或者多个配置资源文件（通常是XML)来指定bundle所需要的参数和服务。然而，我们不直接在配置文件中使用
            imports 命令导入它们，而是仅仅在bundle中调用一个服务容器扩展来为我们做同样的工作。一个服务容器扩展是 bundle
            的作者创建的一个PHP类，它主要完成两件事情
            """,
    ),
    WrapTestCase(
      "Wraps SQL comments",
      """
            -- This is a SQL comment. It may not be an important comment, but it's mine. My own. My precious.
            """,
      """
            -- This is a SQL comment. It may not be an important comment, but it's mine. My
            -- own. My precious.
            """
    ),
    WrapTestCase(
      "Wraps single line Python docstrings",
      """
            ''' This is a long docstring comment. It goes on and on to explain how the function works. However, I forgot to add line breaks! '''
            """,
      """
            ''' This is a long docstring comment. It goes on and on to explain how the
            function works. However, I forgot to add line breaks! '''
            """
    ),
    WrapTestCase(
      "Wraps Python docstrings that start on the opener line",
      """
            ''' This is a long docstring comment. It goes on and on to explain how the function works. However, I forgot to add line breaks!
            And the comment continues on the next line. '''
            """,
      """
            ''' This is a long docstring comment. It goes on and on to explain how the
            function works. However, I forgot to add line breaks! And the comment continues
            on the next line. '''
            """
    ),
    WrapTestCase(
      "Wraps Markdown block quotes",
      """
            > Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation
            """,
      """
            > Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor
            > incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis
            > nostrud exercitation
            """
    ),
    WrapTestCase(
      description = "Wraps Rust parent line comments",
      rawInput = """
            //! My foo module
            //!
            //! This is the documentation for my foo module. It has some pretty long lines, which I consider a feature and not a bug.
            """,
      rawExpectedOutput = """
            //! My foo module
            //!
            //! This is the documentation for my foo module. It has some pretty long lines,
            //! which I consider a feature and not a bug.
            """
    ),
    WrapTestCase(
      description = "Wrap preserves newlines",
      rawInput = "// test line 1 test line 1 test line 1 test line 1 test line 1 test line 1\n\n",
      rawExpectedOutput = "// test line 1 test line 1 test line 1 test line 1 test line\n// 1 test line 1\n\n",
      trimIndent = false,
      width = 60,
      visibleNewlines = true
    ),
    WrapTestCase(
      description = "uses custom leader list for marker detection",
      rawInput = "; this is a semicolon comment that should wrap",
      rawExpectedOutput = "; this is a semicolon\n; comment that should\n; wrap",
      trimIndent = false,
      width = 22,
      leaders = ":;",
    ),
    WrapTestCase(
      description = "marker not in leader list is treated as plain text",
      rawInput = "# hash comment line",
      rawExpectedOutput = "# hash comment line",
      trimIndent = false,
      width = 80,
      leaders = ":;",
    ),
    WrapTestCase(
      description = "BLANK_REQUIRED leader skips marker followed by non-whitespace",
      rawInput = "#include long line that wraps to multiple lines",
      rawExpectedOutput = "#include long line that wraps to\nmultiple lines",
      trimIndent = false,
      width = 40,
      leaders = "b:#",
    ),
    WrapTestCase(
      description = "NO_CONTINUATION leader omits marker on continuation lines",
      rawInput = "- bullet point item that should wrap across multiple continuation lines",
      rawExpectedOutput = "- bullet point item that\n  should wrap across\n  multiple continuation\n  lines",
      trimIndent = false,
      width = 25,
      leaders = "fb:-",
    ),
    WrapTestCase(
      description = "three-piece leaders drive Javadoc continuation prefix",
      rawInput = "/** long javadoc text that needs wrapping across multiple lines to test the star continuation",
      rawExpectedOutput = "/** long javadoc text that\n * needs wrapping across\n * multiple lines to test\n * the star continuation",
      trimIndent = false,
      width = 26,
      leaders = "s1:/**,s1:/*,mb:*,ex:*/",
    ),
    WrapTestCase(
      description = "two three-piece groups in one leader list pick the correct MIDDLE",
      rawInput = "-- long sql style block comment that needs wrapping across several lines here",
      rawExpectedOutput = "-- long sql style block\n -- comment that needs\n -- wrapping across\n -- several lines here",
      trimIndent = false,
      width = 24,
      leaders = "s1:/*,mb:*,ex:*/,s1:--,mb:--,ex:--",
    ),
    WrapTestCase(
      description = "negative offset on START shifts MIDDLE continuation to the left",
      rawInput = "  /* long text that wraps",
      rawExpectedOutput = "  /* long text that\n * wraps",
      trimIndent = false,
      width = 19,
      leaders = "s-1:/*,mb:*,ex:*/",
    ),

    // ─────────────────────────────────────────────────────────────────────
    // Neovim format_lines parity cases. Each is tagged with the Neovim
    // function it encodes:
    //   [fmt_check_par]   paragraph boundary detection (textformat.c:466)
    //   [same_leader]     two-line join predicate     (textformat.c:506)
    //   [get_leader_len]  leader matching vs comments (change.c:1919)
    //   [join_split]      end-to-end join-then-split  (textformat.c:892)
    //   [internal_format] width-aware break scan      (textformat.c:70)
    //   [continuation]    leader emitted on wrap      (change.c:1220)
    // Some of these may fail today — they document the target behaviour
    // for the planned rewrite, not the present behaviour.
    // ─────────────────────────────────────────────────────────────────────

    WrapTestCase(
      "[fmt_check_par] empty line ends paragraph",
      """
        // first paragraph line one
        // first paragraph line two

        // second paragraph
      """,
      """
        // first paragraph line one first paragraph line two

        // second paragraph
      """,
    ),
    WrapTestCase(
      "[fmt_check_par] leader-only line (no content after leader) is its own paragraph",
      """
        // first
        //
        // second
      """,
      """
        // first
        //
        // second
      """,
    ),
    WrapTestCase(
      "[fmt_check_par] line with e-flagged leader is its own paragraph",
      """
         * middle one
         * middle two
         */
      """,
      """
         * middle one middle two
         */
      """,
    ),
    WrapTestCase(
      "[fmt_check_par] empty content after leader on C block marks paragraph end",
      """
        /*
         * first chunk of text
         * still first chunk
         *
         * second chunk of text
         */
      """,
      """
        /*
         * first chunk of text still first chunk
         *
         * second chunk of text
         */
      """,
    ),

    WrapTestCase(
      "[same_leader] identical line-comment leaders join",
      """
        // part one of the sentence
        // part two of the sentence
      """,
      """
        // part one of the sentence part two of the sentence
      """,
    ),
    WrapTestCase(
      "[same_leader] different line-comment leaders (// vs #) do not join",
      """
        // slashes style comment
        # hash style comment
      """,
      """
        // slashes style comment
        # hash style comment
      """,
    ),
    WrapTestCase(
      "[same_leader] s-flag + m-flag (/* followed by *) joins",
      """
        /* opener text
         * middle text
      """,
      """
        /* opener text middle text
      """,
    ),
    WrapTestCase(
      "[same_leader] s-flag + non-m leader does not join",
      """
        /* opener text here
        // different leader line
      """,
      """
        /* opener text here
        // different leader line
      """,
    ),
    WrapTestCase(
      "[same_leader] m-flag + m-flag joins",
      """
         * middle line one
         * middle line two
      """,
      """
         * middle line one middle line two
      """,
    ),
    WrapTestCase(
      "[same_leader] m-flag (*) + e-flag (*/) does not join (string mismatch)",
      """
         * middle line
         */
      """,
      """
         * middle line
         */
      """,
    ),
    WrapTestCase(
      "[same_leader] e-flagged line never joins forward",
      """
         */
        // next comment line
      """,
      """
         */
        // next comment line
      """,
    ),
    WrapTestCase(
      "[same_leader] f-flag (first-only) + next line with no leader joins",
      """
        - bullet item content
          continuation of the bullet
      """,
      """
        - bullet item content continuation of the bullet
      """,
      leaders = "fb:-,://",
    ),
    WrapTestCase(
      "[same_leader] f-flag + next line with same leader does NOT join",
      """
        - first bullet item
        - second bullet item
      """,
      """
        - first bullet item
        - second bullet item
      """,
      leaders = "fb:-,://",
    ),
    WrapTestCase(
      "[same_leader] leaders with different surrounding whitespace still match",
      """
         //  one word
         // two word
      """,
      """
         //  one word two word
      """,
    ),

    WrapTestCase(
      "[get_leader_len] longest leader wins (/** preferred over /*)",
      """
        /** javadoc start text
         * middle text
      """,
      """
        /** javadoc start text middle text
      """,
    ),
    WrapTestCase(
      "[get_leader_len] b-flag leader rejected when not followed by whitespace",
      """
        #include <stdio.h>
        // plain line
      """,
      """
        #include <stdio.h>
        // plain line
      """,
    ),
    WrapTestCase(
      "[get_leader_len] b-flag leader accepted when followed by whitespace",
      """
        # python comment one
        # python comment two
      """,
      """
        # python comment one python comment two
      """,
    ),
    WrapTestCase(
      "[get_leader_len] leader preceded by tabs still recognised",
      "\t// tabbed comment one\n\t// tabbed comment two",
      "\t// tabbed comment one tabbed comment two",
      trimIndent = false,
    ),

    WrapTestCase(
      "[join_split] three short // lines join then re-wrap at width 40",
      """
        // aa aa aa aa aa aa aa aa
        // aa aa aa aa aa aa aa aa
        // aa aa aa aa aa aa aa aa
      """,
      """
        // aa aa aa aa aa aa aa aa aa aa aa aa
        // aa aa aa aa aa aa aa aa aa aa aa aa
      """,
      width = 40,
    ),
    WrapTestCase(
      "[join_split] s+m block comment joins then wraps with ` * ` continuation",
      """
        /* opener text that is long enough to force a wrap
         * second physical line
      """,
      """
        /* opener text that is long enough to force a
         * wrap second physical line
      """,
      width = 45,
    ),
    WrapTestCase(
      "[join_split] s+m+e block: e-line excluded from join, preserved verbatim",
      """
        /* start of comment text that is long enough
         * middle line text here
         */
      """,
      """
        /* start of comment text that is long enough
         * middle line text here
         */
      """,
      width = 50,
    ),
    WrapTestCase(
      "[join_split] mixed leaders with no blank line between split into separate paragraphs",
      """
        // part of the first leader group
        # part of the second leader group
      """,
      """
        // part of the first leader group
        # part of the second leader group
      """,
    ),
    WrapTestCase(
      "[join_split] joining strips subsequent leaders, preserves first line's leader",
      """
         *    heavily indented middle one
         *    heavily indented middle two
      """,
      """
         *    heavily indented middle one heavily indented middle two
      """,
    ),
    WrapTestCase(
      "[join_split] plain-text paragraph joins then re-wraps (no leaders)",
      """
        one two three four five
        six seven eight nine ten
      """,
      """
        one two three four
        five six seven eight
        nine ten
      """,
      width = 20,
    ),

    WrapTestCase(
      "[internal_format] break happens at last whitespace at or before textwidth",
      "one two three four five",
      "one two three four\nfive",
      trimIndent = false,
      width = 20,
    ),
    WrapTestCase(
      "[internal_format] unbreakable word longer than width stays on one line",
      "supercalifragilisticexpialidocious",
      "supercalifragilisticexpialidocious",
      trimIndent = false,
      width = 10,
    ),
    WrapTestCase(
      "[internal_format] content fitting inside textwidth is left unwrapped",
      "// short comment",
      "// short comment",
      trimIndent = false,
      width = 80,
    ),
    WrapTestCase(
      "[internal_format] break point refuses positions inside leader; long word stays with leader",
      """
        // aaaaaaaaa bbb
      """,
      """
        // aaaaaaaaa
        // bbb
      """,
      width = 10,
    ),
    WrapTestCase(
      "[internal_format] leader longer than width still wraps at first whitespace past the leader",
      "          // hello world",
      "          // hello\n          // world",
      trimIndent = false,
      width = 10,
    ),

    WrapTestCase(
      "[continuation] s1 offset aligns ` *` under `/*` on wrapped lines",
      "/* long text that must be wrapped because it exceeds the width",
      "/* long text that\n * must be wrapped\n * because it\n * exceeds the\n * width",
      trimIndent = false,
      width = 18,
      leaders = "s1:/*,mb:*,ex:*/",
    ),
    WrapTestCase(
      "[continuation] s-1 negative offset shifts continuation one column left",
      "  /* indented opener text that wraps across lines",
      "  /* indented opener\n * text that wraps\n * across lines",
      trimIndent = false,
      width = 20,
      leaders = "s-1:/*,mb:*,ex:*/",
    ),
    WrapTestCase(
      "[continuation] f-flag leader produces space-only continuation indent",
      "- bullet item that wraps across several lines of text here",
      "- bullet item that wraps\n  across several lines of\n  text here",
      trimIndent = false,
      width = 25,
      leaders = "fb:-",
    ),
    WrapTestCase(
      "[continuation] line-comment leader repeated verbatim on wrapped lines",
      "// aaa bbb ccc ddd eee fff ggg hhh iii jjj",
      "// aaa bbb ccc ddd eee\n// fff ggg hhh iii jjj",
      trimIndent = false,
      width = 22,
    ),
    WrapTestCase(
      "[continuation] three-piece with two distinct groups picks matching MIDDLE (parity)",
      "-- sql style block comment that needs wrapping across several lines here",
      "-- sql style block\n -- comment that needs\n -- wrapping across\n -- several lines here",
      trimIndent = false,
      width = 22,
      leaders = "s1:/*,mb:*,ex:*/,s1:--,mb:--,ex:--",
    ),
  )

  @TestFactory
  fun generateTests(): List<DynamicTest> {
    return testCases.map { testCase ->
      DynamicTest.dynamicTest(testCase.description) {
        val parsedLeaders = CommentLeaderParser.parse(testCase.leaders ?: testDefaultLeaders)
        val wrapper = CodeWrapper(
          tabWidth = testCase.tabWidth,
          width = testCase.width,
          leaders = parsedLeaders,
        )
        val result = wrapper.wrap(testCase.input)
        val expected = testCase.expectedOutput.maybeVisible(testCase.visibleNewlines)
        val actual = result.maybeVisible(testCase.visibleNewlines)
        assertEquals(expected, actual, testCase.description)
      }
    }
  }

  private fun String.maybeVisible(visible: Boolean): String =
    if (visible) replace("\n", "\\n") else this
}
