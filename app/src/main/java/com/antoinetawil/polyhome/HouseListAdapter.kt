package com.antoinetawil.polyhome

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView


class HouseListAdapter(
    private val houseList: List<House>,
    private val context: Context,
    private val onManagePermission: (houseId: Int, isOwner: Boolean) -> Unit,
    private val onHouseSelected: (houseId: Int) -> Unit
) : RecyclerView.Adapter<HouseListAdapter.HouseViewHolder>() {

    class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val houseIdTextView: TextView = itemView.findViewById(R.id.houseIdValueTextView)
        val ownerTextView: TextView = itemView.findViewById(R.id.houseIdTextView)
        val managePermissionButton: Button = itemView.findViewById(R.id.managePermissionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.house_list_item, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val house = houseList[position]
        holder.houseIdTextView.text = "ID: ${house.houseId}"
        holder.ownerTextView.text = if (house.owner) "Owner" else "Guest"

        // Navigate to peripherals when the item is clicked
        holder.itemView.setOnClickListener {
            onHouseSelected(house.houseId)
        }

        // Show permission popup when the button is clicked
        holder.managePermissionButton.setOnClickListener {
            if (house.owner) {
                onManagePermission(house.houseId, house.owner)
            } else {
                Toast.makeText(context, "You do not have permission to manage this house", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = houseList.size
}
