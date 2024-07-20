package com.github.shingyx.connectspeaker.data

import androidx.annotation.StringRes
import com.github.shingyx.connectspeaker.R

enum class ConnectAction(
    @StringRes val actionStringResId: Int,
) {
    TOGGLE(R.string.connect_action_toggle),
    CONNECT(R.string.connect_action_connect),
    DISCONNECT(R.string.connect_action_disconnect),
}
