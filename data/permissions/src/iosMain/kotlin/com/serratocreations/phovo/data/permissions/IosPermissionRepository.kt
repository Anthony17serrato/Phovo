package com.serratocreations.phovo.data.permissions

import com.serratocreations.phovo.core.database.dao.PermissionsDao
import com.serratocreations.phovo.core.database.entities.PermissionsEntity
import com.serratocreations.phovo.core.database.entities.PermissionStateEntity
import com.serratocreations.phovo.data.permissions.annotations.DelicatePermissionsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class IosPermissionRepository(
    private val permissionsDataSource: PermissionsDao,
    private val appScope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher,
    private val localNetworkPermissionDelegate: LocalNetworkPermissionDelegate
) : PermissionRepository {

    private val _permissionsState = MutableStateFlow<Map<Permission, PermissionStatus>?>(null)
    val permissionsState = _permissionsState.asStateFlow()

    /** Serialises local network probes; the platform delegate supports one at a time. */
    private val localNetworkProbeMutex = Mutex()

    // TODO This is not the correct place to have this logic but this code detects when photoLibrary
    //  changes such as photo picker updates or new library photos available, it should be updated
    //  as part of https://github.com/Anthony17serrato/Phovo/issues/123
//    private val photoLibraryObserver = object : NSObject(), PHPhotoLibraryChangeObserverProtocol {
//        override fun photoLibraryDidChange(changeInstance: PHChange) {
//            appScope.launch {
//                updatePermissionsState()
//            }
//        }
//    }
//    PHPhotoLibrary.sharedPhotoLibrary().registerChangeObserver(photoLibraryObserver)

    init {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            appScope.launch {
                updatePermissionsState()
                // Re-probe local network in background on resume to detect Settings changes
                reprobeLocalNetworkInBackground()
            }
        }
        appScope.launch {
            updatePermissionsState()
        }
    }

    private suspend fun updatePermissionsState() {
        withContext(defaultDispatcher) {
            _permissionsState.update { current ->
                val allPersistedPermissions =
                    permissionsDataSource.permissionFlow().first()
                val updateState = current?.toMutableMap() ?: mutableMapOf()

                // Gallery permission — synchronous check
                val photoStatus = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
                when (photoStatus) {
                    PHAuthorizationStatusAuthorized -> {
                        updateState[IosPermissionDeclaration.PhotoLibrary.permissionId] = PermissionStatus.Granted
                        updateState[IosPermissionDeclaration.PhotoLibraryPartial.permissionId] = PermissionStatus.Ungranted
                    }
                    PHAuthorizationStatusLimited -> {
                        updateState[IosPermissionDeclaration.PhotoLibrary.permissionId] = PermissionStatus.Ungranted
                        updateState[IosPermissionDeclaration.PhotoLibraryPartial.permissionId] = PermissionStatus.Granted
                    }
                    PHAuthorizationStatusNotDetermined -> {
                        updateState[IosPermissionDeclaration.PhotoLibrary.permissionId] = PermissionStatus.Ungranted
                        updateState[IosPermissionDeclaration.PhotoLibraryPartial.permissionId] = PermissionStatus.Ungranted
                    }
                    else -> {
                        updateState[IosPermissionDeclaration.PhotoLibrary.permissionId] = PermissionStatus.PermanentlyDenied
                        updateState[IosPermissionDeclaration.PhotoLibraryPartial.permissionId] = PermissionStatus.PermanentlyDenied
                    }
                }

                // Local network permission — read from persisted state
                // 3 states: null (never asked -> Ungranted), Allowed -> Granted, Denied -> Ungranted, PermanentlyDenied -> PermanentlyDenied
                val localNetworkPermissionId =
                    IosPermissionDeclaration.LocalNetwork.permissionId
                val persistedEntity = allPersistedPermissions.find { it.permissionId == localNetworkPermissionId }
                updateState[localNetworkPermissionId] = when (persistedEntity?.state) {
                    PermissionStateEntity.Allowed -> PermissionStatus.Granted
                    PermissionStateEntity.PermanentlyDenied -> PermissionStatus.PermanentlyDenied
                    PermissionStateEntity.Denied -> PermissionStatus.Ungranted
                    null -> PermissionStatus.Ungranted
                }

                updateState
            }
        }
    }

    @DelicatePermissionsApi
    override fun galleryPermissionStatus(): GalleryPermissionsStatus {
        val permissionsState = _permissionsState.value
            ?: throw IllegalStateException("Permissions status state holder not initialized")
        return getGalleryPermissionStatus(permissionsState)
    }

    override fun observeGalleryPermissionStatus(): Flow<GalleryPermissionsStatus> =
        _permissionsState.filterNotNull().map { permissionsState ->
            getGalleryPermissionStatus(permissionsState)
        }

    private fun getGalleryPermissionStatus(
        permissionsState: Map<Permission, PermissionStatus>
    ): GalleryPermissionsStatus {
        val status = permissionsState.getValue(IosPermissionDeclaration.PhotoLibrary.permissionId)
        val isFullGranted = status == PermissionStatus.Granted
        val isLimited = permissionsState[IosPermissionDeclaration.PhotoLibraryPartial.permissionId] == PermissionStatus.Granted &&
                isFullGranted.not()

        return GalleryPermissionsStatus(
            permissionStatus = status,
            isLimited = isLimited
        )
    }

    override suspend fun requestGalleryPermissions(): GalleryPermissionsStatus {
        val permissionState = _permissionsState.filterNotNull().first()
        val currentStatus = getGalleryPermissionStatus(permissionState)
        if (currentStatus.permissionStatus == PermissionStatus.Granted || currentStatus.isLimited) {
            updatePermissionsState()
            return currentStatus
        }

        suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { _ ->
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
        updatePermissionsState()
        return observeGalleryPermissionStatus().first()
    }

    private fun reprobeLocalNetworkInBackground() {
        appScope.launch {
            val allPersistedPermissions = permissionsDataSource.permissionFlow().first()
            val localNetworkPermissionId = IosPermissionDeclaration.LocalNetwork.permissionId
            val persistedState = allPersistedPermissions
                .firstOrNull { it.permissionId == localNetworkPermissionId }
                ?.state

            // Only re-probe when iOS has already given a definitive answer that the user could
            // have since changed in Settings. `Denied` here means an earlier probe was
            // inconclusive (timeout or transient failure), so the system prompt may never have
            // been shown — probing on resume would surface it cold, outside the onboarding
            // primer that explains why we need it. That case belongs to the user-initiated flow.
            val shouldReprobe = persistedState == PermissionStateEntity.Allowed ||
                persistedState == PermissionStateEntity.PermanentlyDenied
            if (!shouldReprobe) {
                return@launch
            }

            // Permission has already been answered once, so no system prompt appears and the
            // probe resolves quickly.
            val result = probeLocalNetworkPermission(timeout = SILENT_PROBE_TIMEOUT)
            persistLocalNetworkResult(result)
            updatePermissionsState()
        }
    }

    @DelicatePermissionsApi
    override fun localNetworkPermissionStatus(): PermissionStatus {
        val permissionsState = _permissionsState.value
            ?: throw IllegalStateException("Permissions status state holder not initialized")
        return permissionsState.getValue(IosPermissionDeclaration.LocalNetwork.permissionId)
    }

    override fun observeLocalNetworkPermissionStatus(): Flow<PermissionStatus> =
        _permissionsState.filterNotNull().map { permissionsState ->
            permissionsState.getValue(IosPermissionDeclaration.LocalNetwork.permissionId)
        }

    override suspend fun requestLocalNetworkPermissions(): PermissionStatus {
        // The system prompt may appear here and the user's think time is unbounded, so this
        // timeout is only a guard against a probe that never resolves at all.
        val result = probeLocalNetworkPermission(timeout = PROMPTED_PROBE_TIMEOUT)
        persistLocalNetworkResult(result)
        updatePermissionsState()
        return observeLocalNetworkPermissionStatus().first()
    }

    /**
     * Runs a single local network probe and maps its outcome to a [PermissionStatus].
     *
     * The probe is serialised by [localNetworkProbeMutex] because the delegate is a singleton that
     * can only track one in-flight probe: a user-initiated request and the background reprobe can
     * otherwise overlap, and the second would silently discard the first one's callback.
     *
     * If the probe never reports an outcome it is cancelled after [timeout] and reported as
     * [PermissionStatus.Ungranted] — an inconclusive probe is retryable and must not be persisted
     * as a permanent denial.
     */
    private suspend fun probeLocalNetworkPermission(timeout: Duration): PermissionStatus =
        localNetworkProbeMutex.withLock {
            val event = withTimeoutOrNull(timeout) {
                suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation {
                        localNetworkPermissionDelegate.cancelProbe()
                    }
                    localNetworkPermissionDelegate.startProbe { event ->
                        if (continuation.isActive) {
                            continuation.resume(event)
                        }
                    }
                }
            }

            when (event) {
                // Publishing only succeeds once access is authorized.
                LocalNetworkProbeEvent.Published -> PermissionStatus.Granted
                // iOS offers no in-app path back from a denial; the user must use Settings.
                LocalNetworkProbeEvent.Denied -> PermissionStatus.PermanentlyDenied
                // Inconclusive: a transient failure or a timeout. Retryable, not permanent.
                LocalNetworkProbeEvent.Failed,
                null -> PermissionStatus.Ungranted
            }
        }

    private suspend fun persistLocalNetworkResult(result: PermissionStatus) {
        val permissionId = IosPermissionDeclaration.LocalNetwork.permissionId
        val state = when (result) {
            PermissionStatus.Granted -> PermissionStateEntity.Allowed
            PermissionStatus.PermanentlyDenied -> PermissionStateEntity.PermanentlyDenied
            PermissionStatus.Ungranted -> PermissionStateEntity.Denied
        }
        permissionsDataSource.insert(
            PermissionsEntity(
                permissionId = permissionId,
                state = state
            )
        )
    }

    override fun openSystemPermissionSettings() {
        val url = NSURL(string = UIApplicationOpenSettingsURLString)
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(
                url,
                options = emptyMap<Any?, Any>(),
                completionHandler = null
            )
        }
    }
}

/**
 * Guard for a user-initiated probe. The system prompt may be on screen, so this must comfortably
 * exceed the time a user might spend reading it.
 */
private val PROMPTED_PROBE_TIMEOUT: Duration = 30.seconds

/** Guard for a background reprobe, where permission was already answered and no prompt appears. */
private val SILENT_PROBE_TIMEOUT: Duration = 10.seconds

enum class IosPermissionDeclaration(val permissionId: String) {
    PhotoLibrary("photo_library"),
    PhotoLibraryPartial("photo_library_partial"),
    LocalNetwork("local_network")
}
