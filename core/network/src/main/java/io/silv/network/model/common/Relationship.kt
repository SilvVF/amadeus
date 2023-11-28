package io.silv.network.model.common

import android.os.Parcelable
import io.silv.common.model.Related
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class Relationship(
    val id : String,
    val type: String,
    /**
     * Related Manga type, only present if you
     * are on a Manga entity and a Manga relationship
     */
    val related: Related? = null,
    /**
     *
    If Reference Expansion is applied, contains objects attributes
     */
    val attributes: Attribute? = null
): Parcelable {

    @Parcelize
    @Serializable
    data class Attribute(
        val fileName: String? = null,
        val name	:	String?	 = null,
        val username: String? = null,
        val imageUrl: String?  = null,
        val biography: Map<String?, String?>? = null,
        val twitter: String?	=	null,
        val pixiv	: String?=	null,
        val melonBook: String? = null,
        val fanBox: String?	=	null,
        val booth: String?	=	null,
        val nicoVideo: String?	=	null,
        val skeb	: String?=	null,
        val fantia: String?	=	null,
        val tumblr: String?=	null,
        val youtube: String?=null,
        val weibo: String?	=null,
        val naver: String?	=	null,
        val website: String?	=null,
        val createdAt: String?	= null,
        val updatedAt: String?	= null,
        val version	: String?	= null
    ): Parcelable
}
