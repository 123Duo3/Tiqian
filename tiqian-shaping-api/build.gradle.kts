plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":tiqian-core"))
            api(project(":tiqian-font"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
