package eu.zio.musicsync.ui.album_list

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import eu.zio.musicsync.R
import eu.zio.musicsync.model.RichAlbum


/**
 * A fragment representing a list of Items.
 */
class AlbumListFragment : Fragment() {
    private val albumListViewModel: AlbumListViewModel by viewModels()

    private lateinit var mSharedPrefs: SharedPreferences
    private lateinit var mContentResolver: ContentResolver

    private val MUSIC_URI_KEY = "MUSIC_URI"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mSharedPrefs = context!!.getSharedPreferences(getString(R.string.album_list_prefs), Context.MODE_PRIVATE)
        mContentResolver = context!!.contentResolver
        val artist = arguments?.getString("artist")
        val root = inflater.inflate(R.layout.fragment_album_list, container, false)
        val view = root.findViewById<RecyclerView>(R.id.album_list_view)
        val albumListAdapter = AlbumRecyclerViewAdapter(albumListViewModel.selectedAlbum, albumListViewModel.deleteAllClicked)

        if(getMusicDir() == null) {
            openMusicUri()
        }

        with(view) {
            layoutManager = LinearLayoutManager(context)
            adapter = albumListAdapter
        }

        albumListViewModel.selectedAlbum.observe(this, Observer {
            albumListViewModel.download(it)
            Toast.makeText(context, "Queued download", Toast.LENGTH_SHORT).show()
        })

        albumListViewModel.albumChanged.observe(this, Observer { (pos, status) ->
            val item = albumListAdapter.currentList[pos]
            albumListAdapter.currentList[pos] = RichAlbum(item.album, status)
            albumListAdapter.notifyItemChanged(pos)
        })

        albumListViewModel.deleteAllClicked.observe(this, Observer { (pos, album) ->
            albumListViewModel.delete(getMusicDir()!!, pos, album) } // TODO: !!
        )

        albumListViewModel.albums.observe(this, Observer { list -> albumListAdapter.submitList(list) })

        albumListViewModel.userMessage.observe(this, Observer { msg -> Toast.makeText(this.context, msg, Toast.LENGTH_SHORT).show() })

        albumListViewModel.refresh(artist)

        return root
    }

    private fun openMusicUri() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }

        startActivityForResult(intent, 0)
    }

    private fun getMusicDir(): Uri? {
        return mSharedPrefs.getString(MUSIC_URI_KEY, null)?.let { Uri.parse(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.

            resultData?.data?.also { uri ->
                with(mSharedPrefs.edit()) {
                    putString(MUSIC_URI_KEY, uri.toString())
                    commit()
                }

                mContentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
    }
}
