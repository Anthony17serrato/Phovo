package com.serratocreations.phovo.data.photos.network

import kotlin.jvm.JvmInline

object ApiEndpoints {

    val GET_MEDIA = Endpoint("media/")
    val LOW_RES_THUMBNAIL_API = Endpoint("low_res_thumbnails/")
    val HIGH_RES_THUMBNAIL_API = Endpoint("high_res_thumbnails/")
}

@JvmInline
value class Endpoint(val value: String)