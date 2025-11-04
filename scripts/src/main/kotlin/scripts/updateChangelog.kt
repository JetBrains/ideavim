/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import java.io.File

fun main(args: Array<String>) {
  val projectDir = if (args.isNotEmpty()) File(args[0]) else File(".")
  
  updateChangelog(projectDir)
}

fun updateChangelog(projectDir: File) {
  println("Start update changelog")
  println(projectDir)
  val newFixes = changes(projectDir)

  // Update changes file
  val changesFile = File(projectDir, "CHANGES.md")
  val changes = changesFile.readText()

  val changesBuilder = StringBuilder(changes)
  val insertOffset = setupSection(changes, changesBuilder, "### Fixes:")

  if (insertOffset < 50) error("Incorrect offset: $insertOffset")

  val firstPartOfChanges = changes.take(insertOffset)
  val actualFixes = newFixes
    .filterNot { it.id in firstPartOfChanges }
  val newUpdates = actualFixes
    .joinToString("") { "* [${it.id}](https://youtrack.jetbrains.com/issue/${it.id}) ${it.text}\n" }

  changesBuilder.insert(insertOffset, newUpdates)
  if (actualFixes.isNotEmpty()) {
    changesFile.writeText(changesBuilder.toString())
  }
}


