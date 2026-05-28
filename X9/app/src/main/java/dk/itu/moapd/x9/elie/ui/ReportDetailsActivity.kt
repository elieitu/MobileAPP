package dk.itu.moapd.x9.elie.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import coil.load
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.databinding.ActivityReportDetailsBinding
import dk.itu.moapd.x9.elie.model.TrafficReport
import dk.itu.moapd.x9.elie.repository.StorageRepository

class ReportDetailsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_TITLE = "dk.itu.moapd.x9.elie.report_title"
        private const val EXTRA_LOCATION = "dk.itu.moapd.x9.elie.report_location"
        private const val EXTRA_DATE = "dk.itu.moapd.x9.elie.report_date"
        private const val EXTRA_TYPE = "dk.itu.moapd.x9.elie.report_type"
        private const val EXTRA_SEVERITY = "dk.itu.moapd.x9.elie.report_severity"
        private const val EXTRA_DESCRIPTION = "dk.itu.moapd.x9.elie.report_description"
        private const val EXTRA_IMAGE_URL = "dk.itu.moapd.x9.elie.report_image_url"
        private const val EXTRA_IMAGE_PATH = "dk.itu.moapd.x9.elie.report_image_path"

        fun newIntent(context: Context, report: TrafficReport): Intent {
            return Intent(context, ReportDetailsActivity::class.java).apply {
                putExtra(EXTRA_TITLE, report.title)
                putExtra(EXTRA_LOCATION, report.location)
                putExtra(EXTRA_DATE, report.date)
                putExtra(EXTRA_TYPE, report.type)
                putExtra(EXTRA_SEVERITY, report.severity)
                putExtra(EXTRA_DESCRIPTION, report.description)
                putExtra(EXTRA_IMAGE_URL, report.imageUrl)
                putExtra(EXTRA_IMAGE_PATH, report.imagePath)
            }
        }
    }

    private lateinit var binding: ActivityReportDetailsBinding
    private val storageRepository = StorageRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.title = getString(R.string.report_details)
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.reportTitleValue.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.reportLocationValue.text = intent.getStringExtra(EXTRA_LOCATION).orEmpty()
        binding.reportDateValue.text = intent.getStringExtra(EXTRA_DATE).orEmpty()
        binding.reportTypeValue.text = intent.getStringExtra(EXTRA_TYPE).orEmpty()
        binding.reportSeverityValue.text = intent.getStringExtra(EXTRA_SEVERITY).orEmpty()
        binding.reportDescriptionValue.text = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()

        showReportImage(
            imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL).orEmpty(),
            imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH).orEmpty()
        )
    }

    private fun showReportImage(imageUrl: String, imagePath: String) {
        when {
            imageUrl.isNotBlank() -> loadImage(imageUrl)
            imagePath.isNotBlank() -> resolveImagePathAndLoad(imagePath)
            else -> {
                binding.reportImageProgress.visibility = View.GONE
                binding.reportImageValue.setImageDrawable(null)
                binding.reportImageStatusValue.text = getString(R.string.report_image_not_available)
            }
        }
    }

    private fun resolveImagePathAndLoad(imagePath: String) {
        binding.reportImageProgress.visibility = View.VISIBLE
        binding.reportImageStatusValue.text = getString(R.string.report_image_loading)

        storageRepository.getDownloadUrl(imagePath)
            .addOnSuccessListener { uri ->
                loadImage(uri.toString())
            }
            .addOnFailureListener {
                binding.reportImageProgress.visibility = View.GONE
                binding.reportImageStatusValue.text = getString(R.string.report_image_load_failed)
            }
    }

    private fun loadImage(url: String) {
        binding.reportImageProgress.visibility = View.VISIBLE
        binding.reportImageStatusValue.text = getString(R.string.report_image_loading)

        binding.reportImageValue.load(url) {
            crossfade(true)
            listener(
                onSuccess = { _, _ ->
                    binding.reportImageProgress.visibility = View.GONE
                    binding.reportImageStatusValue.text = getString(R.string.report_image_loaded)
                },
                onError = { _, _ ->
                    binding.reportImageProgress.visibility = View.GONE
                    binding.reportImageStatusValue.text = getString(R.string.report_image_load_failed)
                }
            )
        }
    }
}
