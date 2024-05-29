package com.example.thesis_saas_mobile_app.adapters

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class BluetoothDeviceAdapter(context: Context, devices: MutableList<BluetoothDevice>) : ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_spinner_item, devices) {
    private val items = mutableListOf<BluetoothDevice?>().apply {
        add(null) // Placeholder
        addAll(devices)
    }

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): BluetoothDevice? {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_item, parent, false)
        val device = getItem(position)
        (view as TextView).text = device?.name ?: device?.address ?: "Select a device"
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        val device = getItem(position)
        (view as TextView).text = device?.name ?: device?.address ?: "Select a device"
        return view
    }

    fun updateDevices(newDevices: List<BluetoothDevice>) {
        items.clear()
        items.add(null) // Placeholder
        items.addAll(newDevices)
        notifyDataSetChanged()
    }
}

