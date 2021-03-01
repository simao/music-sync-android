package eu.zio.musicsync.ui.artist_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.toolbox.Volley
import eu.zio.musicsync.MusicSyncHttpClient
import eu.zio.musicsync.model.Event
import eu.zio.musicsync.model.RichArtist
import eu.zio.musicsync.AlbumDownloader
import kotlinx.coroutines.launch
import java.util.*

class ArtistListViewModel(app: Application) : AndroidViewModel(app) {
    private val client =
        MusicSyncHttpClient(Volley.newRequestQueue(app))

    val errorMessage = MutableLiveData<String>()

    val artists = MutableLiveData<List<RichArtist>>()

    private val _selectedArtist = MutableLiveData<Event<String>>()

    val selectedArtistEvent : LiveData<Event<String>>
        get() = _selectedArtist

    fun userClickedArtist(artist: String) {
        _selectedArtist.value = Event(artist)
    }

    fun refresh() {
        viewModelScope.launch {
            val r = client.fetchArtists()

            r.onSuccess {
                val remoteArtists = it.map {
                    val status = AlbumDownloader.artistStatus(it.name)
                    RichArtist(it, status)
                }.sortedBy { it.artist.name.toLowerCase(Locale.ROOT) }

                artists.postValue(remoteArtists)
            }

            r.onFailure {
                errorMessage.postValue("Could not get artists. ${it.message}")
            }
        }
    }
}