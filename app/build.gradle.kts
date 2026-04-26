import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.questboard.widget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.questboard.widget"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "2.0"

        buildConfigField("String", "NOTION_TOKEN", "\"${localProps["notion.token"]}\"")
        buildConfigField("String", "NOTION_DATABASE_ID", "\"${localProps["notion.database_id"]}\"")
        buildConfigField("String", "NOTION_STATUS_VALUE", "\"${localProps["notion.status_value"]}\"")
        buildConfigField("String", "NOTION_GOALS_DATABASE_ID", "\"${localProps["notion.goals_database_id"]}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
}
