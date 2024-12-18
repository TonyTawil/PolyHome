package com.antoinetawil.polyhome.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.R

class HouseUsersAdapter(
        private val users: MutableList<HouseUser>,
        private val isOwner: Boolean,
        private val onRemoveUser: (String) -> Unit
) : RecyclerView.Adapter<HouseUsersAdapter.UserViewHolder>() {

    data class HouseUser(val userLogin: String, val owner: Int)

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userLoginText: TextView = view.findViewById(R.id.userLoginText)
        val removeButton: ImageButton = view.findViewById(R.id.removeUserButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view =
                LayoutInflater.from(parent.context).inflate(R.layout.house_user_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.userLoginText.text = user.userLogin

        // Only show remove button if current user is owner and the user being displayed is not the
        // owner
        holder.removeButton.visibility = if (isOwner && user.owner == 0) View.VISIBLE else View.GONE

        holder.removeButton.setOnClickListener { onRemoveUser(user.userLogin) }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<HouseUser>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun addUser(user: HouseUser) {
        users.add(user)
        notifyItemInserted(users.size - 1)
    }

    fun removeUser(userLogin: String) {
        val position = users.indexOfFirst { it.userLogin == userLogin }
        if (position != -1) {
            users.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
