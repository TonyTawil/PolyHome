package com.antoinetawil.polyhome.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.R

class HouseListAdapter(
    private val houseList: List<House>,
    private val context: Context,
    private val onManagePermission: (houseId: Int, view: View) -> Unit,
    private val onHouseSelected: (houseId: Int) -> Unit
) : RecyclerView.Adapter<HouseListAdapter.HouseViewHolder>() {

    class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val houseIdTextView: TextView = itemView.findViewById(R.id.houseIdValueTextView)
        val ownerTextView: TextView = itemView.findViewById(R.id.houseIdTextView)
        val managePermissionButton: ImageButton = itemView.findViewById(R.id.managePermissionButton)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrowIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.house_list_item, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val house = houseList[position]
        holder.houseIdTextView.text = if (house.owner) "Owner" else "Guest"
        holder.houseIdTextView.text = "ID: ${house.houseId}"

        holder.itemView.setOnClickListener {
            onHouseSelected(house.houseId)
        }

        holder.managePermissionButton.setOnClickListener {
            onManagePermission(house.houseId, holder.managePermissionButton)
        }
    }

    override fun getItemCount(): Int = houseList.size
}
