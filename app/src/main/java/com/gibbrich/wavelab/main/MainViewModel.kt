package com.gibbrich.wavelab.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gibbrich.wavelab.data.ResourceManager
import com.gibbrich.wavelab.di.DI
import com.gibbrich.wavelab.model.Wave
import com.gibbrich.wavelab.model.WavePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


/**
 * NOTE - For further improvement:
 * 1. Notify user in case of wave data load/save error
 * 2. Handle case large files read/write - add loader and make these operations cancelable
 */
class MainViewModel : ViewModel() {
    @Inject
    lateinit var resourceManager: ResourceManager

    private val _wave = MutableStateFlow<Wave?>(null)
    val wave: StateFlow<Wave?> = _wave

    private val _canExport = MutableStateFlow(false)
    val canExport: StateFlow<Boolean> = _canExport

    private val _canResetSelection = MutableStateFlow(false)
    val canResetSelection: StateFlow<Boolean> = _canResetSelection

    var selectionPoints = 0 to 0
        private set

    init {
        DI.appComponent.inject(this)
    }

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
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

            if (createdWave != null) {
                _wave.emit(createdWave)
                onWaveSelectionChanged(0, createdWave.data.lastIndex)
            }
        }
    }

    fun onExportFileRequest(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _wave.value?.data?.subList(selectionPoints.first, selectionPoints.second)?.let {
                resourceManager.openOutputStream(uri, "wt")?.use { outputStream ->
                    WavePoint.save(it, outputStream)
                }
            }
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
