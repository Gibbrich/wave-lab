package com.gibbrich.wavelab.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gibbrich.wavelab.data.ResourceManager
import com.gibbrich.wavelab.model.Wave
import com.gibbrich.wavelab.model.WavePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * NOTE - For further improvement:
 * 1. Handle case large files read/write - add loader and make these operations cancelable
 */
class MainViewModel(private val resourceManager: ResourceManager) : ViewModel() {
    private val _wave = MutableStateFlow<Wave?>(null)
    val wave: StateFlow<Wave?> = _wave

    private val _waveLoading = MutableStateFlow(false)
    val waveLoading: SharedFlow<Boolean> = _waveLoading

    private val _waveLoadError = MutableSharedFlow<WaveLoadError>()
    val waveLoadError: SharedFlow<WaveLoadError> = _waveLoadError

    private val _canExport = MutableStateFlow(false)
    val canExport: StateFlow<Boolean> = _canExport

    private val _canResetSelection = MutableStateFlow(false)
    val canResetSelection: StateFlow<Boolean> = _canResetSelection

    var selectionPoints = 0 to 0
        private set

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            _waveLoading.emit(true)
            val createdWave = withContext(Dispatchers.IO) {
                try {
                    resourceManager.openInputStream(uri)?.use { inputStream ->
                        val name = resourceManager.getFileName(uri)
                        val points = WavePoint.load(inputStream)
                        Wave(name, points)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            _waveLoading.emit(false)

            if (createdWave != null) {
                _wave.emit(createdWave)
                onWaveSelectionChanged(0, createdWave.data.lastIndex)
            } else {
                _waveLoadError.emit(WaveLoadError.Import)
            }
        }
    }

    fun onExportFileRequest(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _waveLoading.emit(true)
            try {
                _wave.value?.data?.subList(selectionPoints.first, selectionPoints.second)?.let {
                    resourceManager.openOutputStream(uri, "wt")?.use { outputStream ->
                        WavePoint.save(it, outputStream)
                    }
                }
            } catch (e: Exception) {
                _waveLoadError.emit(WaveLoadError.Export)
            }
            _waveLoading.emit(false)
        }
    }

    fun onWaveSelectionChanged(startPointId: Int, endPointId: Int) {
        selectionPoints = startPointId to endPointId
        val hasData = _wave.value?.data?.isNotEmpty() ?: false

        _canExport.value = hasData && endPointId > startPointId
        _canResetSelection.value = hasData
    }

    fun onResetSelectionRequest() {
        onWaveSelectionChanged(0, _wave.value?.data?.lastIndex ?: 0)
    }
}

class MainViewModelFactory(private val resourceManager: ResourceManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(resourceManager) as T
    }
}

sealed class WaveLoadError {
    object Import : WaveLoadError()
    object Export : WaveLoadError()
}
