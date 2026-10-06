package com.secretcalc.browser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class BookmarkAdapter(
    private val context: Context,
    private val onClick: (BookmarkEntry) -> Unit,
    private val onLongClick: (BookmarkEntry) -> Unit
) : ListAdapter<BookmarkEntry, BookmarkAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvUrl: TextView = itemView.findViewById(R.id.tvUrl)

        fun bind(entry: BookmarkEntry) {
            tvTitle.text = entry.title
            tvUrl.text = entry.url
            itemView.setOnClickListener { onClick(entry) }
            itemView.setOnLongClickListener { onLongClick(entry); true }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BookmarkEntry>() {
        override fun areItemsTheSame(oldItem: BookmarkEntry, newItem: BookmarkEntry) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BookmarkEntry, newItem: BookmarkEntry) = oldItem == newItem
    }
}
