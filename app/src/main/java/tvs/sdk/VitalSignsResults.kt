package tvs.sdk

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VitalSignsResults : AppCompatActivity() {
    var vbp1 = 0
    var vbp2 = 0
    var vrr = 0
    var vhr = 0
    var vo2 = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_results)
        val vsrr = findViewById<TextView>(R.id.RRV)
        val vsbps = findViewById<TextView>(R.id.BP2V)
        val vshr = findViewById<TextView>(R.id.HRV)
        val vso2 = findViewById<TextView>(R.id.O2V)
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
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(this@VitalSignsResults, AboutApp::class.java)
        startActivity(i)
        finish()
    }
}