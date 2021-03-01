package eu.zio.musicsync

import android.app.DownloadManager
import android.content.ContentResolver
import android.os.Environment
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import eu.zio.musicsync.model.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class AlbumDownloader(private val client: MusicSyncHttpClient,
                      private val downloadManager: DownloadManager
) {

    companion object {
        private fun trackFile(album: Album, t: Track): File {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                .toPath()
                .resolve("MusicSync/${album.artistName}/${album.name}/${t.filename}")
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

    fun albumStatus(albumWithTracks: AlbumWithTracks): OfflineStatus {
        val trackStatus = albumWithTracks.tracks.map { downloaded(albumWithTracks.album, it) }

        return when {
            trackStatus.isEmpty() -> OfflineStatus.Offline
            trackStatus.all { it } -> OfflineStatus.Downloaded
            trackStatus.any { it } -> OfflineStatus.PartialDownload
            else -> OfflineStatus.Offline
        }
    }

    suspend fun downloadAlbum(baseDir: DocumentFile, contentResolver: ContentResolver, album: Album): Result<Int> {
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

            val response = client.fetchAlbumArtwork(album).getOrThrow() // TODO: Throw
            val mimetype = response.mimetype ?: "image/jpeg"

            if (baseDir.findFile(album.artistName) == null) {
                baseDir.createDirectory(album.artistName)
            }

            val albumDir = baseDir.findFile(album.artistName)?.findFile(album.name)
            if (albumDir == null) {
                baseDir.findFile(album.artistName)?.createDirectory(album.name)
            }

            val uri = albumDir?.createFile(mimetype, "cover")?.uri

            uri?.let {
                contentResolver.openFileDescriptor(it, "rw")?.use {
                    FileOutputStream(it.fileDescriptor).use {
                        it.write(response.bytes.array())
                    }
                }
            }


            tracks.size
        }
    }

    fun deleteAlbum(baseDir: DocumentFile?, album: Album): Boolean {
        val documentFile = baseDir?.findFile(album.artistName)?.findFile(album.name)
        return documentFile?.delete() ?: false
    }
}
