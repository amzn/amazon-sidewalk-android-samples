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

package com.amazon.sidewalk.sample.fragment

import androidx.appcompat.widget.MenuPopupWindow.MenuDropDownListView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.sidewalk.Sidewalk
import com.amazon.sidewalk.device.BeaconInfo
import com.amazon.sidewalk.device.SidewalkDevice
import com.amazon.sidewalk.result.RegisterResult
import com.amazon.sidewalk.result.SidewalkResult
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.data.ScanRepository
import com.amazon.sidewalk.sample.di.SidewalkAuthModule
import com.amazon.sidewalk.sample.launchFragmentInHiltContainer
import com.amazon.sidewalk.sample.utils.withRecyclerViewAtPosition
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.instanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@UninstallModules(SidewalkAuthModule::class)
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class ScanFragmentTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private val sidewalk = mockk<Sidewalk> {
        every { scan() } returns flowOf(
            SidewalkResult.Success(
                mockk {
                    every { name } returns "nordic-dk1"
                    every { address } returns "00:11:22:33:44:55"
                    every { endpointId } returns "BAC688BC1F"
                    every { rssi } returns -10
                    every { beaconInfo } returns mockk {
                        every { deviceMode } returns BeaconInfo.DeviceMode.OOBE
                        every { batteryLevel } returns BeaconInfo.BatteryLevel.Normal
                    }
                }
            ),
            SidewalkResult.Success(
                mockk {
                    every { name } returns "fetch2"
                    every { address } returns "FF:EE:DD:CC:BB:AA"
                    every { endpointId } returns "BFFFFFFB88"
                    every { rssi } returns -13
                    every { beaconInfo } returns mockk {
                        every { deviceMode } returns BeaconInfo.DeviceMode.Normal
                        every { batteryLevel } returns BeaconInfo.BatteryLevel.Low
                    }
                }
            )
        )
        every { register(any<SidewalkDevice>()) } answers {
            val device = args[0] as SidewalkDevice
            flowOf(
                RegisterResult.Success(
                    wirelessDeviceId = "WirelessDeviceId",
                    sidewalkId = device.endpointId
                )
            )
        }
    }

    @BindValue
    @JvmField
    val scanRepository: ScanRepository = ScanRepository(sidewalk, Dispatchers.IO)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `Launch ScanFragment, then scanning is triggered automatically`() {
        launchFragmentInHiltContainer<ScanFragment> {
            onView(withRecyclerViewAtPosition(R.id.list, 0, R.id.device_section)).check(
                matches(withText(getString(R.string.unregistered_devices).uppercase()))
            )
            onView(withRecyclerViewAtPosition(R.id.list, 1, R.id.deviceInfo)).check(
                matches(withText("nordic-dk1 BAC688BC1F -10"))
            )
            onView(withRecyclerViewAtPosition(R.id.list, 2, R.id.device_section)).check(
                matches(withText(getString(R.string.registered_devices).uppercase()))
            )
            onView(withRecyclerViewAtPosition(R.id.list, 3, R.id.deviceInfo)).check(
                matches(withText("fetch2 BFFFFFFB88 -13"))
            )
        }
    }

    @Test
    fun `Launch ScanFragment and register an OOBE device`() {
        launchFragmentInHiltContainer<ScanFragment> {
            onView(withRecyclerViewAtPosition(R.id.list, 1, R.id.deviceInfo)).perform(click())

            onData(anything())
                .inRoot(withDecorView(hasFocus()))
                .inAdapterView(instanceOf(MenuDropDownListView::class.java))
                .atPosition(0) // register
                .perform(click())

            val dialog = ShadowDialog.getLatestDialog()
            assertTrue(dialog.isShowing)
            onView(withSubstring("Registration for BAC688BC1F succeeded with"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun `Launch ScanFragment and establish a D2D connect`() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        launchFragmentInHiltContainer<ScanFragment> {
            // Set the graph on the TestNavHostController
            navController.setGraph(R.navigation.nav_graph)
            // Make the NavController available via the findNavController() APIs
            Navigation.setViewNavController(requireView(), navController)

            onView(withRecyclerViewAtPosition(R.id.list, 1, R.id.deviceInfo)).perform(click())

            onData(anything())
                .inRoot(withDecorView(hasFocus()))
                .inAdapterView(instanceOf(MenuDropDownListView::class.java))
                .atPosition(1) // secure connect
                .perform(click())

            val currentDestination = navController.currentDestination
            assertNotNull(currentDestination)
            assertEquals(R.id.connectionViewFragment, currentDestination.id)
        }
    }
}
