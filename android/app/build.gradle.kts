plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val frontendDir = rootProject.projectDir.resolve("../frontend")

val installFrontendDependencies by tasks.registering(Exec::class) {
    workingDir(frontendDir)
    commandLine("npm", "ci")
    inputs.files(frontendDir.resolve("package.json"), frontendDir.resolve("package-lock.json"))
    outputs.dir(frontendDir.resolve("node_modules"))
}

val buildMobileFrontend by tasks.registering(Exec::class) {
    dependsOn(installFrontendDependencies)
    workingDir(frontendDir)
    commandLine(
        "npm", "run", "build", "--",
        "--outDir", "../android/app/src/main/assets/www",
        "--base", "./"
    )
    inputs.dir(frontendDir.resolve("src"))
    inputs.file(frontendDir.resolve("index.html"))
    outputs.dir(projectDir.resolve("src/main/assets/www"))
}

tasks.named("preBuild") {
    dependsOn(buildMobileFrontend)
}

android {
    namespace = "com.muhammed.jarvismobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.muhammed.jarvismobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.13.0")
}
