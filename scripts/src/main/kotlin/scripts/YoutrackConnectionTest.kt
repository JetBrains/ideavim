/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import kotlinx.coroutines.runBlocking

/**
 * Simple test to verify YouTrack connection works.
 * Fetches some tickets and prints them.
 */
fun main() {
  val token = System.getenv("YOUTRACK_TOKEN")
  if (token.isNullOrBlank()) {
    System.err.println("ERROR: YOUTRACK_TOKEN environment variable is not set or empty!")
    System.exit(1)
  }

  runBlocking {
    println("Testing YouTrack connection...")
    println("YOUTRACK_TOKEN is set: true (length: ${token.length})")

    // Try to fetch tickets with "Ready To Release" tag
    val tickets = getYoutrackTicketsByQuery("%23%7BReady%20To%20Release%7D")
    println("Found ${tickets.size} tickets with 'Ready To Release' tag:")
    tickets.take(10).forEach { println("  - $it") }
    if (tickets.size > 10) {
      println("  ... and ${tickets.size - 10} more")
    }

    println("\nYouTrack connection test completed successfully!")
  }
}
