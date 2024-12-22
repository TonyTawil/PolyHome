package com.antoinetawil.polyhome.Adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.R

class PeripheralTypeAdapter(
    private val types: List<String>,
    private val onTypeSelected: (String) -> Unit
) : RecyclerView.Adapter<PeripheralTypeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val typeIcon: ImageView = view.findViewById(R.id.typeIcon)
        val typeTextView: TextView = view.findViewById(R.id.typeTextView)
        val arrowIcon: ImageView = view.findViewById(R.id.arrowIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.peripheral_type_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val type = types[position]
        val context = holder.itemView.context

        Log.d("PeripheralTypeAdapter", "Peripheral type at position $position: $type")

        val iconResId = when (type) {
            "Light" -> R.drawable.ic_light_on
            "Shutter" -> R.drawable.ic_shutter
            "GarageDoor" -> R.drawable.ic_garage
            else -> R.drawable.ic_light_off
        }

        holder.typeIcon.setImageResource(iconResId)

        val translatedType = when (type) {
            "Light" -> context.getString(R.string.light)
            "Shutter" -> context.getString(R.string.shutter)
            "GarageDoor" -> context.getString(R.string.garage_door)
            else -> type.replaceFirstChar { it.uppercase() }
        }

        holder.typeTextView.text = translatedType

        holder.itemView.setOnClickListener { onTypeSelected(type) }
    }

    override fun getItemCount(): Int = types.size
}
