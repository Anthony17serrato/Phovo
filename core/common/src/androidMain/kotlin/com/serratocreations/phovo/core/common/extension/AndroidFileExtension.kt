package com.serratocreations.phovo.core.common.extension

import android.net.Uri
import io.github.vinceglb.filekit.AndroidFile

val AndroidFile.androidUri: Uri
    get() = when (this) {
        is AndroidFile.FileWrapper -> Uri.fromFile(this.file)
        is AndroidFile.UriWrapper -> this.uri
    }