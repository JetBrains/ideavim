/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
  val version = args.getOrNull(0) ?: error("Version not provided")
  val releaseType = args.getOrNull(1)
  
  performReleaseActions(version, releaseType)
}

fun performReleaseActions(version: String, releaseType: String?) = runBlocking {
  if (releaseType == "patch") {
    println("Skipping release actions for patch release")
    return@runBlocking
  }

  val tickets = getYoutrackTicketsByQuery("%23%7BReady+To+Release%7D%20and%20tag:%20%7BIdeaVim%20Released%20In%20EAP%7D%20")
  
  if (tickets.isNotEmpty()) {
    println("Updating statuses for tickets: $tickets")
    setYoutrackStatus(tickets, "Fixed")
    
    println("Checking if version $version exists...")
    val versionId = getVersionIdByName(version)
    
    if (versionId == null) {
      addReleaseToYoutrack(version)
    } else {
      println("Version $version already exists in YouTrack. Version id: $versionId")
    }
    
    setYoutrackFixVersion(tickets, version)
  } else {
    println("No tickets to update statuses")
  }
}
