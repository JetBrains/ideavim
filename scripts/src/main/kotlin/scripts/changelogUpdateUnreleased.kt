/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun main(args: Array<String>) {
  println("Start updating unreleased section")
  val newVersion = args[0]
  val rootDir = args[1]
  println("New version: $newVersion")
  println("root dir: $rootDir")

  val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
  val newItem = "## $newVersion, $currentDate"

  val changelogPath = Path("$rootDir/CHANGES.md")
  val changelog = changelogPath.readText()
  val newChangelog = changelog.replace("## To Be Released", newItem)
  changelogPath.writeText(newChangelog)
}