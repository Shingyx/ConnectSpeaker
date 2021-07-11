package com.github.shingyx.connectspeaker.data

import androidx.annotation.StringRes

class ExceptionWithStringRes(
    message: String,
    @StringRes val stringResId: Int,
) : Exception(message)
