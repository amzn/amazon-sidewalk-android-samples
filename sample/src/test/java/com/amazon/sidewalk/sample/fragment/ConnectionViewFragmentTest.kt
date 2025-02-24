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

import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotClickable
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.sidewalk.Sidewalk
import com.amazon.sidewalk.SidewalkConnection
import com.amazon.sidewalk.message.MessageDescriptor
import com.amazon.sidewalk.message.MessageType
import com.amazon.sidewalk.message.SidewalkMessage
import com.amazon.sidewalk.result.RegistrationDetail
import com.amazon.sidewalk.result.SidewalkResult
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.data.ConnectionRepository
import com.amazon.sidewalk.sample.di.SidewalkAuthModule
import com.amazon.sidewalk.sample.launchFragmentInHiltContainer
import com.amazon.sidewalk.sample.utils.withRecyclerViewAtPosition
import com.amazon.sidewalk.sample.viewmodel.ConnectionViewModel
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@UninstallModules(SidewalkAuthModule::class)
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class ConnectionViewFragmentTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private val connection =
        mockk<SidewalkConnection>(relaxed = true) {
            every { subscribe() } returns
                flowOf(
                    SidewalkResult.Success(
                        SidewalkMessage(
                            byteArrayOf(0x01, 0x02, 0x03),
                            MessageDescriptor(MessageType.Notify, 1),
                        ),
                    ),
                    SidewalkResult.Success(
                        SidewalkMessage(
                            byteArrayOf(0x11, 0x12, 0x13, 0x14),
                            MessageDescriptor(MessageType.Notify, 2),
                        ),
                    ),
                )
            coEvery { write(any()) } returns SidewalkResult.Success(Unit)
        }
    private val sidewalk =
        mockk<Sidewalk> {
            coEvery { secureConnectDevice(any<String>()) } returns SidewalkResult.Success(connection)
            coEvery { registerDevice(any<SidewalkConnection>()) } returns
                SidewalkResult.Success(
                    RegistrationDetail.RegistrationSucceeded,
                )
        }

    @BindValue
    @JvmField
    val connectionRepository: ConnectionRepository = ConnectionRepository(sidewalk, Dispatchers.IO)

    private val smsn = "A1A5BD7A90"

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `Launch ConnectionViewFragment with unregistered device, then views are initialized`() {
        launchFragmentInHiltContainer<ConnectionViewFragment>(
            fragmentArgs = bundleOf(ConnectionViewModel.ARG_SMSN to smsn),
        ) {
            onView(withId(R.id.messageHeader)).check(
                matches(withText(getString(R.string.subscribe_msg_header, smsn))),
            )
            onView(withId(R.id.registerButton)).apply {
                check(matches(isEnabled()))
                check(matches(isClickable()))
            }
            onView(withRecyclerViewAtPosition(R.id.subscribeList, 0, R.id.contentInfo)).check(
                matches(withSubstring("010203")),
            )
            onView(withRecyclerViewAtPosition(R.id.subscribeList, 1, R.id.contentInfo)).check(
                matches(withSubstring("11121314")),
            )
        }
    }

    @Test
    fun `Launch ConnectionViewFragment, then write a message`() {
        launchFragmentInHiltContainer<ConnectionViewFragment>(
            fragmentArgs = bundleOf(ConnectionViewModel.ARG_SMSN to smsn),
        ) {
            onView(withId(R.id.editText)).perform(typeText("sidewalk"))
            onView(withId(R.id.writeButton)).perform(click())
            onView(withId(R.id.editText)).check(matches(withText("")))
        }
    }

    @Test
    fun `Launch ConnectionViewFragment with unregistered device, then register it through connection`() {
        launchFragmentInHiltContainer<ConnectionViewFragment>(
            fragmentArgs = bundleOf(ConnectionViewModel.ARG_SMSN to smsn),
        ) {
            onView(withId(R.id.registerButton)).apply {
                check(matches(isEnabled()))
                check(matches(isClickable()))
            }
            onView(withId(R.id.registerButton)).perform(click())

            val dialog = ShadowDialog.getLatestDialog()
            assertTrue(dialog.isShowing)
            onView(withSubstring("Register succeeded after establishing a secure channel"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun `Launch ConnectionViewFragment with registered device, then register button is disabled`() {
        launchFragmentInHiltContainer<ConnectionViewFragment>(
            fragmentArgs =
                bundleOf(
                    ConnectionViewModel.ARG_SMSN to smsn,
                    ConnectionViewModel.ARG_REGISTERED to true,
                ),
        ) {
            onView(withId(R.id.registerButton)).apply {
                check(matches(isNotEnabled()))
                check(matches(isNotClickable()))
            }
        }
    }
}
