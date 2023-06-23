package tvs.sdk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tvs.model.FramesDataAndroid
import com.tvs.model.GlucoseLevelProcessorAndroid
import com.tvs.model.User
import com.tvs.model.UserParameters
import com.tvs.utils.ProcessingStatus
import com.tvs.vitals.VitalSignsProcessorNg

class CalculatingResults : AppCompatActivity() {

    private fun isGlucoseEnabled(): Boolean {
        return true
    }

    private fun isVitalsEnabled(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculating_results)

        val glucoseLevelProcessor = GlucoseLevelProcessorAndroid()
        val vitalsProcessor = VitalSignsProcessorNg(this.getUserParameters())

        // Run in thread to avoid blocking UI
        Thread {
            val glucoseFrameData = this.getGlucoseFrameData()
            val vitalsFrameData = this.getVitalsFrameData()
            val vitalsResult = vitalsProcessor.process(vitalsFrameData)
            val glucoseResult = glucoseLevelProcessor.process(glucoseFrameData)

            if (glucoseResult === ProcessingStatus.FINISHED && vitalsResult == ProcessingStatus.FINISHED) {
                val i = Intent(this@CalculatingResults, VitalSignsResults::class.java)
                i.putExtra("glucoseMin", glucoseLevelProcessor.getGlucoseMinValue())
                i.putExtra("glucoseMax", glucoseLevelProcessor.getGlucoseMaxValue())
                i.putExtra("O2R", vitalsProcessor.o2.value)
                i.putExtra("breath", vitalsProcessor.Breath.value)
                i.putExtra("bpm", vitalsProcessor.Beats.value)
                i.putExtra("SP", vitalsProcessor.SP.value)
                i.putExtra("DP", vitalsProcessor.DP.value)
                startActivity(i)
                finish()
            } else {
                val i = Intent(this@CalculatingResults, CalculatingResults::class.java)
                startActivity(i)
                finish()
            }
        }.start()
    }

    private fun getUserParameters(): User {
        val bundle = intent.extras ?: throw IllegalArgumentException("No bundle")

        if (!bundle.containsKey("userParams")) {
            throw IllegalArgumentException("No userParams in bundle")
        }

        return bundle.getSerializable("userParams") as UserParameters?
            ?: throw IllegalArgumentException("No userParams in bundle")
    }

    private fun getGlucoseFrameData(): FramesDataAndroid {
        val bundle = intent.extras ?: throw IllegalArgumentException("No bundle")

        if (!bundle.containsKey("glucoseData")) {
            throw IllegalArgumentException("No glucoseData in bundle")
        }

        return bundle.getSerializable("glucoseData") as FramesDataAndroid?
            ?: throw IllegalArgumentException("No glucoseData in bundle")
    }

    private fun getVitalsFrameData(): FramesDataAndroid {
        val bundle = intent.extras ?: throw IllegalArgumentException("No bundle")

        if (!bundle.containsKey("vitalsData")) {
            throw IllegalArgumentException("No vitalsData in bundle")
        }

        return bundle.getSerializable("vitalsData") as FramesDataAndroid?
            ?: throw IllegalArgumentException("No vitalsData in bundle")
    }
}