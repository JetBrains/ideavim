/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package _Self

import _Self.buildTypes.LargeFileTests
import jetbrains.buildServer.configs.kotlin.v2019_2.CustomChart
import jetbrains.buildServer.configs.kotlin.v2019_2.CustomChart.Serie
import jetbrains.buildServer.configs.kotlin.v2019_2.CustomChart.SeriesKey
import jetbrains.buildServer.configs.kotlin.v2019_2.ProjectFeatures
import jetbrains.buildServer.configs.kotlin.v2019_2.projectCustomChart

/**
 * Trend charts for the timings that the large file tests publish (see IdeaVimLargeFileTestBase.publishMetric), shown in
 * the project's Statistics tab
 */
fun ProjectFeatures.largeFileCharts() {
  largeFileChart("GotoDeclaration", "Large files: Go to Declaration with other large files visible") {
    listOf(0, 1, 3).map { serie("$it other large editors", "gotoDeclaration.otherLargeEditors$it") }
  }
  largeFileChart("SearchLarge", "Large files: search in a 200k-line file") {
    listOf(
      serie("/ in 1k lines (baseline)", "search.nohlsearch.log1k.search.caretMs"),
      serie("/", "search.nohlsearch.log200k.search.caretMs"),
      serie("n", "search.nohlsearch.log200k.next.caretMs"),
      serie("*", "search.nohlsearch.log200k.star.caretMs"),
      serie("/ count shown", "search.nohlsearch.log200k.search.countMs"),
      serie("n count shown", "search.nohlsearch.log200k.next.countMs"),
      serie("* count shown", "search.nohlsearch.log200k.star.countMs"),
    )
  }
  largeFileChart("SearchNextToLarge", "Large files: search in a small file next to a 200k-line one") {
    listOf(
      serie("/ next to 1k lines (baseline)", "search.nohlsearch.smallNextTo.log1k.search.caretMs"),
      serie("/", "search.nohlsearch.smallNextTo.log200k.search.caretMs"),
      serie("n", "search.nohlsearch.smallNextTo.log200k.next.caretMs"),
      serie("*", "search.nohlsearch.smallNextTo.log200k.star.caretMs"),
    )
  }
  largeFileChart("Freezes", "Large files: UI freezes", CustomChart.Format.INTEGER) {
    listOf(
      serie("Search tests", "SearchLargeFileTest.freezes"),
      serie("Go to Declaration tests", "GotoDeclarationVisibleEditorsTest.freezes"),
    )
  }
}

private fun ProjectFeatures.largeFileChart(
  id: String,
  title: String,
  format: CustomChart.Format = CustomChart.Format.DURATION,
  series: () -> List<Serie>,
) {
  projectCustomChart {
    this.id = "IdeaVimLargeFileChart_$id"
    this.title = title
    this.seriesTitle = "Serie"
    this.format = format
    this.series = series()
  }
}

private fun serie(title: String, metric: String) =
  Serie(title, SeriesKey("ideavim.largeFile.$metric"), sourceBuildTypeId = LargeFileTests.id?.value)
