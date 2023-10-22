package com.maddyhome.idea.vim.common

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertEquals

class GraphemesTest {
  companion object {
    /** Extracts the text before the comment symbol '#'. */
    val withoutCommentRegex = Regex("""^(.*?)#.*""")
  }

  @Test
  fun `test next() against UCDs GraphemeBreakTest_txt`() {
    val testCases = parseGraphemeBreakTestCases(resource("GraphemeBreakTest.txt"))

    for ((i, testCase) in testCases.withIndex()) {
      val actualGraphemes = graphemes(testCase.string, start = 0, next = Graphemes::next)

      assertEquals(testCase.graphemes, actualGraphemes, "test case #$i")
    }
  }

  @Test
  fun `test prev() against UCDs GraphemeBreakTest_txt`() {
    val testCases = parseGraphemeBreakTestCases(resource("GraphemeBreakTest.txt"))

    for ((i, testCase) in testCases.withIndex()) {
      val actualGraphemes = graphemes(testCase.string, start = testCase.string.length, next = Graphemes::prev)

      assertEquals(testCase.graphemes.reversed(), actualGraphemes, "test case #$i")
    }
  }

  /** Breaks a string into a list of grapheme clusters using the testee class ([Graphemes]). */
  private fun graphemes(text: String, start: Int, next: (CharSequence, Int) -> Int?): List<String> {
    var boundary = start
    val graphemes = mutableListOf<String>()
    while (true) {
      val nextBoundary = next(text, boundary) ?: break

      // Since we may traverse in both directions, we should properly get the grapheme range.
      val from = min(boundary, nextBoundary)
      val to = max(boundary, nextBoundary)

      graphemes.add(text.substring(from, to))
      boundary = nextBoundary
    }
    return graphemes
  }

  private fun parseGraphemeBreakTestCases(contents: String) = contents.lines().mapNotNull { parseTestCase(it) }

  /**
   * Parses a single test case.
   *
   * The test cases are presented as a sequence of code points in the following format:
   *     ÷ 034F × 0308 ÷ 0020 ÷
   * Where the "÷" symbol represents a break (including start of text and end of text breaks)
   * and the "×" symbol means that the two adjacent code points are part of the same grapheme cluster.
   * Each code point is encoded as a hexadecimal.
   */
  private fun parseTestCase(line: String): TestCase? {
    val match = withoutCommentRegex.find(line) ?: return null
    val groups = match.groupValues
    if (groups.size != 2) return null

    val breakChar = '÷'
    val joinChar = '×'

    val testBody = groups[1].trim()
    if (testBody.isEmpty()) return null

    val composites = testBody.split(breakChar)
      .filter { it.isNotBlank() }
      .map { it.trim() }

    val compositesStrings = composites
      .map { composite ->
        composite
          .split(joinChar)
          .filter { it.isNotBlank() }.joinToString(separator = "") { codePoint ->
            String(
              Character.toChars(
                Integer.parseInt(codePoint.trim(), 16)
              )
            )
          }
      }

    return TestCase(string = compositesStrings.joinToString(separator = ""), graphemes = compositesStrings)
  }

  private fun resource(name: String): String {
    val resourceUrl = javaClass.classLoader.getResource(name)
      ?: error("resource `$name' wasn't found")

    return Files.readString(Paths.get(resourceUrl.toURI()))
  }

  private data class TestCase(
    val string: String,
    val graphemes: List<String>
  )
}