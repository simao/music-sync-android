package eu.zeroio.musicsync.ui.album_list

import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.zeroio.musicsync.R
import eu.zeroio.musicsync.model.OfflineStatus
import eu.zeroio.musicsync.model.DisplayAlbum

class AlbumRecyclerViewAdapter (
    private val selectedAlbum: MutableLiveData<DisplayAlbum>,
    private val deleteAllClicked: MutableLiveData<Pair<Int, DisplayAlbum>>) : ListAdapter<DisplayAlbum, AlbumRecyclerViewAdapter.ViewHolder>(
    AlbumDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_album_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.contentView.text = item.fullAlbum.album.title

        when(item.status) {
            OfflineStatus.Downloaded ->
                holder.imgView.setColorFilter(ContextCompat.getColor(holder.imgView.context, android.R.color.holo_green_dark))
            OfflineStatus.PartialDownload ->
                holder.imgView.setColorFilter(ContextCompat.getColor(holder.imgView.context, android.R.color.holo_orange_dark))
            OfflineStatus.Offline -> {
                holder.imgView.setColorFilter(ContextCompat.getColor(holder.imgView.context, android.R.color.transparent))
            }
        }

        holder.view.setOnLongClickListener {
            selectedAlbum.value = item
            true
        }

        holder.imgView.setOnClickListener {
            val popup = PopupMenu(holder.imgView.context, holder.imgView)
            val inflater: MenuInflater = popup.menuInflater
            inflater.inflate(R.menu.delete_menu, popup.menu)

            popup.setOnMenuItemClickListener { c ->
                when(c.itemId) {
                    R.id.delete_all -> {
                        deleteAllClicked.postValue(Pair(position, item))
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val contentView: TextView = view.findViewById(R.id.content)
        val imgView: ImageButton = view.findViewById(R.id.album_status_icon)

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }
}

class AlbumDiffCallback : DiffUtil.ItemCallback<DisplayAlbum>() {
    override fun areItemsTheSame(oldItem: DisplayAlbum, newItem: DisplayAlbum): Boolean {
        return oldItem.id == newItem.id && oldItem.status == newItem.status
    }

    override fun areContentsTheSame(oldItem: DisplayAlbum, newItem: DisplayAlbum): Boolean {
        return oldItem.id == newItem.id && oldItem.status == newItem.status
    }
}
