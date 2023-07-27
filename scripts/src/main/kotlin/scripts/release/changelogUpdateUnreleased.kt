/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun main(args: Array<String>) {
  println("Start updating unreleased section")
  val (newVersion, rootDir, releaseType) = readArgs(args)

  checkReleaseType(releaseType)

  if (releaseType == "patch") {
    println("Skip updating the changelog because release type is 'patch'")
    return
  }

  val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
  val newItem = "## $newVersion, $currentDate"

  val changelogPath = Path("$rootDir/CHANGES.md")
  val changelog = changelogPath.readText()
  val newChangelog = changelog.replace("## To Be Released", newItem)
  changelogPath.writeText(newChangelog)
}