plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvm()
    android {
        namespace = "org.tiqian.linebreak"
        compileSdk = 36
        minSdk = 31
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":tiqian-core"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
