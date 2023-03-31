package com.gibbrich.wavelab.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gibbrich.wavelab.databinding.ActivityMainBinding
import com.gibbrich.wavelab.main.MainViewModel
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel>()

    private lateinit var fileSelectLauncher: ActivityResultLauncher<Unit>
    private lateinit var fileSaveLauncher: ActivityResultLauncher<Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        fileSelectLauncher =
            registerForActivityResult(FileSelectContract(), ::handleFileSelectionResult)
        fileSaveLauncher = registerForActivityResult(FileSaveContract(), ::handleSaveFileResult)

        binding.apply {
            setContentView(binding.root)

            activityMainImportWave.setOnClickListener {
                fileSelectLauncher.launch(Unit)
            }

            // export button
            activityMainExportWave.setOnClickListener {
                fileSaveLauncher.launch(Unit)
            }

            lifecycleScope.launch {
                viewModel.canExport.collect {
                    activityMainExportWave.isEnabled = it
                }
            }

            // reset selection button
            activityMainResetSelection.setOnClickListener {
                viewModel.onResetSelectionRequest()
                updateSelection()
            }

            lifecycleScope.launch {
                viewModel.canResetSelection.collect {
                    activityMainResetSelection.isEnabled = it
                }
            }

            waveView.onSelectedPointsChanged = viewModel::onWaveSelectionChanged

            lifecycleScope.launch {
                viewModel.wave.collect { wave ->
                    if (wave != null) {
                        waveName.text = wave.name
                        waveView.setData(wave.data)
                    }
                }
            }

            updateSelection()
        }
    }

    private fun updateSelection() =
        binding.waveView.setSelection(viewModel.selectionPoints.first, viewModel.selectionPoints.second)

    private fun handleFileSelectionResult(uri: Uri?) = uri?.let(viewModel::onFileSelected)
    private fun handleSaveFileResult(uri: Uri?) = uri?.let(viewModel::onExportFileRequest)
}

private class FileSaveContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) =
        getIntent(Intent.ACTION_CREATE_DOCUMENT)
            .apply {
                putExtra(Intent.EXTRA_TITLE, "export.txt")
            }

    override fun parseResult(resultCode: Int, intent: Intent?) = getResult(resultCode, intent)
}

private class FileSelectContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) = getIntent(Intent.ACTION_OPEN_DOCUMENT)
    override fun parseResult(resultCode: Int, intent: Intent?) = getResult(resultCode, intent)
}

private fun getIntent(action: String) = Intent(action).apply {
    type = "text/plain"
    addCategory(Intent.CATEGORY_OPENABLE)
}

private fun getResult(resultCode: Int, intent: Intent?) = intent?.data?.takeIf {
    resultCode == Activity.RESULT_OK
}