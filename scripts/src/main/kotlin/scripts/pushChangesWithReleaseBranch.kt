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
  val releaseType = args[1]
  println("root dir: $rootDir")
  println("releaseType: $releaseType")

  val currentBranch = withRepo(rootDir) { it.branch }
  println("Current branch is $currentBranch")


  withGit(rootDir) { git ->
    if (currentBranch != "master") {
      git.checkoutBranch("master")
      println("Check out master branch")
    }

    git.push()
      .setPushTags()
      .call()
    println("Master pushed with tags")

    if (releaseType != "patch") {
      git.checkoutBranch("release")
      println("Checked out release")

      git
        .push()
        .setForce(true)
        .setPushTags()
        .call()
      println("Pushed release branch with tags")
    } else {
      println("Do not push release branch because type of release is $releaseType")
    }

    git.checkoutBranch(currentBranch)
    println("Checked out $currentBranch branch")
  }
}