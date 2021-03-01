package eu.zio.musicsync.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Artist(val name: String)


enum class OfflineStatus {
    Downloaded,
    PartialDownload,
    Offline
}

class RichArtist(val artist: Artist, val status: OfflineStatus)

class RichAlbum(val album: Album, val status: OfflineStatus)

@JsonClass(generateAdapter = true)
class Album(val id: Int, val name: String, val artist: Artist, val year: Int)

@JsonClass(generateAdapter = true)
class Track(val id: String, val name: String, val track_nr: Int?, val album: Album?, val filename: String)

