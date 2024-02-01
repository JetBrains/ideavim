/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

import com.vdurmont.semver4j.Semver
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val toBeReleased = "## To Be Released"

fun main(args: Array<String>) {
  println("Start updating unreleased section")
  val (newVersion, rootDir, releaseType) = readArgs(args)

  checkReleaseType(releaseType)

  val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
  val newItem = "## $newVersion, $currentDate"

  val changelogPath = Path("$rootDir/CHANGES.md")
  val changelog = changelogPath.readText()
  val newChangelog = if (releaseType == "patch") {
    val decreasedVersion = Semver(newVersion).withIncPatch(-1)
    val firstEntry = changelog.indexOf("## $decreasedVersion")
    if (firstEntry != -1) {
      val newLog = StringBuilder(changelog)
      newLog.insert(firstEntry, newItem + "\n")
      newLog.toString()
    } else {
      changelog
    }
  } else {
    if (toBeReleased in changelog) {
      changelog.replace(toBeReleased, newItem)
    } else {
      val firstEntry = changelog.indexOf("##")
      val newLog = StringBuilder(changelog)
      newLog.insert(firstEntry, newItem + "\n")
      newLog.toString()
    }
  }
  changelogPath.writeText(newChangelog)
}