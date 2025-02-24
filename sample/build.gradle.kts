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
    compileSdk = 34

    defaultConfig {
        applicationId = "com.amazon.sidewalk.sample"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
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
    namespace = "com.amazon.sidewalk.sample"
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to "*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Nordic BLE libraries
    implementation("no.nordicsemi.android.support.v18:scanner:1.6.0")
    implementation("no.nordicsemi.android:ble-ktx:2.7.5")

    // Curve25519 and AES-CMAC
    implementation("org.bouncycastle:bcprov-jdk15to18:1.78.1")

    // Amazon Sidewalk SDK
    implementation("com.amazon.sidewalk:mobile-sdk:1.3.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")

    // Navigation Architecture Component-Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Hilt testing
    testImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptTest("com.google.dagger:hilt-android-compiler:2.52")

    // To use the androidx.test.core APIs
    testImplementation("androidx.test:core:1.6.1")
    // Kotlin extensions for androidx.test.core
    testImplementation("androidx.test:core-ktx:1.6.1")

    // To use the androidx.test.espresso
    testImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // To use the JUnit Extension APIs
    testImplementation("androidx.test.ext:junit:1.2.1")
    // Kotlin extensions for androidx.test.ext.junit
    testImplementation("androidx.test.ext:junit-ktx:1.2.1")

    // To use the androidx.test.runner APIs
    testImplementation("androidx.test:runner:1.6.2")

    // Navigation Testing
    testImplementation("androidx.navigation:navigation-testing:2.7.7")

    // Robolectric
    testImplementation("org.robolectric:robolectric:4.13")
}
