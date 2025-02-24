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

package com.amazon.sidewalk.sample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.sidewalk.message.SidewalkMessage
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.asHexUpper
import com.amazon.sidewalk.sample.viewmodel.MessageType
import java.text.SimpleDateFormat
import java.util.Date

class SubscribeAdapter : ListAdapter<Pair<SidewalkMessage, MessageType>, SubscribeAdapter.ViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.subscribe_content, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val message = getItem(position)
        val timeStamp: String =
            SimpleDateFormat("HH:mm:ss").format(Date())
        val subscribeMessage =
            "$timeStamp: ${message.second.type} - ${message.first.message.asHexUpper}"
        holder.messageView.text = subscribeMessage
    }

    class ViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val messageView = itemView.findViewById(R.id.contentInfo) as TextView
    }

    object DiffCallback : DiffUtil.ItemCallback<Pair<SidewalkMessage, MessageType>>() {
        override fun areItemsTheSame(
            oldItem: Pair<SidewalkMessage, MessageType>,
            newItem: Pair<SidewalkMessage, MessageType>,
        ): Boolean =
            oldItem.first == newItem.first &&
                oldItem.second == newItem.second

        override fun areContentsTheSame(
            oldItem: Pair<SidewalkMessage, MessageType>,
            newItem: Pair<SidewalkMessage, MessageType>,
        ): Boolean =
            oldItem.first.message.asHexUpper == newItem.first.message.asHexUpper &&
                oldItem.first.descriptor.type == newItem.first.descriptor.type &&
                oldItem.first.descriptor.id == newItem.first.descriptor.id &&
                oldItem.second.type == newItem.second.type
    }
}
