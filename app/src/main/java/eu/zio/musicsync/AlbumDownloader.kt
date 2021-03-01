package eu.zio.musicsync

import android.app.DownloadManager
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.Artist
import eu.zio.musicsync.model.OfflineStatus
import eu.zio.musicsync.model.Track
import timber.log.Timber
import java.io.File
import java.sql.Time

class AlbumDownloader(private val client: MusicSyncHttpClient,
                      private val downloadManager: DownloadManager
) {

    companion object {
        private fun trackFile(album: Album, t: Track): File {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                .toPath()
                .resolve("MusicSync/${album.artist.name}/${album.name}/${t.filename}")
                .toFile()
        }

        fun albumDir(album: Album): File {
            return artistDir(album.artist.name)
                .toPath()
                .resolve(album.name)
                .toFile()
        }

        private fun artistDir(name: String): File {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                .toPath()
                .resolve("MusicSync/${name}")
                .toFile()
        }

        private fun downloaded(album: Album, t: Track): Boolean = trackFile(album, t).exists()

        fun artistStatus(name: String): OfflineStatus {
            return when(artistDir(name).exists()) {
                true -> OfflineStatus.Downloaded
                false -> OfflineStatus.Offline
            }
        }
    }

    suspend fun albumStatus(album: Album): OfflineStatus {
        val tracksR = client.albumTracks(album.id)
        val tracks = tracksR.getOrDefault(ArrayList())

        val trackStatus = tracks.map { downloaded(album, it) }

        return when {
            trackStatus.all { it } -> OfflineStatus.Downloaded
            trackStatus.any { it } -> OfflineStatus.PartialDownload
            else -> OfflineStatus.Offline
        }
    }

    suspend fun downloadAlbum(album: Album): Result<Int> {
        val tracksR = client.albumTracks(album.id)

        return tracksR.map { tracks ->
            for (t in tracks) {
                val trackFile = trackFile(album, t)

                if (trackFile.exists()) {
                    Timber.i("File already exists, skipping")
                } else {
                    val req = DownloadManager.Request(client.trackAudioUri(album.id, t.id))
                    req.setDestinationUri(trackFile.toUri())
                    req.setAllowedOverMetered(false)

                    downloadManager.enqueue(req)
                }
            }

            tracks.firstOrNull()?.album?.let {
                val req = DownloadManager.Request(client.albumCoverUri(it.id))
                req.setDestinationUri(albumDir(it).resolve("cover.jpg").toUri())
                req.setAllowedOverMetered(false)
                downloadManager.enqueue(req)
            }

            tracks.size
        }
    }

    fun deleteAlbum(album: Album): Boolean {
        Timber.e(albumDir(album).listFiles()!!.toString())
        return albumDir(album).deleteRecursively()
    }
}