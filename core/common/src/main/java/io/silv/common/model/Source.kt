package io.silv.common.model

import androidx.compose.runtime.Stable

@Stable
data class Source(
    val id: Long,
    val lang: String,
    val name: String,
    val supportsLatest: Boolean,
    val isStub: Boolean,
    val isUsedLast: Boolean = false,
) {
    val visualName: String
        get() =
            when {
                lang.isEmpty() -> name
                else -> "$name (${lang.uppercase()})"
            }

    val key: () -> String = {
        when {
            isUsedLast -> "$id-lastused"
            else -> "$id"
        }
    }
}
