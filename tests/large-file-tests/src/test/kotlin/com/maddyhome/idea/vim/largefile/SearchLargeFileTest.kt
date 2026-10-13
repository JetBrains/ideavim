/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.largefile

import com.maddyhome.idea.vim.largefile.SearchLargeFileTest.Companion.ITERATIONS
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.name

private typealias Record = (op: String, timing: IdeaVimLargeFileTestBase.SearchTiming) -> Unit

/**
 * Measures searching (`/`, `n`, `*`) in large files, and in a small file while a large one is visible (VIM-4341).
 *
 * IdeaVim updates search highlights and the search count in every visible editor, not just the one being searched.
 * A large file in another split - or a big console such as the Unreal Engine log in Rider - can make searching in a
 * small file slow.
 *
 * Each pattern is typed before the clock starts, so only the key that runs the search is measured. Each search reports
 * the time until the caret moved and, as "count", the time until the search count was shown.
 */
class SearchLargeFileTest : IdeaVimLargeFileTestBase() {

  private val results = linkedMapOf<String, Timings>()

  override fun createProjectFiles() {
    createFile(SMALL, (0 until SMALL_LINES).joinToString("\n", postfix = "\n") { i ->
      if (i in SMALL_MATCHES) "line $i haystack" else "line $i of plain text"
    })
    LOG_SIZES.forEach { lines ->
      createFile(logPath(lines), (0 until lines).joinToString("\n", postfix = "\n") { i ->
        if (i in needleLines(lines)) "[$i] needle marker" else "[$i] LogTemp: Display: message payload lorem ipsum"
      })
    }
  }

  @Test
  fun `search in large files`() {
    for (hlsearch in listOf(false, true)) {
      LOG_SIZES.forEach { lines ->
        openFile(logPath(lines))
        setHlsearch(hlsearch)
        measure("${hlsearchName(hlsearch)} | in ${logName(lines)}") { record -> searchInLog(lines, record) }
      }
    }
  }

  @Test
  fun `search in a small file next to a large file`() {
    for (hlsearch in listOf(false, true)) {
      openFile(SMALL)
      setHlsearch(hlsearch)
      typeVim(":vsplit\n")
      var shownLog: String? = null
      LOG_SIZES.forEach { lines ->
        shownLog = showInOtherSplit(logPath(lines), shownLog)
        measure("${hlsearchName(hlsearch)} | $SMALL_NAME next to ${logName(lines)}") { record -> searchInSmall(record) }
      }
      typeVim(":only\n")
    }
  }

  @AfterAll
  fun printReport() {
    println()
    println("Search timings (ms, min/median/max)")
    results.forEach { (scenario, timings) -> println("%-48s %s".format(scenario, timings)) }
  }

  /** Runs [searches] once to warm up, then [ITERATIONS] times recording the timings under [scenario] */
  private fun measure(scenario: String, searches: (record: Record) -> Unit) {
    repeat(WARMUP) { searches { _, _ -> } }
    val timings = results.getOrPut(scenario) { Timings() }
    repeat(ITERATIONS) { searches(timings::add) }
  }

  private fun searchInLog(lines: Int, record: Record) {
    val (first, second, third) = needleLines(lines)
    moveTo(0)
    typeVim("/needle")
    record("/needle", measureSearch(first, "[1/3]") { enter() })
    record("n", measureSearch(second, "[2/3]") { typeText("n") })
    record("*", measureSearch(third, "[3/3]") { typeText("*") })

    typeVim("5G$")
    typeVim("/LogTemp")
    record("/LogTemp", measureSearch(5, "[6/${logTempTotal(lines)}]") { enter() })
  }

  private fun searchInSmall(record: Record) {
    val (first, second, third) = SMALL_MATCHES
    moveTo(0)
    typeVim("/haystack")
    record("/haystack", measureSearch(first, "[1/3]") { enter() })
    record("n", measureSearch(second, "[2/3]") { typeText("n") })
    record("*", measureSearch(third, "[3/3]") { typeText("*") })
  }

  /** Opens [logPath] in the split that shows [shownLog], then goes back to the small file. Returns the log's name */
  private fun showInOtherSplit(logPath: String, shownLog: String?): String {
    shownLog?.let { focusEditor(it) }
    openFile(logPath)
    focusEditor(SMALL_NAME)
    return Path(logPath).name
  }

  private fun setHlsearch(enabled: Boolean) {
    typeVim(if (enabled) ":set hlsearch\n" else ":set nohlsearch\n")
  }

  private fun moveTo(line: Int) {
    typeVim("${line + 1}G")
    check(waitUntil(pollMs = 20) { selectedEditorState()?.caretLine == line }) { "Caret did not move to line $line" }
  }

  private class Timings {
    private val samples = linkedMapOf<String, MutableList<Long>>()

    fun add(op: String, timing: SearchTiming) {
      samples.getOrPut(op) { mutableListOf() } += timing.caretMs
      timing.countMs?.let { samples.getOrPut("$op count") { mutableListOf() } += it }
    }

    override fun toString(): String =
      samples.entries.joinToString("  ") { (op, values) -> "%-16s %-16s".format(op, minMedianMax(values)) }

    private fun minMedianMax(values: List<Long>): String {
      val sorted = values.sorted()
      return "${sorted.first()}/${sorted[sorted.size / 2]}/${sorted.last()}"
    }
  }

  companion object {
    private const val SMALL_NAME = "small.txt"
    private const val SMALL = "src/$SMALL_NAME"
    private const val SMALL_LINES = 50
    private val SMALL_MATCHES = listOf(10, 20, 30)

    private val LOG_SIZES = listOf(1_000, 10_000, 50_000, 200_000)

    private const val WARMUP = 1
    private const val ITERATIONS = 2

    private fun logName(lines: Int) = "log${lines / 1000}k.txt"
    private fun logPath(lines: Int) = "src/${logName(lines)}"
    private fun needleLines(lines: Int) = listOf(lines / 4, lines / 2, lines * 3 / 4)
    private fun hlsearchName(hlsearch: Boolean) = if (hlsearch) "hlsearch" else "nohlsearch"

    // Every line but the needles matches, so in large files the count stops at 'maxsearchcount'
    private fun logTempTotal(lines: Int): String {
      val total = lines - 3
      return if (total > 999) ">999" else total.toString()
    }
  }
}
