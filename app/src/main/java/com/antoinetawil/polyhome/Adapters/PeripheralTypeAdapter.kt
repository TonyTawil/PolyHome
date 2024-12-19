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
        val context = holder.itemView.context

        // Log the type to understand why the wrong icon might be shown
        Log.d("PeripheralTypeAdapter", "Peripheral type at position $position: $type")

        // Update the icon based on the peripheral type
        val iconResId = when (type) {
            "Light" -> R.drawable.ic_light_on // Use correct icon for "light"
            "Shutter" -> R.drawable.ic_shutter // Use correct icon for "rolling shutter"
            "GarageDoor" -> R.drawable.ic_garage // Use correct icon for "garage door"
            else -> R.drawable.ic_light_off // Use a default icon for unknown types
        }

        holder.typeIcon.setImageResource(iconResId)

        // Translate the type text based on the current locale
        val translatedType = when (type) {
            "Light" -> context.getString(R.string.light)
            "Shutter" -> context.getString(R.string.shutter)
            "GarageDoor" -> context.getString(R.string.garage_door)
            else -> type.replaceFirstChar { it.uppercase() } // Fallback to raw type with capitalization
        }

        holder.typeTextView.text = translatedType

        // Set the onClickListener for the item
        holder.itemView.setOnClickListener { onTypeSelected(type) }
    }

    override fun getItemCount(): Int = types.size
}
