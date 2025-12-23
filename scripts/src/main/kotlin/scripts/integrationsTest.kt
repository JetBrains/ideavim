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
  val rootDir = if (args.isNotEmpty()) java.io.File(args[0]) else java.io.File(".").canonicalFile
  runIntegrationsTest(rootDir)
}

fun runIntegrationsTest(projectDir: java.io.File = java.io.File(".").canonicalFile): Unit = runBlocking {
  val testTicketId = "VIM-2784"

  // YouTrack set to Ready To Release on Fix commit
  setYoutrackStatus(listOf(testTicketId), "Ready To Release")
  if ("Ready To Release" != getYoutrackStatus(testTicketId)) {
    error("Ticket status was not updated")
  }
  setYoutrackStatus(listOf(testTicketId), "Open")

  // Check YouTrack requests
  val prevStatus = getYoutrackStatus(testTicketId)
  setYoutrackStatus(listOf(testTicketId), "Ready To Release")
  val tickets = getYoutrackTicketsByQuery("%23%7BReady+To+Release%7D")
  if (testTicketId !in tickets) {
    error("Test ticket is not found in request")
  }
  setYoutrackStatus(listOf(testTicketId), prevStatus)

  // Check adding and removing release
  val existingVersionId = getVersionIdByName("TEST_VERSION")
  if (existingVersionId != null) {
    deleteVersionById(existingVersionId)
  }
  val versionId = addReleaseToYoutrack("TEST_VERSION")
  guard(getVersionIdByName("TEST_VERSION") != null) { "Test version isn't created" }
  setYoutrackStatus(listOf(testTicketId), "Fixed")
  setYoutrackFixVersion(listOf(testTicketId), "TEST_VERSION")
  deleteVersionById(versionId)
  setYoutrackStatus(listOf(testTicketId), "Open")
  guard(getVersionIdByName("TEST_VERSION") == null) { "Test version isn't deleted" }

  // TODO: test Ticket parsing
  // TODO: test Update CHANGES
  // TODO: test Update AUTHORS
  // TODO: test Slack notification
  // TODO: Add a comment on EAP release
}

fun guard(check: Boolean, ifWrong: () -> String) {
  if (!check) {
    error(ifWrong())
  }
}
