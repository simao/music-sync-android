package eu.zeroio.musicsync.ui.album_list

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.android.volley.toolbox.Volley
import eu.zeroio.musicsync.AlbumDownloader
import eu.zeroio.musicsync.MusicSyncHttpClient
import eu.zeroio.musicsync.model.Album
import eu.zeroio.musicsync.model.DisplayAlbum
import kotlinx.coroutines.launch

class AlbumListViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(app.applicationContext)
    private val client = MusicSyncHttpClient.from(Volley.newRequestQueue(app), preferences).getOrThrow()
    private val downloader = AlbumDownloader(client, app.getSystemService(DownloadManager::class.java)!!)

    val userMessage = MutableLiveData<String>()

    val albums = MutableLiveData<List<DisplayAlbum>>()

    val selectedAlbum = MutableLiveData<DisplayAlbum>()

    val deleteAllClicked = MutableLiveData<Pair<Int, DisplayAlbum>>()

    fun refresh(artistId: Int) {
        viewModelScope.launch {
            val falbums = client.fetchArtistAlbums(artistId)

            falbums.fold(
                { downloadedAlbums ->
                    val displayAlbums = downloadedAlbums.map {
                        val status = downloader.albumStatus(it)
                        DisplayAlbum(it.album.id, it, status)
                    }.sortedBy { it.fullAlbum.album.title }

                    albums.postValue(displayAlbums)
                },
                {
                    userMessage.postValue("Could not get albums: ${it.message}")
                }
            )
        }
    }

    fun delete(displayAlbum: DisplayAlbum) {
        if(downloader.deleteAlbum(displayAlbum.fullAlbum.album)) {
            userMessage.postValue("Deleted ${displayAlbum.fullAlbum.album.title}")
        } else {
            userMessage.postValue("Could not delete album")
        }

        viewModelScope.launch {
            val newStatus = downloader.albumStatus(displayAlbum.fullAlbum)
            val _albums = albums.value ?: listOf()
            _albums.find { it.id == displayAlbum.id }?.status = newStatus
            albums.postValue(_albums)
        }
    }

    fun download(album: Album) {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val ctx: Context = getApplication()

        ctx.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val _albums = albums.value ?: listOf()
                viewModelScope.launch {
                    for ((idx, a) in _albums.withIndex()) {
                        val status = downloader.albumStatus(a.fullAlbum)

                        if (status != a.status) {
                            _albums[idx].status = status
                        }
                    }

                    albums.postValue(_albums)
                }
            } }, filter)

        viewModelScope.launch {
            downloader
                .downloadAlbum(album)
                .onFailure { userMessage.postValue("Could not download albums: ${it.message}") }
        }
    }
}
