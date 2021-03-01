package eu.zio.musicsync.ui.album_list

import android.app.Application
import android.app.DownloadManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.toolbox.Volley
import eu.zio.musicsync.AlbumDownloader
import eu.zio.musicsync.MusicSyncHttpClient
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.RichAlbum
import kotlinx.coroutines.launch


class AlbumListViewModel(app: Application) : AndroidViewModel(app) {

    private val client = MusicSyncHttpClient(Volley.newRequestQueue(app))

    private val downloader = AlbumDownloader(client, app.getSystemService(DownloadManager::class.java)!!)

    val userMessage = MutableLiveData<String>()

    val albums = MutableLiveData<List<RichAlbum>>()

    val selectedAlbum = MutableLiveData<Album>()

    val deleteAllClicked = MutableLiveData<Album>()

    fun refresh(artistName: String?) {
        viewModelScope.launch {
            val falbums = client.fetchArtistAlbums(artistName)

            falbums.fold(
                {
                    val res = it.map {
                        val status = downloader.albumStatus(it)
                        RichAlbum(it, status)
                    }

                    albums.postValue(res)
                },
                {
                    userMessage.postValue("Could not get albums: ${it.message}")
                }
            )
        }
    }

    fun delete(album: Album) {
        if(downloader.deleteAlbum(album)) {
            userMessage.postValue("Album ${album.name} deleted")
        } else {
            userMessage.postValue("Could not delete album")
        }
    }

    fun download(album: Album) {
        viewModelScope.launch {
            downloader
                .downloadAlbum(album)
                .onFailure { userMessage.postValue("Could not download albums: ${it.message}") }
        }
    }
}
