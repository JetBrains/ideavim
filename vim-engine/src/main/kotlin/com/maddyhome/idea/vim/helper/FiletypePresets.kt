/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

/**
 * Per-filetype `'comments'` defaults, modelled on Vim's ftplugins.
 *
 * Keys are lowercase filetype names (matching Vim's `&filetype`). Plugin-side
 * adapters translate IntelliJ's `FileType`/`Language` id into one of these keys.
 */
object FiletypePresets {
  private val presets: Map<String, String> = mapOf(
    // C-family. Three-piece block plus `//` line and `///` doc, matching Vim's c.vim ftplugin.
    "c" to C_FAMILY,
    "cpp" to C_FAMILY,
    "java" to C_FAMILY,
    "kotlin" to C_FAMILY,
    "scala" to C_FAMILY,
    "groovy" to C_FAMILY,
    "javascript" to C_FAMILY,
    "typescript" to C_FAMILY,
    "dart" to C_FAMILY,

    // C-like but without the three-piece bullet continuation.
    "go" to "s1:/*,mb:*,ex:*/,://",
    "swift" to "s1:/*,mb:*,ex:*/,:///,://",
    "rust" to "s1:/*,mb:*,ex:*/,:///,://!,://",
    "php" to "s1:/*,mb:*,ex:*/,://,:#",
    "css" to "s1:/*,mb:*,ex:*/",
    "scss" to "s1:/*,mb:*,ex:*/,://",
    "less" to "://",

    // Shell/Python family.
    "python" to "b:#,fb:-",
    "sh" to "b:#",
    "bash" to "b:#",
    "zsh" to ":#",
    "fish" to ":#",
    "ruby" to "b:#",
    "perl" to ":#",
    "yaml" to ":#",
    "toml" to ":#",
    "dockerfile" to ":#",
    "makefile" to "sO:# -,mO:#  ,b:#",
    "cmake" to "b:#",
    "r" to ":#',:###,:##,:#",
    "julia" to ":#",
    "nix" to ":#",
    "terraform" to "://,:#",

    // SQL / Haskell / Lua family.
    "sql" to "s1:/*,mb:*,ex:*/,:--,://",
    "lua" to ":---,:--",
    "haskell" to "s1fl:{-,mb:-,ex:-},:--",
    "elm" to "s1fl:{-,mb: ,ex:-},:--",
    "ada" to "O:--,:--  ",

    // Lisp family.
    "lisp" to ":;;;;,:;;;,:;;,:;,sr:#|,mb:|,ex:|#",
    "scheme" to ":;;;;,:;;;,:;;,:;,sr:#|,mb:|,ex:|#",
    "clojure" to "n:;",
    "racket" to ":;;;;,:;;;,:;;,:;",

    // Markdown.
    "markdown" to "fb:*,fb:-,fb:+,n:>",

    // Vim script. Deliberate improvement over Vim (which inherits the C default for .vim files).
    "vim" to "sO:\"\\ -,mO:\"\\ \\ ,:\"",

    // TeX.
    "tex" to ":%",
    "latex" to ":%",

    // Erlang/Elixir.
    "erlang" to ":%%%,:%%,:%",
    "elixir" to ":#",

    // Other.
    "nim" to "exO:]#,fs1:#[,mb:*,ex:]#,:#",
    "crystal" to ":#",
  )

  fun presetFor(filetype: String): String? = presets[filetype.lowercase()]

  /** All registered presets as `(filetype, commentsValue)` pairs. */
  fun allPresets(): Map<String, String> = presets

  // Two trailing spaces after `mO:*` are intentional — Vim writes this as `mO:*\ \ `
  // in its ftplugins; the space-pair is the literal continuation.
  private const val C_FAMILY = "sO:* -,mO:*  ,exO:*/,s1:/*,mb:*,ex:*/,:///,://"
}
