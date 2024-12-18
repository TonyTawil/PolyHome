package com.antoinetawil.polyhome.Adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Models.House
import com.antoinetawil.polyhome.R

class HouseListAdapter(
        private val houseList: List<House>,
        private val context: Context,
        private val onManagePermission: (house: House, view: View) -> Unit,
        private val onHouseSelected: (houseId: Int) -> Unit
) : RecyclerView.Adapter<HouseListAdapter.HouseViewHolder>() {

    companion object {
        private const val TAG = "HouseListAdapter"
    }

    class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val houseIdTextView: TextView = itemView.findViewById(R.id.houseIdValueTextView)
        val ownerStatusTextView: TextView = itemView.findViewById(R.id.houseOwnerStatusTextView)
        val managePermissionButton: ImageButton = itemView.findViewById(R.id.managePermissionButton)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrowIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view =
                LayoutInflater.from(parent.context).inflate(R.layout.house_list_item, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val house = houseList[position]

        holder.houseIdTextView.text = context.getString(R.string.house_id_label, house.houseId)
        holder.ownerStatusTextView.text =
                if (house.owner) {
                    context.getString(R.string.owner)
                } else {
                    context.getString(R.string.guest)
                }

        Log.d(TAG, "Binding house: ${house.houseId}, Owner: ${house.owner}")

        holder.itemView.setOnClickListener {
            Log.d(TAG, "House selected: ${house.houseId}")
            onHouseSelected(house.houseId)
        }

        holder.managePermissionButton.visibility = if (house.owner) View.VISIBLE else View.GONE

        holder.managePermissionButton.setOnClickListener {
            Log.d(TAG, "Manage permission clicked for house: ${house.houseId}")
            onManagePermission(house, holder.managePermissionButton)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "House list size: ${houseList.size}")
        return houseList.size
    }
}
