/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import scripts.release.checkoutBranch
import scripts.release.withGit
import scripts.release.withRepo

fun main(args: Array<String>) {
  val rootDir = args[0]
  println("root dir: $rootDir")

  val currentBranch = withRepo(rootDir) { it.branch }
  println("Current branch is $currentBranch")


  withGit(rootDir) { git ->
    if (currentBranch != "master") {
      git.checkoutBranch("master")
      println("Check out master branch")
    }

    try {
      git.push()
        .setPushTags()
        .call()
    } catch (e: Throwable) {
      e.printStackTrace()
    }
    println("Master pushed with tags")

    git.checkoutBranch("release")
    println("Checked out release")

    git
      .push()
      .setForce(true)
      .setPushTags()
      .call()
    println("Pushed release branch with tags")

    git.checkoutBranch(currentBranch)
    println("Checked out $currentBranch branch")
  }
}