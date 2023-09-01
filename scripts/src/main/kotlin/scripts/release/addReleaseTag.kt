/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

fun main(args: Array<String>) {
  val (newVersion, rootDir, _) = readArgs(args)

  withGit(rootDir) { git ->
    git
      .tag()
      .setName(newVersion)
      .call()

    val tagFound = git.tagList().call()
      .map { it.name }
      .find { it == "refs/tags/$newVersion" }
      .isNullOrBlank()
      .not()
    println("Tag added. New tag found: '$tagFound'")
  }
}
