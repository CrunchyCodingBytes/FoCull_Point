package com.example.focullpointv2.ui.culling

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.focullpointv2.data.SettingsRepository
import com.example.focullpointv2.filemanager.DefaultFileMover
import com.example.focullpointv2.filemanager.MoveResult
import com.example.focullpointv2.filemanager.PhotoScanner
import com.example.focullpointv2.model.ConflictStrategy
import com.example.focullpointv2.model.CullAction
import com.example.focullpointv2.model.KeyBindings
import com.example.focullpointv2.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PendingConflict(
    val action: CullAction,
    val item: PhotoItem,
    val conflictingFiles: List<File>
)

/**
 * Snapshot needed to undo the last committed action.
 * [restoreMappings] are `(currentLocation -> originalLocation)` pairs to move files
 * back; empty for a Skip (nothing was moved).
 */
private data class UndoRecord(
    val previousQueue: List<PhotoItem>,
    val previousIndex: Int,
    val restoreMappings: List<Pair<File, File>>
)

data class CullingUiState(
    val loading: Boolean = true,
    val queue: List<PhotoItem> = emptyList(),
    val index: Int = 0,
    val pendingConflict: PendingConflict? = null,
    val isComplete: Boolean = false,
    val isEmpty: Boolean = false,
    val canUndo: Boolean = false,
    val errorMessage: String? = null
) {
    val currentItem: PhotoItem? get() = queue.getOrNull(index)
    val nextItem: PhotoItem? get() = queue.getOrNull(index + 1)
    val processedCount: Int get() = index
    val totalCount: Int get() = queue.size
}

class CullingViewModel(
    app: Application,
    private val sourceDir: File,
    private val favoriteDir: File,
    private val rejectDir: File
) : AndroidViewModel(app) {

    private val scanner = PhotoScanner()
    private val mover = DefaultFileMover()
    private val repo = SettingsRepository(app)

    val keyBindings: StateFlow<KeyBindings> = repo.keyBindings
        .stateIn(viewModelScope, SharingStarted.Eagerly, KeyBindings())

    val themeMode: StateFlow<com.example.focullpointv2.model.ThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.focullpointv2.model.ThemeMode.DARK)

    private val _uiState = MutableStateFlow(CullingUiState())
    val uiState: StateFlow<CullingUiState> = _uiState.asStateFlow()

    /** Snapshot of the single most recent action, or null when there is nothing to undo. */
    private var lastAction: UndoRecord? = null

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) { scanner.scan(sourceDir) }
            _uiState.value = _uiState.value.copy(
                loading = false,
                queue = items,
                index = 0,
                isEmpty = items.isEmpty(),
                isComplete = items.isEmpty()
            )
        }
    }

    /** Handles a committed cull action for the current item. */
    fun onAction(action: CullAction) {
        val state = _uiState.value
        if (state.pendingConflict != null) return
        val item = state.currentItem ?: return

        when (action) {
            CullAction.SKIP -> {
                val record = UndoRecord(state.queue, state.index, emptyList())
                requeueAndAdvance(item)
                lastAction = record
                _uiState.value = _uiState.value.copy(canUndo = true)
            }
            CullAction.FAVORITE -> moveCurrent(item, action, favoriteDir, strategy = null)
            CullAction.REJECT -> moveCurrent(item, action, rejectDir, strategy = null)
        }
    }

    /** Undoes the most recent action, including reverting any file move. */
    fun undo() {
        val record = lastAction ?: return
        if (_uiState.value.pendingConflict != null) return
        lastAction = null

        if (record.restoreMappings.isEmpty()) {
            // Skip (or no-op) undo: nothing was moved, just restore the queue/index.
            restoreFromUndo(record)
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { mover.moveBack(record.restoreMappings) }
            when (result) {
                is MoveResult.Success -> restoreFromUndo(record)
                is MoveResult.Error -> _uiState.value = _uiState.value.copy(
                    errorMessage = "Undo failed: ${result.throwable.message}"
                )
                is MoveResult.Conflict -> _uiState.value = _uiState.value.copy(
                    errorMessage = "Undo failed: destination conflict."
                )
            }
        }
    }

    private fun restoreFromUndo(record: UndoRecord) {
        _uiState.value = _uiState.value.copy(
            queue = record.previousQueue,
            index = record.previousIndex,
            isComplete = false,
            canUndo = false
        )
    }

    /** Resolves a pending destination conflict with the user's chosen [strategy]. */
    fun resolveConflict(strategy: ConflictStrategy) {
        val pending = _uiState.value.pendingConflict ?: return
        val destDir = destFor(pending.action)
        _uiState.value = _uiState.value.copy(pendingConflict = null)
        moveCurrent(pending.item, pending.action, destDir, strategy)
    }

    fun dismissConflict() {
        _uiState.value = _uiState.value.copy(pendingConflict = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun destFor(action: CullAction): File = when (action) {
        CullAction.FAVORITE -> favoriteDir
        CullAction.REJECT -> rejectDir
        CullAction.SKIP -> sourceDir
    }

    private fun moveCurrent(
        item: PhotoItem,
        action: CullAction,
        destDir: File,
        strategy: ConflictStrategy?
    ) {
        val prevQueue = _uiState.value.queue
        val prevIndex = _uiState.value.index
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { mover.move(item, destDir, strategy) }
            when (result) {
                is MoveResult.Success -> {
                    // Map each moved file back to its original location for undo.
                    val restoreMappings = item.files
                        .zip(result.movedFiles)
                        .map { (original, moved) -> moved to original }
                    lastAction = UndoRecord(prevQueue, prevIndex, restoreMappings)
                    advance()
                    _uiState.value = _uiState.value.copy(canUndo = true)
                }
                is MoveResult.Conflict -> {
                    _uiState.value = _uiState.value.copy(
                        pendingConflict = PendingConflict(action, item, result.conflictingFiles)
                    )
                }
                is MoveResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Move failed: ${result.throwable.message}"
                    )
                }
            }
        }
    }

    private fun requeueAndAdvance(item: PhotoItem) {
        // Re-add the skipped item at the end so it reappears after the rest.
        val newQueue = _uiState.value.queue + item
        _uiState.value = _uiState.value.copy(queue = newQueue)
        advance()
    }

    private fun advance() {
        val next = _uiState.value.index + 1
        val complete = next >= _uiState.value.queue.size
        _uiState.value = _uiState.value.copy(index = next, isComplete = complete)
    }

    class Factory(
        private val app: Application,
        private val sourceDir: File,
        private val favoriteDir: File,
        private val rejectDir: File
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CullingViewModel(app, sourceDir, favoriteDir, rejectDir) as T
        }
    }
}
