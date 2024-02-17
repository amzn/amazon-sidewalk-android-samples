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

package com.amazon.sidewalk.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.DialogFragment

class DeregisterDialog private constructor() : DialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): DeregisterDialog {
            return DeregisterDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fargment_deregister_dialog, container, false)
        dialog?.setCancelable(false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<AppCompatButton>(R.id.btn_positive).setOnClickListener {
            val input: EditText = view.findViewById(R.id.smsn)
            val smsn = input.text.toString()
            if (smsn.isEmpty()) {
                input.error = resources.getString(R.string.deregister_alert_msg)
                input.requestFocus()
            } else {
                if (parentFragment is IDeregisterOnClickListener) {
                    (parentFragment as IDeregisterOnClickListener).onDeregisterClick(smsn)
                } else if (activity is IDeregisterOnClickListener) {
                    (activity as IDeregisterOnClickListener).onDeregisterClick(smsn)
                }
                dismiss()
            }
        }
        view.findViewById<AppCompatButton>(R.id.btn_negative).setOnClickListener {
            dismiss()
        }
    }
}
