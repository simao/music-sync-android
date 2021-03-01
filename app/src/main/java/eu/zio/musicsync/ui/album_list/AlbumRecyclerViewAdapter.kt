package eu.zio.musicsync.ui.album_list

import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.View.inflate
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ColorStateListInflaterCompat.inflate
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.zio.musicsync.R
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.OfflineStatus
import eu.zio.musicsync.model.RichAlbum

class AlbumRecyclerViewAdapter(private val selectedAlbum: MutableLiveData<Album>,
                               private val deleteAllClicked: MutableLiveData<Album>) : ListAdapter<RichAlbum, AlbumRecyclerViewAdapter.ViewHolder>(AlbumDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_album_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.contentView.text = item.album.name

        when(item.status) {
            OfflineStatus.Downloaded ->
                holder.imgView.setColorFilter(ContextCompat.getColor(holder.imgView.context, android.R.color.holo_green_dark))
            OfflineStatus.PartialDownload ->
                holder.imgView.setColorFilter(ContextCompat.getColor(holder.imgView.context, android.R.color.holo_orange_dark))
            OfflineStatus.Offline ->
                holder.imgView.setColorFilter(ContextCompat.getColor(holder.imgView.context, android.R.color.darker_gray))
        }

        holder.view.setOnLongClickListener {
            selectedAlbum.value = item.album
            true
        }

        holder.imgView.setOnClickListener {
            val popup = PopupMenu(holder.imgView.context, holder.imgView)
            val inflater: MenuInflater = popup.menuInflater
            inflater.inflate(R.menu.delete_menu, popup.menu)

            popup.setOnMenuItemClickListener { c ->
                when(c.itemId) {
                    R.id.delete_all -> {
                        deleteAllClicked.postValue(item.album)
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

class AlbumDiffCallback : DiffUtil.ItemCallback<RichAlbum>() {
    override fun areItemsTheSame(oldItem: RichAlbum, newItem: RichAlbum): Boolean {
        return oldItem.album.id == newItem.album.id && oldItem.status == newItem.status
    }

    override fun areContentsTheSame(oldItem: RichAlbum, newItem: RichAlbum): Boolean {
        return oldItem.album.id == newItem.album.id && oldItem.status == newItem.status
    }
}
