package com.serratocreations.phovo.data.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.serratocreations.phovo.core.common.annotations.SingleUseMutex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import com.serratocreations.phovo.core.database.dao.PermissionsDao
import com.serratocreations.phovo.core.database.entities.PermissionsEntity
import com.serratocreations.phovo.data.permissions.annotations.DelicatePermissionsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

typealias Permission = String

class AndroidPermissionRepository(
    private val permissionsDataSource: PermissionsDao,
    private val context: Context,
    private val appScope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher
) : PermissionRepository {

    @SingleUseMutex
    private val permissionRequestMutex = Mutex()

    private val _requestEventQueue = Channel<PermissionRequest>(Channel.BUFFERED)
    val requestEventQueue = _requestEventQueue.receiveAsFlow()

    private val resultChannel = Channel<List<PermissionRequestResult>>(Channel.BUFFERED)

    private val _permissionsState = MutableStateFlow<Map<Permission, PermissionStatus>?>(null)
    val permissionsState = _permissionsState.asStateFlow()

    init {
        appScope.launch {
            updatePermissionsState()
        }
    }

    override suspend fun requestGalleryPermissions(): GalleryPermissionsStatus {
        val permissions = mutableSetOf<Permission>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
        firePermissionRequestAndHandleResult(
            PermissionRequest(
                permissions = permissions.toTypedArray()
            )
        )

        return observeGalleryPermissionStatus().first()
    }

    override suspend fun requestLocalNetworkPermissions(): PermissionStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            firePermissionRequestAndHandleResult(
                PermissionRequest(
                    permissions = arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK)
                )
            )
        }
        return observeLocalNetworkPermissionStatus().first()
    }

    fun onPermissionResult(result: List<PermissionRequestResult>) {
        resultChannel.trySend(result)
    }

    fun updatePermissionsStateSynchronous() {
        appScope.launch {
            updatePermissionsState()
        }
    }

    private suspend fun updatePermissionsState() {
        withContext(defaultDispatcher) {
            _permissionsState.update { current ->
                val permanentlyDeniedPermissions =
                    permissionsDataSource.deniedPermissionFlow().first()
                        .filter { it.isPermanentlyDenied }
                        .map { it.permissionId }
                val updateState = current?.toMutableMap() ?: mutableMapOf()
                PermissionDeclaration.entries.toSet()
                    .forEach { permission ->
                        val isGranted = ContextCompat.checkSelfPermission(
                            context,
                            permission.permissionId
                        ) == PackageManager.PERMISSION_GRANTED
                        if (isGranted) {
                            updateState[permission.permissionId] = PermissionStatus.Granted
                            if (permission.permissionId in permanentlyDeniedPermissions) {
                                appScope.launch {
                                    // remove granted from permanently denied
                                    permissionsDataSource.remove(permission.permissionId)
                                }
                            }
                        } else if (permission.permissionId in permanentlyDeniedPermissions) {
                            updateState[permission.permissionId] =
                                PermissionStatus.PermanentlyDenied
                        } else {
                            updateState[permission.permissionId] = PermissionStatus.Ungranted
                        }
                    }
                updateState
            }
        }
    }

    @DelicatePermissionsApi
    override fun galleryPermissionStatus(): GalleryPermissionsStatus {
        val permissionsState = _permissionsState.value ?: throw IllegalStateException("Permissions status state holder not initialized")
        return getGalleryPermissionStatus(permissionsState)
    }

    override fun observeGalleryPermissionStatus(): Flow<GalleryPermissionsStatus> =
        _permissionsState.filterNotNull().map { permissionsState ->
            getGalleryPermissionStatus(permissionsState)
        }

    private fun getGalleryPermissionStatus(
        permissionsState: Map<Permission, PermissionStatus>
    ): GalleryPermissionsStatus {
        val status = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionsState.getValue(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            setOf(
                permissionsState.getValue(Manifest.permission.READ_MEDIA_IMAGES),
                permissionsState.getValue(Manifest.permission.READ_MEDIA_VIDEO)
            ).representativeStatus()
        }
        val isFullGranted = status == PermissionStatus.Granted
        val isLimited = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsState[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == PermissionStatus.Granted &&
                    isFullGranted.not()
        } else {
            false
        }
        return GalleryPermissionsStatus(
            permissionStatus = status,
            isLimited = isLimited
        )
    }

    @DelicatePermissionsApi
    override fun localNetworkPermissionStatus(): PermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) {
            return PermissionStatus.Granted
        }
        val permissionsState = _permissionsState.value ?: return PermissionStatus.Ungranted
        return permissionsState[Manifest.permission.ACCESS_LOCAL_NETWORK] ?: PermissionStatus.Ungranted
    }

    override fun observeLocalNetworkPermissionStatus(): Flow<PermissionStatus> =
        _permissionsState.filterNotNull().map { permissionsState ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) {
                PermissionStatus.Granted
            } else {
                permissionsState[Manifest.permission.ACCESS_LOCAL_NETWORK] ?: PermissionStatus.Ungranted
            }
        }

    override fun openSystemPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    @OptIn(SingleUseMutex::class)
    private suspend fun firePermissionRequestAndHandleResult(
        request: PermissionRequest
    ) {
        val result = permissionRequestMutex.withLock {
            _requestEventQueue.send(request)
            return@withLock resultChannel.receive()
        }
        appScope.launch {
            result.forEach { result ->
                if (result.isGranted.not() && result.shouldShowRequestPermissionRationale.not()) {
                    // Permission has been perma denied, notify data source
                    permissionsDataSource.insert(
                        PermissionsEntity(
                            result.permission,
                            isPermanentlyDenied = true
                        )
                    )
                }
            }
            updatePermissionsState()
        }.join()
    }

    private fun Set<PermissionStatus>.representativeStatus(): PermissionStatus {
        require(this.isEmpty().not()) { "Permission set should not be empty" }
        return if (this.any { it == PermissionStatus.PermanentlyDenied }) {
            PermissionStatus.PermanentlyDenied
        } else if (this.any { it == PermissionStatus.Ungranted }) {
            PermissionStatus.Ungranted
        } else {
            PermissionStatus.Granted
        }
    }
}

enum class PermissionDeclaration(val permissionId: String) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    Images(Manifest.permission.READ_MEDIA_IMAGES),
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    Video(Manifest.permission.READ_MEDIA_VIDEO),
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    UserSelectedMedia(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    AccessLocalNetwork(Manifest.permission.ACCESS_LOCAL_NETWORK)
}

class PermissionRequest(
    val permissions: Array<Permission>
)

data class PermissionRequestResult(
    val permission: Permission,
    val isGranted: Boolean,
    val shouldShowRequestPermissionRationale: Boolean
)