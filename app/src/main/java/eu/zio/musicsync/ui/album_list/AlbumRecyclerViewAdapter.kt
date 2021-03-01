package eu.zio.musicsync.ui.album_list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.zio.musicsync.R
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.AlbumStatus
import eu.zio.musicsync.model.RichAlbum
import kotlinx.coroutines.withContext

class AlbumRecyclerViewAdapter(private val selectedAlbum: MutableLiveData<Int>) : ListAdapter<RichAlbum, AlbumRecyclerViewAdapter.ViewHolder>(AlbumDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_album_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.contentView.text = item.album.name

        val imgView: ImageView = holder.view.findViewById(R.id.status_icon)

        when(item.status) {
            AlbumStatus.Downloaded ->
                imgView.setColorFilter(ContextCompat.getColor(imgView.context, android.R.color.holo_green_dark))
            AlbumStatus.PartialDownload ->
                imgView.setColorFilter(ContextCompat.getColor(imgView.context, android.R.color.holo_orange_dark))
            AlbumStatus.Offline ->
                imgView.setColorFilter(ContextCompat.getColor(imgView.context, android.R.color.darker_gray))
        }

        holder.view.setOnLongClickListener { view ->
            selectedAlbum.value = item.album.id
            true
        }
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val contentView: TextView = view.findViewById(R.id.content)

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
