package eu.zio.musicsync.ui.album_list

import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.telephony.mbms.DownloadRequest
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import eu.zio.musicsync.MusicSyncHttpClient
import eu.zio.musicsync.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File



/**
 * A fragment representing a list of Items.
 */
class AlbumListFragment : Fragment() {
    private val albumListViewModel: AlbumListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val artist = arguments?.getString("artist")

        val root = inflater.inflate(R.layout.fragment_album_list, container, false)

        val view = root.findViewById<RecyclerView>(R.id.album_list_view)

        val albumListAdapter = AlbumRecyclerViewAdapter(albumListViewModel.selectedAlbum)

        with(view) {
            layoutManager = LinearLayoutManager(context)
            adapter = albumListAdapter
        }

        albumListViewModel.selectedAlbum.observe(this, Observer { id ->
            albumListViewModel.download(id)
            Toast.makeText(context, "Queued download", Toast.LENGTH_SHORT).show()
        })

        albumListViewModel.albums.observe(this, Observer { list -> albumListAdapter.submitList(list) })

        albumListViewModel.errorMessage.observe(this, Observer { msg -> Toast.makeText(this.context, msg, Toast.LENGTH_SHORT).show() })

        albumListViewModel.refresh(artist)

        return root
    }
}
