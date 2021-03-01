package eu.zio.musicsync.db

import androidx.room.*
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.AlbumWithTracks
import eu.zio.musicsync.model.Artist
import eu.zio.musicsync.model.Track

@Dao
interface AlbumDao {
    @Transaction
    @Query("SELECT * FROM album where id = :id")
    suspend fun getAlbumWithTracks(id: Int): AlbumWithTracks?

    @Transaction
    suspend fun insertAlbumsWithTracks(vararg tracks: AlbumWithTracks) {
        val albums = tracks.map { it.album }
        insertAll(albums)

        val tracks = tracks.flatMap { it.tracks }
        insertAllTracks(tracks)

        tracks
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTracks(albums: List<Track>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<Album>)

    @Insert
    suspend fun insertAllArtists(vararg artist: Artist)
}
