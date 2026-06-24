plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvm()
    android {
        namespace = "org.tiqian.clreq"
        compileSdk = 36
        minSdk = 31
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":tiqian-core"))
            api(project(":tiqian-linebreak"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
