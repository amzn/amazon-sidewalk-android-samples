/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.android.build.api.dsl.TestOptions

plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs")
}

apply {
    from("$rootDir/gradle/ktlint.gradle")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.amazon.sidewalk.sample"
        minSdk = 23
        targetSdk = 31
        versionCode = 1
        versionName = "1.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        missingDimensionStrategy("log", "internal")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions.add("testing")
    productFlavors {
        register("hiltTest") {
            dimension = "testing"
        }
    }

    sourceSets {
        named("hiltTest") {
            java.srcDirs("src/hiltTest/java")
        }
    }

    testOptions {
        @Suppress("UNCHECKED_CAST")
        val action = rootProject.ext["testOptions"] as TestOptions.() -> Unit
        action()
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to "*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1")

    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.activity:activity-ktx:1.5.1")
    implementation("androidx.fragment:fragment-ktx:1.5.3")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.8.0")

    // Nordic BLE libraries
    implementation("no.nordicsemi.android.support.v18:scanner:1.6.0")
    implementation("no.nordicsemi.android:ble-ktx:2.3.1")

    // Curve25519 and AES-CMAC
    implementation("org.bouncycastle:bcprov-jdk15to18:1.72")

    // Amazon Sidewalk SDK
    implementation("com.amazon.sidewalk:mobile-sdk:1.2.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-compiler:2.44")
    implementation("androidx.hilt:hilt-navigation-fragment:1.0.0")

    // Navigation Architecture Component-Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.10.6")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.21")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.1")

    // Hilt testing
    testImplementation("com.google.dagger:hilt-android-testing:2.40.5")
    kaptTest("com.google.dagger:hilt-android-compiler:2.40.5")

    // To use the androidx.test.core APIs
    testImplementation("androidx.test:core:1.4.0")
    // Kotlin extensions for androidx.test.core
    testImplementation("androidx.test:core-ktx:1.4.0")

    // To use the androidx.test.espresso
    testImplementation("androidx.test.espresso:espresso-core:3.4.0")

    // To use the JUnit Extension APIs
    testImplementation("androidx.test.ext:junit:1.1.3")
    // Kotlin extensions for androidx.test.ext.junit
    testImplementation("androidx.test.ext:junit-ktx:1.1.3")

    // To use the androidx.test.runner APIs
    testImplementation("androidx.test:runner:1.4.0")

    // Navigation Testing
    testImplementation("androidx.navigation:navigation-testing:2.5.2")

    // Robolectric
    testImplementation("org.robolectric:robolectric:4.8")
}
