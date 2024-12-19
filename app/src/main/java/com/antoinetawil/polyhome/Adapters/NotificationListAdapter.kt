package com.antoinetawil.polyhome.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.antoinetawil.polyhome.Models.Notification
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.NotificationTranslator
import java.text.SimpleDateFormat
import java.util.*

class NotificationListAdapter :
        ListAdapter<Notification, NotificationListAdapter.ViewHolder>(NotificationDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.notificationTitle)
        val contentText: TextView = view.findViewById(R.id.notificationContent)
        val timeText: TextView = view.findViewById(R.id.notificationTime)
        val statusIcon: ImageView = view.findViewById(R.id.statusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.notification_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = getItem(position)

        // Translate the stored English notification to the current locale
        val (localizedTitle, localizedContent) =
                NotificationTranslator.getLocalizedNotification(
                        holder.itemView.context,
                        notification.title,
                        notification.content
                )

        holder.titleText.text = localizedTitle
        holder.contentText.text = localizedContent

        // Format timestamp
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        holder.timeText.text = dateFormat.format(Date(notification.timestamp))

        // Set status icon
        holder.statusIcon.setImageResource(
                if (notification.success) R.drawable.ic_success else R.drawable.ic_error
        )
    }
}

private class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
    override fun areItemsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem == newItem
}
