package dk.itu.moapd.x9.elie.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dk.itu.moapd.x9.elie.fragment.MainFragment
import dk.itu.moapd.x9.elie.R
import dk.itu.moapd.x9.elie.activity.TrafficReportActivity

class MainActivity : AppCompatActivity(), MainFragment.Callbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment.Companion.newInstance())
                .commit()
        }
    }

    override fun onOpenCreateReport() {
        startActivity(Intent(this, TrafficReportActivity::class.java))
    }
}