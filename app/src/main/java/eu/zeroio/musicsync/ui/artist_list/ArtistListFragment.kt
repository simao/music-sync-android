package eu.zeroio.musicsync.ui.artist_list

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import eu.zeroio.musicsync.R


class ArtistListFragment : Fragment() {
    private lateinit var artistListViewModel: ArtistListViewModel
    private lateinit var artistViewAdapter: ArtistRecyclerViewAdapter
    private lateinit var pullToRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        artistListViewModel = ViewModelProviders.of(this).get(ArtistListViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_artist_list, container, false)
        val view: RecyclerView = root.findViewById(R.id.artist_list)
        artistViewAdapter = ArtistRecyclerViewAdapter(artistListViewModel)
        view.adapter = artistViewAdapter
        pullToRefresh = root.findViewById(R.id.artistListPullToRefresh)

        askPermissions()

        setupArtistList()

        return root
    }

    private fun setupArtistList() {
        artistListViewModel.selectedArtistEvent.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {
                val action = ArtistListFragmentDirections.actionNavigationArtistListToAlbumList(it)
                findNavController().navigate(action)
            }
        })

        artistListViewModel.artists.observe(viewLifecycleOwner, Observer { list ->
            artistViewAdapter.submitList(list)
        })

        artistListViewModel.errorMessage.observe(viewLifecycleOwner, Observer { msg ->
            Toast.makeText(this.context, msg, Toast.LENGTH_LONG).show()
        })

        pullToRefresh.setOnRefreshListener {
            artistListViewModel.refresh { pullToRefresh.isRefreshing = false }
        }

        artistListViewModel.refresh()
    }

    private val PERMISSION_REQUEST_CODE: Int =  200

    private fun askPermissions() {
        val permissions = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

        requestPermissions(permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if ((requestCode == PERMISSION_REQUEST_CODE) && grantResults.contains(PackageManager.PERMISSION_DENIED)) {
            Toast.makeText(requireContext(), "Access to external storage is necessary to save music file", Toast.LENGTH_LONG).show()
        }
    }
}
