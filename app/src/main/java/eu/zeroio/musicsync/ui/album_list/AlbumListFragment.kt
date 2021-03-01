package eu.zeroio.musicsync.ui.album_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.zeroio.musicsync.R



/**
 * A fragment representing a list of Items.
 */
class AlbumListFragment : Fragment() {
    private val albumListViewModel: AlbumListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val artistId = arguments?.getInt("artistId")!!
        val root = inflater.inflate(R.layout.fragment_album_list, container, false)
        val view = root.findViewById<RecyclerView>(R.id.album_list_view)
        val albumListAdapter = AlbumRecyclerViewAdapter(albumListViewModel.selectedAlbum, albumListViewModel.deleteAllClicked)

        with(view) {
            layoutManager = LinearLayoutManager(context)
            adapter = albumListAdapter
        }

        albumListViewModel.selectedAlbum.observe(viewLifecycleOwner, Observer {
            albumListViewModel.download(it.fullAlbum.album)
            Toast.makeText(context, "Queued download", Toast.LENGTH_SHORT).show()
        })

        albumListViewModel.deleteAllClicked.observe(viewLifecycleOwner, Observer { (_, album) ->
            albumListViewModel.delete(album) }
        )

        albumListViewModel.albums.observe(viewLifecycleOwner, Observer { list ->
            albumListAdapter.submitList(list)
            albumListAdapter.notifyDataSetChanged()
        })

        albumListViewModel.userMessage.observe(viewLifecycleOwner, Observer {
                msg -> Toast.makeText(this.context, msg, Toast.LENGTH_LONG).show() }
        )

        albumListViewModel.refresh(artistId)

        return root
    }
}
