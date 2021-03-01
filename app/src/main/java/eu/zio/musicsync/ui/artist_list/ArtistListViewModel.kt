package eu.zio.musicsync.ui.artist_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.toolbox.Volley
import eu.zio.musicsync.MusicSyncHttpClient
import eu.zio.musicsync.model.Artist
import eu.zio.musicsync.model.Event

class ArtistListViewModel(app: Application) : AndroidViewModel(app) {
    private val client =
        MusicSyncHttpClient(Volley.newRequestQueue(app))

    val errorMessage = MutableLiveData<String>()

    val artists = MutableLiveData<List<Artist>>()

    private val _selectedArtist = MutableLiveData<Event<String>>()

    val selectedArtistEvent : LiveData<Event<String>>
        get() = _selectedArtist

    fun userClickedArtist(artist: String) {
        _selectedArtist.value = Event(artist)
    }

    fun refresh() {
        client.fetchArtists(artists, errorMessage)
    }
}