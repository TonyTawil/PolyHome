package com.antoinetawil.polyhome.Adapters

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
        val typeIcon: ImageView = view.findViewById(R.id.typeIcon) // Left icon
        val typeTextView: TextView = view.findViewById(R.id.typeTextView) // Type text
        val arrowIcon: ImageView = view.findViewById(R.id.arrowIcon) // Right arrow icon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.peripheral_type_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val type = types[position]

        // Update the icon based on the peripheral type
        val iconResId = when (type.lowercase()) {
            "light" -> R.drawable.ic_light_on
            "rolling shutter" -> R.drawable.ic_shutter
            "garage door" -> R.drawable.ic_garage
            else -> R.drawable.ic_light_on
        }
        holder.typeIcon.setImageResource(iconResId)

        // Set the type text
        holder.typeTextView.text = type.capitalize()

        // Set the onClickListener for the item
        holder.itemView.setOnClickListener { onTypeSelected(type) }
    }

    override fun getItemCount(): Int = types.size
}