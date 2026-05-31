package com.serratocreations.phovo.core.model.network

import kotlin.jvm.JvmInline

object ApiEndpoints {
    val GET_ALL_MEDIA_API = Endpoint("all_media/")
    val LOW_RES_THUMBNAIL_API = Endpoint("low_res_thumbnails/")
    val HIGH_RES_THUMBNAIL_API = Endpoint("high_res_thumbnails/")
    val SOURCE_FILE_API = Endpoint("source_file/")

    object Upload {
        private val BASE_UPLOAD_API = Endpoint("upload/")

        val INIT_API = BASE_UPLOAD_API / Endpoint("init")
        val CHUNK_API = BASE_UPLOAD_API / Endpoint("chunk")
        val COMPLETE_API = BASE_UPLOAD_API / Endpoint("complete")
    }
}

// TODO This needs some improvement
@JvmInline
value class Endpoint(val value: String) {
    operator fun div(other: Endpoint): Endpoint {
        return Endpoint("${this.value}${other.value}")
    }
}

@JvmInline
value class BaseUrl(val value: String) {
    operator fun div(endpoint: Endpoint): String {
        return "${this.value}${endpoint.value}"
    }
}