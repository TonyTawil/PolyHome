package com.antoinetawil.polyhome.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.R

class FloorListAdapter(
    private val floors: List<String>,
    private val onFloorSelected: (String) -> Unit
) : RecyclerView.Adapter<FloorListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val floorTextView: TextView = view.findViewById(R.id.typeTextView)
        val arrowIcon: ImageView = view.findViewById(R.id.arrowIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.peripheral_type_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val floor = floors[position]
        holder.floorTextView.text = floor
        holder.itemView.setOnClickListener { onFloorSelected(floor) }
    }

    override fun getItemCount(): Int = floors.size
}
