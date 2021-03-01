package eu.zio.musicsync.model

import androidx.room.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


enum class OfflineStatus {
    Downloaded,
    PartialDownload,
    Offline
}

@JsonClass(generateAdapter = true)
@Entity
data class Artist(@PrimaryKey @ColumnInfo(name = "name") val name: String)

class DisplayArtist(val artist: Artist, val status: OfflineStatus)

data class DisplayAlbum(val album: Album, var status: OfflineStatus)

@JsonClass(generateAdapter = true)
class JsonAlbum(
    val id: Int,
    val name: String,
    val year: Int,
    val artist: Artist
)

@JsonClass(generateAdapter = true)
data class JsonTrack(val id: String,
                 val name: String,
                 val filename: String,
                 val album: JsonAlbum // TODO: Should be JsonAlbum?
)

@Entity
data class Album(@PrimaryKey @ColumnInfo(name = "id") val id: Int,
            @ColumnInfo(name = "name") val name: String,
            @ColumnInfo(name = "year") val year: Int,
            @ColumnInfo(name = "artist_name") val artistName: String
)

@Entity
data class Track(@PrimaryKey @ColumnInfo(name = "id") val id: String,
                 @ColumnInfo(name = "name") val name: String,
                 @ColumnInfo(name = "filename") val filename: String,
                 @ColumnInfo(name = "album_id") val albumId: Int,
                 @ColumnInfo(name = "album_name") val albumName: String // Duplicated, ok?
)

data class AlbumWithTracks(
    @Embedded val album: Album,
    @Relation(
         parentColumn = "id",
         entityColumn = "album_id"
    )
    val tracks: List<Track>
)