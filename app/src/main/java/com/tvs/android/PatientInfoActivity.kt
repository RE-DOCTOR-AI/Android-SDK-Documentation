package com.tvs.android

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Button
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.gson.Gson
import com.tvs.android.utill.CM_TO_FEET_COEFF
import com.tvs.android.utill.IMPERIAL_HEIGHT_LOW_LIMIT
import com.tvs.android.utill.IMPERIAL_HEIGHT_TOP_LIMIT
import com.tvs.android.utill.IMPERIAL_WEIGHT_LOW_LIMIT
import com.tvs.android.utill.IMPERIAL_WEIGHT_TOP_LIMIT
import com.tvs.android.utill.KG_TO_LB_COEFF
import com.tvs.android.utill.MAX_AGE
import com.tvs.android.utill.METRIC_HEIGHT_LOW_LIMIT
import com.tvs.android.utill.METRIC_HEIGHT_TOP_LIMIT
import com.tvs.android.utill.METRIC_WEIGHT_LOW_LIMIT
import com.tvs.android.utill.METRIC_WEIGHT_TOP_LIMIT
import com.tvs.android.utill.MIN_AGE
import com.tvs.model.UserInfo
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Activity to edit and save patient data that is later loaded and used by Re.Doctor library to
 * calculate certain vital signs
 */
