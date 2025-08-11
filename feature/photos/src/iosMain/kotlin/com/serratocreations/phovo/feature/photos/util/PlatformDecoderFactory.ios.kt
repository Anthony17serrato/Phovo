package com.serratocreations.phovo.feature.photos.util

import coil3.decode.Decoder

actual fun getPlatformDecoderFactory(): Decoder.Factory = DefaultPlatformDecoderFactory()