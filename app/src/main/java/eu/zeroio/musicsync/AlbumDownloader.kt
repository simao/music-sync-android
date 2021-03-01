package eu.zeroio.musicsync

import android.app.DownloadManager
import android.os.Environment
import androidx.core.net.toUri
import eu.zeroio.musicsync.model.Album
import eu.zeroio.musicsync.model.FullAlbum
import eu.zeroio.musicsync.model.OfflineStatus
import eu.zeroio.musicsync.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.ResponseCache
import java.nio.file.Files

class AlbumDownloader(
    private val client: MusicSyncHttpClient,
    private val downloadManager: DownloadManager) {

    companion object {
        private fun trackFile(album: Album, t: Track): File {
            @Suppress("DEPRECATION") // Not dealing with this now, https://commonsware.com/blog/2019/06/07/death-external-storage-end-saga.html
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                .toPath()
                .resolve("MusicSync/${album.artist.name}/${album.title}/${t.filename}")
                .toFile()
        }

        private fun albumDir(album: Album): File {
            return artistDir(album.artist.name).resolve(album.title)
        }

        private fun artistDir(name: String): File {
            @Suppress("DEPRECATION") // Not dealing with this now, https://commonsware.com/blog/2019/06/07/death-external-storage-end-saga.html
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                .toPath()
                .resolve("MusicSync/${name}")
                .toFile()
        }

        private fun downloaded(album: Album, t: Track): Boolean =
            trackFile(album, t).exists()

        fun artistStatus(name: String): OfflineStatus {
            return when(artistDir(name).exists()) {
                true -> OfflineStatus.Downloaded
                false -> OfflineStatus.Offline
            }
        }
    }

    fun albumStatus(albumWithTracks: FullAlbum): OfflineStatus {
        val trackStatus = albumWithTracks.tracks.map { downloaded(albumWithTracks.album, it) }

        return when {
            trackStatus.isEmpty() -> OfflineStatus.Offline
            trackStatus.all { it } -> OfflineStatus.Downloaded
            trackStatus.any { it } -> OfflineStatus.PartialDownload
            else -> OfflineStatus.Offline
        }
    }

    private suspend fun downloadArtwork(album: Album): String {
        val response = client.fetchAlbumArtwork(album.id).getOrThrow()
        val albumDir = albumDir(album)

        if (!albumDir.exists()) {
            withContext(Dispatchers.IO) {
                Files.createDirectories(albumDir.toPath())
            }
        }

        val filename = response.filename ?: "cover.jpg"
        val coverFile = albumDir.resolve(filename)
        withContext(Dispatchers.IO) {
            Files.write(coverFile.toPath(), response.bytes.array())
        }

        return filename
    }

    suspend fun downloadAlbum(album: Album): Result<Int> {
        val tracksR = client.albumTracks(album.id)

        return tracksR.mapCatching { tracks ->
            for (t in tracks) {
                val trackFile = trackFile(album, t)

                if (trackFile.exists()) {
                    Timber.i("File $trackFile already exists, skipping")
                } else {
                    val req = DownloadManager.Request(client.trackAudioUri(album.id, t.id))
                    req.setDestinationUri(trackFile.toUri())
                    req.setAllowedOverMetered(false)
                    downloadManager.enqueue(req)
                }
            }

            downloadArtwork(album)

            tracks.size
        }
    }

    fun deleteAlbum(album: Album): Boolean {
        val artistDir = artistDir(album.artist.name)
        val deletedAlbum =  albumDir(album).deleteRecursively()

        val artistIsEmpty = try {
            ! Files.newDirectoryStream(artistDir.toPath()).iterator().hasNext()
        } catch(ex: IOException) {
            Timber.w(ex)
            false
        }

        val deletedArtist =
            if (artistIsEmpty)
                artistDir.deleteRecursively()
            else
                true

        return deletedAlbum && deletedArtist
    }
}
