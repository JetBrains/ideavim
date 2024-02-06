/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.releaseEap

import kotlinx.coroutines.runBlocking
import scripts.addComment
import scripts.getYoutrackTicketsByQuery
import scripts.release.readArgs
import scripts.releasedInEapTagId
import scripts.setTag

fun main(args: Array<String>) {
  runBlocking {
    val (newVersion, _, _) = readArgs(args)

    // Search for Ready to release, but without "IdeaVim Released In EAP" tag
    val ticketsToUpdate =
      getYoutrackTicketsByQuery("%23%7BReady%20To%20Release%7D%20tag:%20-%7BIdeaVim%20Released%20In%20EAP%7D%20")
    println("Have to update the following tickets: $ticketsToUpdate")

    ticketsToUpdate.forEach { ticketId ->
      setTag(ticketId, releasedInEapTagId)
      addComment(
        ticketId, """
        The fix is available in the IdeaVim $newVersion. See https://jb.gg/ideavim-eap for the instructions on how to get EAP builds as updates within the IDE. You can also wait till the next stable release with this fix, you’ll get it automatically.
      """.trimIndent()
      )
    }
  }
}
