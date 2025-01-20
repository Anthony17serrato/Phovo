package com.serratocreations.phovo.data.photos.network

import com.serratocreations.phovo.data.photos.db.entity.PhovoImageItem
import com.serratocreations.phovo.data.photos.network.model.getNetworkFile
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import org.koin.core.annotation.Singleton

@Singleton
class PhotosNetworkDataSource(private val client: HttpClient) {
    suspend fun syncImage(imageItem: PhovoImageItem) {
//        val response: HttpResponse = client.post("http://10.0.0.71:8080/upload") {
//            contentType(ContentType.Application.Json)
//            setBody(imageItem)
//        }
//        println(response.status)
        println("syncImage $imageItem")
        val file = getNetworkFile(imageItem.uri, imageItem.name)

        if (!file.exists()) {
            println("File not found at ${imageItem.uri}")
            return
        }

        try {
            val bytes = file.readBytes() ?: throw UnsupportedOperationException("Could not read bytes for $file")
            val fileName = file.fileName()
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "http://10.0.0.71:8080/upload",
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${fileName}")
                    })
                }
            )
            println("Response: ${response.status}")
        } catch (e: UnsupportedOperationException) {
            println("Failed to upload file: ${e.message}")
        }
    }
}