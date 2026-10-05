package com.secretcalc.browser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val context: Context,
    private val onClick: (HistoryEntry) -> Unit
) : ListAdapter<HistoryEntry, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvUrl: TextView = itemView.findViewById(R.id.tvUrl)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(entry: HistoryEntry) {
            tvTitle.text = entry.title
            tvUrl.text = entry.url
            tvTime.text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
            itemView.setOnClickListener { onClick(entry) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HistoryEntry>() {
        override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry) = oldItem.timestamp == newItem.timestamp
        override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry) = oldItem == newItem
    }
}
