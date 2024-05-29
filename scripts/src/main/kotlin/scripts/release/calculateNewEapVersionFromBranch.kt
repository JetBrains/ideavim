/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

fun main(args: Array<String> ) {
  val projectDir = args[0]
  println("Working directory: $projectDir")
  val branch = withRepo(projectDir) { it.branch }
  val (majorBranchVersion, minorBranchVersion) = versions(branch)
  val versions = getVersionsExistingVersionsFor(majorBranchVersion, minorBranchVersion, projectDir)
  val maxExistingVersion = versions.keys.maxOrNull()

  val nextVersion = if (maxExistingVersion != null) {
    if (maxExistingVersion.suffixTokens.isEmpty()) {
      maxExistingVersion.nextPatch().withSuffix("eap.1").value
    }
    else {
      check(maxExistingVersion.suffixTokens.size == 2) {
        "We should have exactly two suffix tokens. Current tokens: ${maxExistingVersion.suffixTokens.toList()}"
      }
      check(maxExistingVersion.suffixTokens[0] == "eap") {
        "First suffix token must be eap. Current tokens: ${maxExistingVersion.suffixTokens.toList()}"
      }

      val newEapNumber = maxExistingVersion.suffixTokens[1].toInt().inc()
      maxExistingVersion.withSuffix("eap.$newEapNumber").value
    }
  } else {
    "$majorBranchVersion.$minorBranchVersion.0-eap.1"
  }


  println("Next eap version: $nextVersion")
  println("##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_version' value='$nextVersion']")
}

private val regex = "releases/(\\d+)\\.(\\d+)\\.x".toRegex()
private fun versions(branchName: String): Pair<Int, Int> {
  val match = regex.matchEntire(branchName) ?: error("Cannot match branch: $branchName")
  val major = match.groups[1]
  val minor = match.groups[2]
  return major!!.value.toInt() to minor!!.value.toInt()
}