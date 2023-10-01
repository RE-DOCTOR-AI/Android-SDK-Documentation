package com.tvs.android

import com.tvs.api.UserParametersProvider
import com.tvs.model.User
import com.tvs.model.UserParameters

//SDK required: Class to provide SDK with user parameters
//-->
class AndroidProvider: UserParametersProvider {

    override fun getUserParameters(): User {
        return UserParameters(
            height = 180.0,
            weight = 74.0,
            age = 39,
            gen = 1,
        )
    }
}
//<--