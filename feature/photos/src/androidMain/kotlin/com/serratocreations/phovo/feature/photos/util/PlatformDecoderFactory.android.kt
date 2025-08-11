package com.serratocreations.phovo.feature.photos.util

import coil3.decode.Decoder
import coil3.video.VideoFrameDecoder

actual fun getPlatformDecoderFactory(): Decoder.Factory = VideoFrameDecoder.Factory()