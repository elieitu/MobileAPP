package dk.itu.moapd.x9.elie

import android.content.Intent
import android.os.Bundle
import dk.itu.moapd.x9.elie.R
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), MainFragment.Callbacks {

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
        startActivity(Intent(this, TrafficReportActivity::class.java))
    }
}