package eu.zio.musicsync.ui.album_list

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.toolbox.Volley
import eu.zio.musicsync.AlbumDownloader
import eu.zio.musicsync.MusicSyncHttpClient
import eu.zio.musicsync.db.AppDatabase
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.AlbumWithTracks
import eu.zio.musicsync.model.DisplayAlbum
import kotlinx.coroutines.launch


class AlbumListViewModel(app: Application) : AndroidViewModel(app) {

    private val client = MusicSyncHttpClient(Volley.newRequestQueue(app))

    private val downloader = AlbumDownloader(client, app.getSystemService(DownloadManager::class.java)!!)

    val userMessage = MutableLiveData<String>()

    val albums = MutableLiveData<List<DisplayAlbum>>()
    private val _albums = ArrayList<DisplayAlbum>()

    val selectedAlbum = MutableLiveData<Album>()

    val deleteAllClicked = MutableLiveData<Pair<Int, Album>>()

    private suspend fun albumTracks(db: AppDatabase, album: Album): AlbumWithTracks {
        val existing = db.albumDao().getAlbumWithTracks(album.id)

        return if ((existing == null) || (existing.tracks.isEmpty())) {
            val remote = client.albumTracks(album.id).onFailure { userMessage.postValue("Could not get album tracks: ${it.message}") }.getOrDefault(ArrayList())
            AlbumWithTracks(album, remote)
                .also { db.albumDao().insertAlbumsWithTracks(it) }
        } else {
            existing
        }
    }

    fun refresh(db: AppDatabase, artistName: String?) {
        viewModelScope.launch {
            val falbums = client.fetchArtistAlbums(artistName)

            falbums.fold(
                {
                    val res = it.map {
                        val tracks = albumTracks(db, it)
                        val status = downloader.albumStatus(tracks)
                        DisplayAlbum(it, status)
                    }

                    _albums.clear()
                    _albums.addAll(res)
                    albums.postValue(res)
                },
                {
                    userMessage.postValue("Could not get albums: ${it.message}")
                }
            )
        }
    }

    fun delete(db: AppDatabase, baseDir: Uri, album: Album) {
        val df = DocumentFile.fromTreeUri(getApplication(), baseDir)
        if(downloader.deleteAlbum(df, album)) {
            userMessage.postValue("Album ${album.name} deleted")
        } else {
            userMessage.postValue("Could not delete album")
        }

        viewModelScope.launch {
            val tracks = albumTracks(db, album)
            val newStatus = downloader.albumStatus(tracks)
            _albums.find { it.album.id == album.id }?.status = newStatus
            albums.postValue(_albums)
        }
    }

    fun download(db: AppDatabase, baseDir: Uri, album: Album) {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val ctx: Context = getApplication()

        ctx.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModelScope.launch {
                    for ((idx, a) in _albums.withIndex()) {
                        val tracks = albumTracks(db, a.album)
                        val status = downloader.albumStatus(tracks)

                        if (status != a.status) {
                            _albums[idx].status = status
                        }
                    }

                    albums.postValue(_albums)
                }
            }         }, filter)

        viewModelScope.launch {
            val df = DocumentFile.fromTreeUri(ctx, baseDir)!!

            downloader
                .downloadAlbum(df, ctx.contentResolver, album)
                .onFailure { userMessage.postValue("Could not download albums: ${it.message}") }
        }
    }
}

