package eu.zeroio.musicsync.model

import com.squareup.moshi.JsonClass


enum class OfflineStatus {
    Downloaded,
    PartialDownload,
    Offline
}

@JsonClass(generateAdapter = true)
data class Artist(val id: Int, val name: String)

class DisplayArtist(val artist: Artist, val status: OfflineStatus)

data class DisplayAlbum(val id: Int, val fullAlbum: FullAlbum, var status: OfflineStatus)

@JsonClass(generateAdapter = true)
class Album(
    val id: Int,
    val title: String,
    val artist: Artist
)

@JsonClass(generateAdapter = true)
class FullAlbum(
    val album: Album,
    val tracks: List<Track>
)

@JsonClass(generateAdapter = true)
data class Track(val id: Int,
                 val name: String?,
                 val filename: String
)

@JsonClass(generateAdapter = true)
class JsonResponse<T>(val values: List<T>)