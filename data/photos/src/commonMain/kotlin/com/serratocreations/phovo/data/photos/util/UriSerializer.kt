package com.serratocreations.phovo.data.photos.util

import coil3.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UriSerializer : KSerializer<Uri> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uri) {
        // Convert 'value' to a String representation and serialize it
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uri {
        // Deserialize a String and convert it back to 'Uri'
        return Uri(decoder.decodeString())
    }
}