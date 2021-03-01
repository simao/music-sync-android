package eu.zio.musicsync.ui.album_list

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.toolbox.Volley
import eu.zio.musicsync.MusicSyncHttpClient
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.AlbumStatus
import eu.zio.musicsync.model.RichAlbum
import eu.zio.musicsync.model.Track
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.collections.ArrayList

class AlbumDownloader(private val client: MusicSyncHttpClient,
                      private val downloadManager: DownloadManager) {

    private fun trackFile(t: Track): File {
        val album = t.album!! // TODO: !!
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            .toPath()
            .resolve("MusicSync/${album.artist.name}/${album.name}/${t.filename}")
            .toFile()
    }

    fun albumDir(album: Album): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            .toPath()
            .resolve("MusicSync/${album.artist.name}/${album.name}")
            .toFile()
    }

    private fun downloaded(t: Track): Boolean = trackFile(t).exists()

    suspend fun albumStatus(id: Int): AlbumStatus {
        val tracksR = client.albumTracks(id)
        val tracks = tracksR.getOrThrow() // TODO: No

        // TODO: repeated it.downloaded()
        return when {
            tracks.all { downloaded(it) } -> AlbumStatus.Downloaded
            tracks.any { downloaded(it) } -> AlbumStatus.PartialDownload
            else -> AlbumStatus.Offline
        }
    }

    suspend fun downloadAlbum(id: Int) {
        val tracksR = client.albumTracks(id)
        val tracks = tracksR.getOrThrow() // TODO: no

        for (t in tracks) {
            if(trackFile(t).exists()) {
                Timber.i("File already exists, skipping")
            } else {
                val albumId = t.album!!.id // TODO: !!
                val req = DownloadManager.Request(client.trackAudioUri(albumId, t.id))
                req.setDestinationUri(trackFile(t).toUri())
                req.setAllowedOverMetered(false)

                downloadManager.enqueue(req)
            }

            // TODO: What if two downloads for the same file are queued?
            // TODO: Not handling Errors on download manager
            // TODO: Not handling Success on download manager
            // TODO: Not handling file already exists
        }

        tracks.firstOrNull()?.album?.let {
            val req = DownloadManager.Request(client.albumCoverUri(it.id))
            req.setDestinationUri(albumDir(it).resolve("cover2.jpg").toUri())
            req.setAllowedOverMetered(false)
            downloadManager.enqueue(req)
        }
    }
}


class AlbumListViewModel(private val app: Application) : AndroidViewModel(app) {

    private val client = MusicSyncHttpClient(Volley.newRequestQueue(app))

    val errorMessage = MutableLiveData<String>()

    val albums = MutableLiveData<List<RichAlbum>>()

    val selectedAlbum = MutableLiveData<Int>()

    fun refresh(artistName: String?) {
        // TODO: Should not do this all the time
        val downloader = AlbumDownloader(client, app.getSystemService(DownloadManager::class.java)!!)

        viewModelScope.launch {
            val falbums = client.fetchArtistAlbums(artistName).getOrThrow() // TODO: Show error
            val res = ArrayList<RichAlbum>()

            for (a in falbums) {
                val status = downloader.albumStatus(a.id)

                res.add(RichAlbum(a, status))
            }

            albums.postValue(res)
        }
    }

    fun download(albumId: Int) {
        val downloadManager = app.getSystemService(DownloadManager::class.java)!! // TODO: !!
        val download = AlbumDownloader(client, downloadManager)

        viewModelScope.launch {
            download.downloadAlbum(albumId)
        }
    }
}
