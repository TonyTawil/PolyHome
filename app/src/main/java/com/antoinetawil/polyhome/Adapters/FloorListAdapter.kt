package com.antoinetawil.polyhome.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.R

class FloorListAdapter(
    private val floors: List<String>,
    private val peripheralType: String, // Added to handle icons
    private val onFloorSelected: (String) -> Unit
) : RecyclerView.Adapter<FloorListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val floorTextView: TextView = view.findViewById(R.id.typeTextView)
        val typeIcon: ImageView = view.findViewById(R.id.typeIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.peripheral_type_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val floor = floors[position]

        // Log the floor and peripheral type for debugging
        Log.d("FloorListAdapter", "Binding floor: $floor with peripheralType: $peripheralType")

        // Update the icon based on the peripheral type
        val iconResId = when (peripheralType) {
            "Light" -> R.drawable.ic_light_off
            "Shutter" -> R.drawable.ic_shutter
            "GarageDoor" -> R.drawable.ic_garage
            else -> R.drawable.ic_light_off // Default icon
        }

        // Log the resolved icon resource
        Log.d("FloorListAdapter", "Resolved icon for peripheralType '$peripheralType': $iconResId")

        holder.typeIcon.setImageResource(iconResId)
        holder.floorTextView.text = floor

        holder.itemView.setOnClickListener {
            Log.d("FloorListAdapter", "Floor selected: $floor")
            onFloorSelected(floor)
        }
    }

    override fun getItemCount(): Int = floors.size
}
