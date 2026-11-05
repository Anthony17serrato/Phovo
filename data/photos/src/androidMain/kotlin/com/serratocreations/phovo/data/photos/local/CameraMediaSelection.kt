package com.serratocreations.phovo.data.photos.local

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

/**
 * The MediaStore `WHERE` clause that picks out camera media: what Phovo backs up by default.
 *
 * Camera media is anything in the `DCIM/` folder at the root of a storage volume, minus the
 * screenshot and screen-recording folders some OEMs put there. The rule follows Google Photos
 * (7.98), whose default "camera folder" check is also path-only, though looser: a path containing
 * `/dcim/` at any depth, case-insensitive.
 *
 * Why each part is there:
 * - **`DCIM/` on any volume.** Android reserves DCIM for camera output, and every stock camera
 *   writes there, though not always to `DCIM/Camera` (some modes and third-party camera apps use
 *   their own `DCIM/<name>` folder). The query runs on `VOLUME_EXTERNAL`, so camera media saved to
 *   an SD card is included.
 * - **Screenshot and screen-recording folders excluded.** Samsung, Xiaomi and OnePlus save these
 *   under `DCIM/` (for example `DCIM/Screenshots`), and a bare `DCIM/%` would back them up. The
 *   folder names are case-insensitive because SQLite `LIKE` is case-insensitive for ASCII.
 * - **No owner-package allowlist.** `OWNER_PACKAGE_NAME` doesn't reliably identify the camera. On
 *   a Pixel 10 Pro XL, 750 of 1,804 `DCIM/Camera` photos were owned by
 *   `com.google.android.apps.restore` (device-transfer restore), and Google Photos edits saved
 *   back to `DCIM/Camera` are owned by Photos. The column is also null for files indexed by a
 *   scan. Google Photos uses the owner package only as upload metadata, never to filter.
 *   Don't put the column in the selection at all: MediaProvider hides it from regular apps, so a
 *   clause such as `owner_package_name IS NULL OR owner_package_name != ?` matches no rows for
 *   Phovo (while `adb shell content query`, running as shell, still matches them).
 * - **Pending and trashed rows** need no clause. MediaStore leaves out `IS_PENDING` rows of
 *   other apps (API 29+) and `IS_TRASHED` rows (API 30+) unless asked for them.
 * - **Below API 29** there is no `RELATIVE_PATH`, so the rule runs on the absolute `DATA` path,
 *   anchored to each mounted volume's root (see [legacyVolumeRoots]). A bare `%/DCIM/%` would
 *   also match `DCIM` folders deeper down, such as an app's
 *   `Android/data/<pkg>/files/DCIM/`, which the pre-Q scanner indexes unless it holds a
 *   `.nomedia` file. API 29+ never selects those, because their `RELATIVE_PATH` doesn't start
 *   with `DCIM/`. Anchoring keeps both branches selecting the same folders.
 */
internal class CameraMediaSelection private constructor(
    val selection: String,
    val args: Array<String>
) {
    companion object {
        /** DCIM subfolders that OEMs use for screen captures rather than camera output. */
        private val screenCaptureFolders = listOf(
            "Screenshots",
            "Screen recordings",
            "ScreenRecorder",
            "ScreenRecords"
        )

        /**
         * The selection for this device. [volumeRoots] is only called below API 29, where it
         * gives the absolute path of every mounted volume's root, without a trailing slash.
         */
        // RELATIVE_PATH is only used on API 29+, DATA only below it.
        @SuppressLint("InlinedApi")
        @Suppress("DEPRECATION")
        fun forSdk(sdkInt: Int, volumeRoots: () -> List<String>): CameraMediaSelection =
            if (sdkInt >= Build.VERSION_CODES.Q) {
                build(pathColumn = MediaStore.MediaColumns.RELATIVE_PATH, prefixes = listOf(""))
            } else {
                build(
                    pathColumn = MediaStore.MediaColumns.DATA,
                    prefixes = volumeRoots().map { "${it.trimEnd('/')}/" }.distinct()
                )
            }

        /**
         * The root of every mounted storage volume, for matching `DATA` paths below API 29.
         * `getExternalFilesDirs` returns `<root>/Android/data/<pkg>/files` for each mounted
         * volume (null for one that isn't mounted) and works from API 19, unlike
         * `StorageVolume.getDirectory()`, which needs API 30.
         */
        @Suppress("DEPRECATION")
        fun legacyVolumeRoots(context: Context): List<String> =
            context.getExternalFilesDirs(null)
                .mapNotNull { dir ->
                    dir?.absolutePath
                        ?.substringBefore("/Android/data/", missingDelimiterValue = "")
                        ?.takeIf { it.isNotEmpty() }
                }
                .ifEmpty { listOf(Environment.getExternalStorageDirectory().absolutePath) }

        /**
         * `(path LIKE '<prefix>DCIM/%' OR …) AND path NOT LIKE '<prefix>DCIM/<capture folder>/%' …`
         * for every prefix. Prefixes are escaped so a `_` or `%` in a volume path matches
         * literally.
         */
        private fun build(pathColumn: String, prefixes: List<String>): CameraMediaSelection {
            val like = "$pathColumn LIKE ? ESCAPE '\\'"
            val notLike = "$pathColumn NOT LIKE ? ESCAPE '\\'"
            val escaped = prefixes.map(::escapeLike)
            val clauses = mutableListOf(escaped.joinToString(" OR ", "(", ")") { like })
            val args = escaped.mapTo(mutableListOf()) { "${it}DCIM/%" }
            escaped.forEach { prefix ->
                screenCaptureFolders.forEach { folder ->
                    clauses += notLike
                    args += "${prefix}DCIM/$folder/%"
                }
            }
            return CameraMediaSelection(
                selection = clauses.joinToString(separator = " AND "),
                args = args.toTypedArray()
            )
        }

        private fun escapeLike(value: String): String =
            value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
