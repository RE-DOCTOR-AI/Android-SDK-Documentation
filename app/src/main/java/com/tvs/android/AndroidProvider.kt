package com.tvs.android

import android.content.SharedPreferences
import com.tvs.api.UserParametersProvider
import com.tvs.model.User
import com.tvs.model.UserInfo
import com.tvs.model.UserParameters
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

//SDK required: Class to provide SDK with user parameters
//-->
class AndroidProvider(
    private val prefs: SharedPreferences,
): UserParametersProvider {
    private val USER_INFO_KEY = "user_info_key"

    override fun getUserParameters(): User? {
        val json = prefs.getString(USER_INFO_KEY, "")
        if (json.isNullOrEmpty()) {
            return null
        }

        val user: UserInfo = Json.decodeFromString(json)

        return UserParameters(
            height = user.height.toDouble(),
            weight = user.weight.toDouble(),
            age = user.age.toInt(),
            gen = user.gender.toInt(),
        )
    }
}
//<--