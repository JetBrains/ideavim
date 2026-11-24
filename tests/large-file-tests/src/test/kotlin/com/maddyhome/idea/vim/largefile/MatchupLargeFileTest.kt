/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.largefile

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.name

/**
 * Measures moving around large files with matchup's match highlighting, compared to the same moves without matchup.
 * `%` is measured with matchit too, which unlike matchup looks for the match in the whole file.
 *
 * Every line of the files is a word of a block, so every move of the cursor looks for the block it's on. The
 * highlighting runs in the background and shouldn't make moving slower, nor freeze the UI.
 */
class MatchupLargeFileTest : IdeaVimLargeFileTestBase() {

  private val results = linkedMapOf<String, MutableMap<String, MutableList<Long>>>()

  override fun ideaVimRcContent(): String = "set messagesopt=hit-enter,history:500,wait:0"

  override fun createProjectFiles() {
    SIZES.forEach { lines ->
      createFile(rubyPath(lines), (0 until lines / 3).joinToString("\n", postfix = "\n") { "  if x$it\n  else\n  end" })
      createFile(
        htmlPath(lines),
        (0 until lines / 3).joinToString("\n", postfix = "\n") { "<div id=\"$it\">\n<p>text $it</p>\n</div>" },
      )
    }
  }

  @Test
  fun `moving in large files`() {
    for (setup in SETUPS) {
      SIZES.forEach { lines ->
        listOf(rubyPath(lines), htmlPath(lines)).forEach { path ->
          openFile(path)
          setup.commands.forEach { typeVim(":$it\n") }
          measure("${setup.name} | ${Path(path).name}", "matchup.${setup.key}.${metricFile(path)}") {
            movesIn(lines, isHtml = path.endsWith(".html"), withPercent = setup.key != "off")
          }
        }
      }
    }
  }

  @AfterAll
  fun printReport() {
    println()
    println("Matchup timings (ms, min/median/max)")
    results.forEach { (scenario, timings) ->
      val ops = timings.entries.joinToString("  ") { (op, samples) -> "%-8s %-16s".format(op, minMedianMax(samples)) }
      println("%-40s %s".format(scenario, ops))
    }
  }

  private fun measure(scenario: String, metric: String, moves: () -> Map<String, Long>) {
    repeat(WARMUP) { moves() }
    val timings = results.getOrPut(scenario) { linkedMapOf() }
    repeat(ITERATIONS) { moves().forEach { (op, ms) -> timings.getOrPut(op) { mutableListOf() } += ms } }
    timings.forEach { (op, samples) -> publishMetric("$metric.${OP_KEYS.getValue(op)}", median(samples)) }
  }

  /**
   * From the opening word of a block in the middle of the file. Without matchup, `%` doesn't jump between words, so
   * it isn't measured then.
   */
  private fun movesIn(lines: Int, isHtml: Boolean, withPercent: Boolean): Map<String, Long> {
    val fileName = checkNotNull(selectedEditorState()).fileName
    val start = lines / 2 - (lines / 2) % 3
    typeVim(if (isHtml) "${start + 1}G^l" else "${start + 1}G^")
    check(waitUntil(pollMs = 20) { selectedEditorState()?.caretLine == start }) { "Caret did not move to $start" }

    val j = measureNavigation(EditorState(fileName, start + MOVES)) { typeText("j".repeat(MOVES)) }
    val k = measureNavigation(EditorState(fileName, start)) { typeText("k".repeat(MOVES)) }
    if (!withPercent) return linkedMapOf("${MOVES}j" to j, "${MOVES}k" to k)
    // From "if" to "else", and from an opening tag to its closing one
    val percent = measureNavigation(EditorState(fileName, start + if (isHtml) 2 else 1)) { typeText("%") }
    return linkedMapOf("${MOVES}j" to j, "${MOVES}k" to k, "%" to percent)
  }

  private fun minMedianMax(values: List<Long>): String = "${values.min()}/${median(values)}/${values.max()}"

  private fun median(values: List<Long>): Long = values.sorted()[values.size / 2]

  private class Setup(val name: String, val key: String, val commands: List<String>)

  companion object {
    private val SIZES = listOf(10_002, 200_001)
    private const val MOVES = 30
    private const val WARMUP = 1
    private const val ITERATIONS = 3

    private val SETUPS = listOf(
      Setup("no matchup", "off", listOf("set nomatchup")),
      Setup("matchit", "matchit", listOf("set matchit")),
      Setup("matchup", "on", listOf("set nomatchit", "set matchup")),
      Setup("matchup, surround always", "surround", listOf("let g:matchup_matchparen_hi_surround_always = 1")),
    )

    private val OP_KEYS = mapOf("${MOVES}j" to "down", "${MOVES}k" to "up", "%" to "percent")

    private fun rubyPath(lines: Int) = "src/blocks${lines / 1000}k.rb"
    private fun htmlPath(lines: Int) = "src/blocks${lines / 1000}k.html"
    private fun metricFile(path: String) = Path(path).name.replace('.', '_')
  }
}
