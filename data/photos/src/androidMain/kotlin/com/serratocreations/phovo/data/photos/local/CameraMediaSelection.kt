package com.serratocreations.phovo.data.photos.local

import android.annotation.SuppressLint
import android.os.Build
import android.provider.MediaStore

/**
 * The MediaStore `WHERE` clause that picks out camera media: what Phovo backs up by default.
 *
 * Camera media is anything in a `DCIM/` folder on any storage volume, minus the screenshot and
 * screen-recording folders some OEMs put there. The rule follows Google Photos (7.98), whose
 * default "camera folder" check is also path-only: a path containing `/dcim/`, case-insensitive,
 * on any volume.
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
 * - **Below API 29** there is no `RELATIVE_PATH`, so the same folder rule
 *   runs on the absolute `DATA` path, where `/DCIM/` can sit on any volume root.
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

        // RELATIVE_PATH is only used on API 29+, DATA only below it.
        @SuppressLint("InlinedApi")
        @Suppress("DEPRECATION")
        fun forSdk(sdkInt: Int): CameraMediaSelection =
            if (sdkInt >= Build.VERSION_CODES.Q) {
                build(
                    pathColumn = MediaStore.MediaColumns.RELATIVE_PATH,
                    dcimPattern = "DCIM/%",
                    screenCapturePattern = { "DCIM/$it/%" }
                )
            } else {
                build(
                    pathColumn = MediaStore.MediaColumns.DATA,
                    dcimPattern = "%/DCIM/%",
                    screenCapturePattern = { "%/DCIM/$it/%" }
                )
            }

        private fun build(
            pathColumn: String,
            dcimPattern: String,
            screenCapturePattern: (String) -> String
        ): CameraMediaSelection {
            val clauses = mutableListOf("$pathColumn LIKE ?")
            val args = mutableListOf(dcimPattern)
            screenCaptureFolders.forEach { folder ->
                clauses += "$pathColumn NOT LIKE ?"
                args += screenCapturePattern(folder)
            }
            return CameraMediaSelection(
                selection = clauses.joinToString(separator = " AND "),
                args = args.toTypedArray()
            )
        }
    }
}
