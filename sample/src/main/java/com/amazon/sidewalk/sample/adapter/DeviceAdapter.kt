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

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.amazon.sidewalk.device.BeaconInfo
import com.amazon.sidewalk.device.SidewalkDevice
import com.amazon.sidewalk.sample.R

class DeviceAdapter(
    val action: DeviceAdapter.(SidewalkDevice, Int) -> Unit,
) : RecyclerView.Adapter<DeviceAdapter.DeviceHolder>() {
    private var dataSet = mutableListOf<SidewalkDevice>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DeviceHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceHolder(itemView).also { holder ->
            itemView.setOnClickListener {
                val popup = PopupMenu(parent.context, itemView, Gravity.START)
                val device = dataSet[holder.bindingAdapterPosition]
                when (device.beaconInfo.deviceMode) {
                    BeaconInfo.DeviceMode.Normal -> popup.inflate(R.menu.options_normal)
                    BeaconInfo.DeviceMode.OOBE -> popup.inflate(R.menu.options_oobe)
                    else -> return@setOnClickListener
                }
                popup.setOnMenuItemClickListener {
                    action(device, it.itemId)
                    true
                }
                popup.show()
            }
        }
    }

    override fun onBindViewHolder(
        holder: DeviceHolder,
        position: Int,
    ) {
        val sidewalkDevice = dataSet[position]
        holder.nameView.text =
            holder.itemView.context.getString(
                R.string.device_information,
                sidewalkDevice.name,
                sidewalkDevice.endpointId,
                sidewalkDevice.rssi,
            )
    }

    override fun getItemCount() = dataSet.size

    fun append(sidewalkDevice: SidewalkDevice) {
        val index = dataSet.indexOfFirst { it.endpointId == sidewalkDevice.endpointId }
        if (index == -1) {
            dataSet.add(sidewalkDevice)
            notifyItemInserted(dataSet.size - 1)
        } else {
            dataSet[index] = sidewalkDevice
            notifyItemChanged(index)
        }
    }

    fun reset() {
        dataSet.clear()
        notifyDataSetChanged()
    }

    class DeviceHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val nameView = itemView.findViewById(R.id.deviceInfo) as TextView
    }
}
