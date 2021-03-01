package eu.zio.musicsync.ui.album_list

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.toolbox.Volley
import eu.zio.musicsync.AlbumDownloader
import eu.zio.musicsync.MusicSyncHttpClient
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.OfflineStatus
import eu.zio.musicsync.model.RichAlbum
import kotlinx.coroutines.launch


class AlbumListViewModel(app: Application) : AndroidViewModel(app) {

    private val client = MusicSyncHttpClient(Volley.newRequestQueue(app))

    private val downloader = AlbumDownloader(client, app.getSystemService(DownloadManager::class.java)!!)

    val userMessage = MutableLiveData<String>()

    val albums = MutableLiveData<List<RichAlbum>>()

    val selectedAlbum = MutableLiveData<Album>()

    val deleteAllClicked = MutableLiveData<Pair<Int, Album>>()

    val albumChanged = MutableLiveData<Pair<Int, OfflineStatus>>()

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

    fun delete(baseDir: Uri, pos: Int, album: Album) {
        val df = DocumentFile.fromTreeUri(getApplication(), baseDir)
        if(downloader.deleteAlbum(df, album)) {
            userMessage.postValue("Album ${album.name} deleted")
        } else {
            userMessage.postValue("Could not delete album")
        }

        viewModelScope.launch {
            val newStatus = downloader.albumStatus(album)
            albumChanged.postValue(Pair(pos, newStatus))
        }
    }

    fun download(baseDir: Uri, album: Album) {
        viewModelScope.launch {
            val ctx: Context = getApplication()
            val df = DocumentFile.fromTreeUri(ctx, baseDir)!!

            downloader
                .downloadAlbum(df, ctx.contentResolver, album)
                .onFailure { userMessage.postValue("Could not download albums: ${it.message}") }
        }
    }
}

