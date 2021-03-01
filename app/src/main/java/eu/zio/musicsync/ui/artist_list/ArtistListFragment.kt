package eu.zio.musicsync.ui.artist_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.RecyclerView
import eu.zio.musicsync.R
import timber.log.Timber

class ArtistListFragment : Fragment() {
    private lateinit var artistListViewModel: ArtistListViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {

        artistListViewModel = ViewModelProviders.of(this).get(ArtistListViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_artist_list, container, false)

        val view: RecyclerView = root.findViewById(R.id.artist_list)

        val adapter = ArtistRecyclerViewAdapter(artistListViewModel)
        view.adapter = adapter

        artistListViewModel.selectedArtistEvent.observe(this, Observer {
            it.getContentIfNotHandled()?.let {
                val action = ArtistListFragmentDirections.actionNavigationArtistListToAlbumList(it)
                findNavController().navigate(action)
            }
        })

        artistListViewModel.artists.observe(this, Observer { list -> adapter.submitList(list) })

        artistListViewModel.errorMessage.observe(this, Observer { msg -> Toast.makeText(this.context, msg, Toast.LENGTH_SHORT).show() })

        artistListViewModel.refresh()

        return root
    }
}
