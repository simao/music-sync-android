package eu.zio.musicsync.db

import androidx.room.Database
import androidx.room.Insert
import androidx.room.RoomDatabase
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.Artist
import eu.zio.musicsync.model.Track

@Database(entities = arrayOf(Artist::class, Album::class, Track::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao

}

