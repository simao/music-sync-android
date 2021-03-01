package eu.zio.musicsync

import android.app.DownloadManager
import android.content.ContentResolver
import android.os.Environment
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.OfflineStatus
import eu.zio.musicsync.model.Track
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
                .resolve("MusicSync/${album.artist.name}/${album.name}/${t.filename}")
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

            if (baseDir.findFile(album.artist.name) == null) {
                baseDir.createDirectory(album.artist.name)
            }

            val albumDir = baseDir.findFile(album.artist.name)?.findFile(album.name)
            if (albumDir == null) {
                baseDir.findFile(album.artist.name)?.createDirectory(album.name)
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
        val documentFile = baseDir?.findFile(album.artist.name)?.findFile(album.name)
        return documentFile?.delete() ?: false
    }
}
