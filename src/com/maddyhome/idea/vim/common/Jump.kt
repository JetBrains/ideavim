package com.maddyhome.idea.vim.common

data class Jump(var logicalLine: Int, val col: Int, var filepath: String)
