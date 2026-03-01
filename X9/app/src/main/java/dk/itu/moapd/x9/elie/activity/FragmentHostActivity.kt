package dk.itu.moapd.x9.elie.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.fragment.CreateReportFragment
import dk.itu.moapd.x9.elie.fragment.MainFragment

class FragmentHostActivity : AppCompatActivity(),
    MainFragment.Callbacks,
    CreateReportFragment.Callbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment.newInstance())
                .commit()
        }
    }

    override fun onOpenCreateReport() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CreateReportFragment.newInstance())
            .addToBackStack("create_report")
            .commit()
    }

    override fun onReportCreated(type: String, description: String, severity: String) {
        supportFragmentManager.popBackStack()
        supportFragmentManager.executePendingTransactions()

        val mainFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as? MainFragment
        mainFragment?.applyReturnedReport(type, description, severity)
    }
}
