package com.maddyhome.idea.vim.mark

data class VimMark(
  override val key: Char,
  override var logicalLine: Int,
  override val col: Int,
  override val filename: String,
  override val protocol: String?,
) : Mark {

  private var cleared = false

  override fun isClear(): Boolean = cleared

  override fun clear() {
    cleared = true
  }

  companion object {
    @JvmStatic
    fun create(key: Char?, logicalLine: Int?, col: Int?, filename: String?, protocol: String?): VimMark? {
      return VimMark(
          key ?: return null,
          logicalLine ?: return null,
          col ?: 0,
          filename ?: return null,
          protocol ?: ""
      )
    }
  }
}