class PatientInfoActivity : AppCompatActivity() {
    private var gender: SwitchCompat? = null
    private var measure: SwitchCompat? = null
    private var metric: TextView? = null
    private var imperial: TextView? = null
    private var firstName: EditText? = null
    private var lastName: EditText? = null
    private var height: EditText? = null
    private var weight: EditText? = null
    private var birthday: EditText? = null
    private var age: EditText? = null
    private var heightText: TextView? = null
    private var weightText: TextView? = null
    private var male: TextView? = null
    private var female: TextView? = null
    private var nextButton: Button? = null
    private var sharedPreferences: SharedPreferences? = null
    private val gson: Gson = Gson()
    private var metricHeight = 0.0
    private var imperialHeight = 0.0
    private var metricWeight = 0.0
    private var imperialWeight = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_info)
        try {
            initVariables()
            personData
            addMeasureSwitchListener()
            addGenderSwitchListener()
            addLoginListeners()
            addDoBListener()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initVariables() {
        firstName = findViewById(R.id.first_name_value)
        lastName = findViewById(R.id.last_name_value)
        height = findViewById(R.id.height)
        weight = findViewById(R.id.weight)
        birthday = findViewById(R.id.dob)
        age = findViewById(R.id.age)
        male = findViewById(R.id.male)
        female = findViewById(R.id.female)
        gender = findViewById(R.id.gender_switch)
        measure = findViewById(R.id.useImperial)
        metric = findViewById(R.id.metric)
        imperial = findViewById(R.id.imperial)
        heightText = findViewById(R.id.height_text)
        weightText = findViewById(R.id.weight_text)
        nextButton = findViewById(R.id.next_btn)
        sharedPreferences = getSharedPreferences(App.PREFS_KEY, MODE_PRIVATE)
    }

    private val personData: Unit
        get() {
            val json = sharedPreferences!!.getString(USER_INFO_KEY, null)
            val userInfo: UserInfo = gson.fromJson(json, UserInfo::class.java)
            if (userInfo != null) drawUI(userInfo)
        }

    private fun drawUI(userInfo: UserInfo) {
        firstName!!.setText(userInfo.firstName)
        lastName!!.setText(userInfo.lastName)
        val savedHeight =
            if (userInfo.useImperial) userInfo.height.toDouble() * CM_TO_FEET_COEFF else userInfo.height.toDouble()
        height!!.setText(String.format(Locale.US, "%.2f", savedHeight))
        val savedWeight =
            if (userInfo.useImperial) userInfo.weight.toDouble() * KG_TO_LB_COEFF else userInfo.weight.toDouble()
        weight!!.setText(String.format(Locale.US, "%.2f", savedWeight))
        birthday!!.setText(userInfo.dob)
        age!!.setText(userInfo.age)
        val genderState = userInfo.gender == FEMALE
        gender!!.setChecked(genderState)
        animateTextForSwitcher(genderState, male, female)
        val imperialState = userInfo.useImperial
        measure!!.setChecked(imperialState)
        animateTextForSwitcher(imperialState, metric, imperial)
    }

    private fun addGenderSwitchListener() {
        gender!!.setOnCheckedChangeListener { _: CompoundButton?, isFemale: Boolean ->
            animateTextForSwitcher(
                isFemale, male, female
            )
        }
    }

    private fun addDoBListener() {
        val yearFormat: String
        val monthFormat: String
        val dayFormat: String
        val date: Date

        //To save and get user data from Shared Preferences (KeyChain analog in iOS)
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK)
        val dateOfBirthFromShared = sharedPreferences!!.getString("DoB", "")

        //setting date for Date picker
        //todo: this should be improved as currently cannot parse the date
        date = if (dateOfBirthFromShared!!.trim { it <= ' ' }.isNotEmpty()) {
            try {
                dateFormatter.parse(dateOfBirthFromShared) as Date
            } catch (e: Exception) {
                e.printStackTrace()
                Date()
            }
        } else {
            Date()
        }
        monthFormat = DateFormat.format("MM", date) as String
        yearFormat = DateFormat.format("yyyy", date) as String
        dayFormat = DateFormat.format("dd", date) as String

        //showing Date Picker to choose date of Birth
        birthday!!.onFocusChangeListener = OnFocusChangeListener { _: View?, focus: Boolean ->
            if (focus) {
                val dialog = DatePickerDialog(
                    this, { _: DatePicker?, year: Int, month: Int, day: Int ->
                        val dateOfBirth = LocalDate.of(year, month + 1, day)
                        birthday!!.setText(dateOfBirth.format(dateFormatter))
                        val ageValue = getAge(year, month + 1, day).toString()
                        age!!.setText(ageValue)
                    }, yearFormat.toInt(), monthFormat.toInt() - 1, dayFormat.toInt()
                )
                dialog.show()
                birthday!!.error = null
            }
        }
    }

    private fun getAge(year: Int, month: Int, dayOfMonth: Int): Int {
        return Period.between(
            LocalDate.of(year, month, dayOfMonth), LocalDate.now()
        ).years
    }

    private fun addMeasureSwitchListener() {
        measure!!.setOnCheckedChangeListener { _: CompoundButton?, isImperial: Boolean ->
            if (isImperial) {
                animateTextForSwitcher(true, metric, imperial)
                heightText?.setText(R.string.height_ft)
                weightText?.setText(R.string.weight_lb)
                switchMeasure(true)
            } else {
                animateTextForSwitcher(false, metric, imperial)
                heightText?.setText(R.string.height_cm)
                weightText?.setText(R.string.weight_kg)
                switchMeasure(false)
            }
        }
    }

    private fun animateTextForSwitcher(
        rightSwitch: Boolean, leftText: TextView?, rightText: TextView?
    ) {
        if (rightSwitch) {
            leftText!!.animate().cancel()
            leftText.setAlpha(0.0f)
            rightText!!.animate().setDuration(ANIM_DELAY.toLong()).alpha(1.0f).start()
        } else {
            rightText!!.animate().cancel()
            rightText.setAlpha(0.0f)
            leftText!!.animate().setDuration(ANIM_DELAY.toLong()).alpha(1.0f).start()
        }
    }

    private fun switchMeasure(rightSwitch: Boolean) {
        val convertedHeight =
            if (rightSwitch) convertHeightToImperial() else convertHeightToMetric()
        val convertedWeight =
            if (rightSwitch) convertWeightToImperial() else convertWeightToMetric()
        height!!.setText(String.format(Locale.US, "%.2f", convertedHeight))
        weight!!.setText(String.format(Locale.US, "%.2f", convertedWeight))
    }

    private fun convertHeightToImperial(): Double {
        val inputValue = getDoubleFromString(height!!.getText().toString())
        if (abs(metricHeight - inputValue) > 0.01) imperialHeight = inputValue * CM_TO_FEET_COEFF
        metricHeight = inputValue
        return imperialHeight
    }

    private fun convertHeightToMetric(): Double {
        val inputValue = getDoubleFromString(height!!.getText().toString())
        if (abs(imperialHeight - inputValue) > 0.01) metricHeight = inputValue / CM_TO_FEET_COEFF
        imperialHeight = inputValue
        return metricHeight
    }

    private fun convertWeightToImperial(): Double {
        val inputValue = getDoubleFromString(weight!!.getText().toString())
        if (abs(metricWeight - inputValue) > 0.01) imperialWeight = inputValue * KG_TO_LB_COEFF
        metricWeight = inputValue
        return imperialWeight
    }

    private fun convertWeightToMetric(): Double {
        val inputValue = getDoubleFromString(weight!!.getText().toString())
        if (abs(imperialWeight - inputValue) > 0.01) metricWeight = inputValue / KG_TO_LB_COEFF
        imperialWeight = inputValue
        return metricWeight
    }

    private fun addLoginListeners() {
        nextButton!!.setOnClickListener { v: View ->
            val intent = Intent(v.context, StartVitalSigns::class.java)
            if (isUserDataValidate(measure!!.isChecked)) {
                val userInfo = UserInfo(
                    firstName!!.getText().toString().trim { it <= ' ' },
                    lastName!!.getText().toString().trim { it <= ' ' },
                    if (measure!!.isChecked) getMetricValue(
                        height, CM_TO_FEET_COEFF
                    ) else height!!.getText().toString(),
                    if (measure!!.isChecked) getMetricValue(
                        weight, KG_TO_LB_COEFF
                    ) else weight!!.getText().toString(),
                    birthday!!.getText().toString(),
                    age!!.getText().toString(),
                    if (gender!!.isChecked) FEMALE else MALE,
                    measure!!.isChecked
                )
                savePersonData(userInfo)
                startActivity(intent)
                finish()
            } else birthday!!.clearFocus()
        }
    }

    private fun getMetricValue(view: EditText?, coeff: Double): String {
        val imperialValue = view!!.getText().toString().toDouble()
        val metricValue = imperialValue / coeff
        return String.format(Locale.US, "%.2f", metricValue)
    }

    private fun isUserDataValidate(metric: Boolean): Boolean {
        val isValidName = checkNames()
        val isValidHeight =
            checkHeightField(metric, height!!.getText().toString().trim { it <= ' ' })
        val isValidWeight =
            checkWeightField(metric, weight!!.getText().toString().trim { it <= ' ' })
        val isValidAge = checkAgeField(age!!.getText().toString().trim { it <= ' ' })
        return isValidName && isValidHeight && isValidWeight && isValidAge
    }

    private fun checkNames(): Boolean {
        var status = true
        val firstNameLength = firstName!!.getText().toString().trim { it <= ' ' }.length
        val secondNameLength = lastName!!.getText().toString().trim { it <= ' ' }.length
        if (firstNameLength < MIN_LENGTH) {
            firstName!!.error = getString(R.string.not_valid_name)
            status = false
        }
        if (secondNameLength < MIN_LENGTH) {
            lastName!!.error = getString(R.string.not_valid_name)
            status = false
        }
        return status
    }

    private fun checkHeightField(metric: Boolean, data: String): Boolean {
        val num = getDoubleFromString(data)
        val minHeight: Double = if (metric) IMPERIAL_HEIGHT_LOW_LIMIT else METRIC_HEIGHT_LOW_LIMIT
        val maxHeight: Double = if (metric) IMPERIAL_HEIGHT_TOP_LIMIT else METRIC_HEIGHT_TOP_LIMIT
        if (num < minHeight || num > maxHeight) {
            height!!.error = getString(R.string.not_valid_height) + minHeight + "-" + maxHeight
            return false
        }
        return true
    }

    private fun checkWeightField(metric: Boolean, data: String): Boolean {
        val num = getDoubleFromString(data)
        val minWeight: Double = if (metric) IMPERIAL_WEIGHT_LOW_LIMIT else METRIC_WEIGHT_LOW_LIMIT
        val maxWeight: Double = if (metric) IMPERIAL_WEIGHT_TOP_LIMIT else METRIC_WEIGHT_TOP_LIMIT
        if (num < minWeight || num > maxWeight) {
            weight!!.error = getString(R.string.not_valid_weight) + minWeight + "-" + maxWeight
            return false
        }
        return true
    }

    private fun checkAgeField(data: String): Boolean {
        val num = if (data.isEmpty()) 0.0 else data.toDouble()
        if (num < MIN_AGE || num > MAX_AGE) {
            birthday!!.error =
                (getString(R.string.not_valid_age) + MIN_AGE) + "-" + MAX_AGE
            return false
        }
        return true
    }

    private fun savePersonData(userInfo: UserInfo) {
        val json: String = gson.toJson(userInfo)
        sharedPreferences!!.edit().putString(USER_INFO_KEY, json).apply()
    }

    private fun getDoubleFromString(str: String): Double {
        return if (str.isEmpty() || str == ".") 0.0 else str.toDouble()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(this@PatientInfoActivity, AboutApp::class.java)
        startActivity(i)
        finish()
    }

    companion object {
        const val USER_INFO_KEY = "user_info_key"
        private const val ANIM_DELAY = 400
        private const val MALE = "1"
        private const val FEMALE = "2"
        private const val MIN_LENGTH = 2
    }
}