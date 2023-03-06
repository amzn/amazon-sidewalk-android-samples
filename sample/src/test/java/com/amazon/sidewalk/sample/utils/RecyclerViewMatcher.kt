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

package com.amazon.sidewalk.sample.utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher

class RecyclerViewMatcher(
    private val resId: Int,
    private val childPosition: Int,
    private val childResId: Int
) : TypeSafeDiagnosingMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("RecyclerView(id=$resId)'s child at position $childPosition")
    }

    override fun matchesSafely(view: View, mismatchDescription: Description): Boolean {
        val recyclerView = view.rootView.findViewById<View>(resId)
        if (recyclerView == null || recyclerView !is RecyclerView) {
            mismatchDescription.appendText("RecyclerView isn't founded")
            return false
        }

        val viewHolder = recyclerView.findViewHolderForAdapterPosition(childPosition)
        if (viewHolder == null) {
            mismatchDescription.appendText("Item ViewHolder is null")
            return false
        }

        val childView = viewHolder.itemView
        return if (childResId == -1) {
            view === childView
        } else {
            val targetView = childView.findViewById<View>(childResId)
            view === targetView
        }
    }
}

fun withRecyclerViewAtPosition(
    recyclerViewResId: Int,
    childPosition: Int,
    childResId: Int = -1
): RecyclerViewMatcher {
    return RecyclerViewMatcher(recyclerViewResId, childPosition, childResId)
}
