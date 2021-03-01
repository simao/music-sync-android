package eu.zeroio.musicsync.ui.artist_list

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.android.volley.toolbox.Volley
import eu.zeroio.musicsync.MusicSyncHttpClient
import eu.zeroio.musicsync.model.Event
import eu.zeroio.musicsync.model.DisplayArtist
import eu.zeroio.musicsync.AlbumDownloader
import eu.zeroio.musicsync.ui.SettingsActivity
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.IllegalArgumentException
import java.util.*

class ArtistListViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(app.applicationContext)

    private val volleyRequestQueue = Volley.newRequestQueue(app)

    val errorMessage = MutableLiveData<String>()

    val artists = MutableLiveData<List<DisplayArtist>>()

    private val _selectedArtist = MutableLiveData<Event<Int>>()

    val selectedArtistEvent : LiveData<Event<Int>>
        get() = _selectedArtist

    fun userClickedArtist(artist: Int) {
        _selectedArtist.value = Event(artist)
    }

    fun refresh(callback: () -> Unit = { }) {
        viewModelScope.launch {
            val r = MusicSyncHttpClient
                .from(volleyRequestQueue, preferences)
                .mapCatching { it.fetchArtists().getOrThrow() }

            r.onSuccess { remoteArtists ->
                val displayArtists = remoteArtists.map {
                    val status = AlbumDownloader.artistStatus(it.name)
                    DisplayArtist(it, status)
                }.sortedBy { it.artist.name.toLowerCase(Locale.ROOT) }

                artists.postValue(displayArtists)
                callback()
            }

            r.onFailure {
                Timber.w(it, "Error refreshing artist list")
                val msg = it.message ?: "Unknown error"
                errorMessage.postValue("Could not get data from server: $msg")
                callback()
            }
        }
    }
}