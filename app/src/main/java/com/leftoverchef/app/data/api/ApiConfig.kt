package com.leftoverchef.app.data.api

import com.leftoverchef.app.BuildConfig

object ApiConfig {
    const val API_KEY = BuildConfig.SPOONACULAR_API_KEY
    const val BASE_URL = "https://api.spoonacular.com/"
    const val SENDGRID_API_KEY = BuildConfig.SENDGRID_API_KEY
    const val SENDGRID_FROM_EMAIL = BuildConfig.SENDGRID_FROM_EMAIL
    const val SENDGRID_FROM_NAME = "LeftoverChef"
}
