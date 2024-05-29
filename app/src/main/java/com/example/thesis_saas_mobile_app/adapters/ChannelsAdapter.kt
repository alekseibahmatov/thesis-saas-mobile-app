package com.example.thesis_saas_mobile_app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

data class Channel(val channelName: String, val channelId: String)
class ChannelsAdapter(context: Context, channels: MutableList<Channel>): ArrayAdapter<Channel>(context, android.R.layout.simple_spinner_item, channels) {
    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_item, parent, false)
        val channel = getItem(position)
        (view as TextView).text = channel?.channelName
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        val channel = getItem(position)
        (view as TextView).text = channel?.channelName
        return view
    }
}