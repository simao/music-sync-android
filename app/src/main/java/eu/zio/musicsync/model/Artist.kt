package eu.zio.musicsync.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Artist(val name: String)


enum class AlbumStatus {
    Downloaded,
    PartialDownload,
    Offline
}

class RichAlbum(val album: Album, val status: AlbumStatus)

@JsonClass(generateAdapter = true)
class Album(val id: Int, val name: String, val artist: Artist, val year: Int)

@JsonClass(generateAdapter = true)
class Track(val id: String, val name: String, val track_nr: Int?, val album: Album?, val filename: String)

