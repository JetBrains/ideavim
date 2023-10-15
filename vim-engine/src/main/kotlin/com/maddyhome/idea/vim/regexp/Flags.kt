/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.regexp

@Deprecated("Remove once old regex engine is removed")
public class Flags {
  private var flags: Int

  public constructor() {
    flags = 0
  }

  public constructor(flags: Int) {
    this.flags = flags
  }

  public fun get(): Int {
    return flags
  }

  public fun isSet(flag: Int): Boolean {
    return flags and flag != 0
  }

  public fun allSet(flags: Int): Boolean {
    return this.flags and flags == flags
  }

  public fun init(flags: Int): Int {
    this.flags = flags
    return this.flags
  }

  public fun set(flags: Int): Int {
    this.flags = this.flags or flags
    return this.flags
  }

  public fun unset(flags: Int): Int {
    this.flags = this.flags and flags.inv()
    return this.flags
  }
}
