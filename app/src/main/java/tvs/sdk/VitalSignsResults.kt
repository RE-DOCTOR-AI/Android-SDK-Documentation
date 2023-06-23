package tvs.sdk

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VitalSignsResults : AppCompatActivity() {
    private var p = 0
    var vbp1 = 0
    var vbp2 = 0
    var vrr = 0
    var vhr = 0
    var vo2 = 0
    var glucoseMax = 0
    var glucoseMin = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_results)
        val vsrr = findViewById<TextView>(R.id.RRV)
        val vsbps = findViewById<TextView>(R.id.BP2V)
        val vshr = findViewById<TextView>(R.id.HRV)
        val vso2 = findViewById<TextView>(R.id.O2V)
        val glucoseField = findViewById<TextView>(R.id.GlucoseValue)
        val bundle = intent.extras
        if (bundle != null) {
            vrr = bundle.getInt("breath")
            vhr = bundle.getInt("bpm")
            vbp1 = bundle.getInt("SP")
            vbp2 = bundle.getInt("DP")
            val  bp = "$vbp1 / $vbp2"
            vo2 = bundle.getInt("O2R")
            vsrr.text = vrr.toString()
            vshr.text = vhr.toString()
            vsbps.text = bp
            vso2.text = vo2.toString()
            glucoseMin = bundle.getInt("glucoseMin")
            glucoseMax = bundle.getInt("glucoseMax")
            glucoseField.text = "[$glucoseMin - $glucoseMax]"
        }
        addStartAgainListener()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(this@VitalSignsResults, AboutApp::class.java)
        startActivity(i)
        finish()
    }

    private fun addStartAgainListener() {

        val startAgainButton = findViewById<Button>(R.id.StartAgain)

        startAgainButton.setOnClickListener { v: View ->
            p = 6
            val i = Intent(v.context, AboutApp::class.java)
            i.putExtra("Page", p)
            startActivity(i)
            finish()
        }
    }
}