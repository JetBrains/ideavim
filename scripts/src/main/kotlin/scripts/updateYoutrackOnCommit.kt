/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) {
  val projectDir = if (args.isNotEmpty()) File(args[0]) else File(".")
  
  println("Start updating youtrack")
  println("Project directory: $projectDir")

  val newFixes = changes(projectDir)
  val newTickets = newFixes.map { it.id }
  println("Set new status for $newTickets")
  
  runBlocking {
    setYoutrackStatus(newTickets, "Ready To Release")
  }
}


