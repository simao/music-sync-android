package eu.zio.musicsync.ui.artist_list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.zio.musicsync.R
import eu.zio.musicsync.model.OfflineStatus
import eu.zio.musicsync.model.RichArtist
import timber.log.Timber

class ArtistRecyclerViewAdapter(val viewModel: ArtistListViewModel) : ListAdapter<RichArtist, ArtistRecyclerViewAdapter.ViewHolder>(
    ArtistDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_artist_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.contentView.text = item.artist.name

        when(item.status) {
            OfflineStatus.Downloaded -> {
                holder.imgView.setColorFilter(ContextCompat.getColor(holder.imgView.context, android.R.color.darker_gray))
                holder.imgView.visibility = View.VISIBLE
            }
            else ->
                holder.imgView.visibility = View.INVISIBLE
        }

        holder.view.setOnClickListener {
            viewModel.userClickedArtist(item.artist.name)
        }
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val contentView: TextView = view.findViewById(R.id.content)
        val imgView: ImageView = view.findViewById(R.id.artist_status_icon)

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }
}


class ArtistDiffCallback : DiffUtil.ItemCallback<RichArtist>() {
    override fun areItemsTheSame(oldItem: RichArtist, newItem: RichArtist): Boolean {
        return oldItem.artist.name == newItem.artist.name && oldItem.status == newItem.status
    }

    override fun areContentsTheSame(oldItem: RichArtist, newItem: RichArtist): Boolean {
        return oldItem.artist.name == newItem.artist.name && oldItem.status == newItem.status
    }
}
