package eu.zeroio.musicsync

import android.content.SharedPreferences
import android.net.Uri
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import eu.zeroio.musicsync.model.Artist
import eu.zeroio.musicsync.model.FullAlbum
import eu.zeroio.musicsync.model.JsonResponse
import eu.zeroio.musicsync.model.Track
import eu.zeroio.musicsync.ui.SettingsActivity
import org.json.JSONException
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MoshiJsonRequest<T>(method: Int, url: Uri, private val adapter: JsonAdapter<T>,
                          ok: Response.Listener<T>,
                          error: Response.ErrorListener) : JsonRequest<T>(method, url.toString(), null, ok, error) {

    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            val cs = Charset.forName(HttpHeaderParser.parseCharset(response!!.headers, PROTOCOL_CHARSET))
            val jsonString = String(response.data, cs)

            val r = adapter.fromJson(jsonString)

            Response.success(r, HttpHeaderParser.parseCacheHeaders(response))

        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (je: JSONException) {
            Response.error(ParseError(je))
        }
    }
}

class ArtworkResponse(val filename: String?, val bytes: ByteBuffer)

class ArtworkRequest(url: String, private val ok: Response.Listener<ArtworkResponse>, error: Response.ErrorListener) : Request<ArtworkResponse>(Method.GET, url, error) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<ArtworkResponse> {
        val bytes = response?.data?.let {
            ByteBuffer.wrap(it)
        }

        return if (bytes == null) {
            Response.error(VolleyError("Empty response received"))
        } else {
            val disposition = response.headers["content-disposition"]

            val re = Regex("filename=\"(.+)\"")

            val filename = disposition?.let {
                re.find(it)?.groups?.get(1)?.value
            }

            Response.success(ArtworkResponse(filename, bytes), HttpHeaderParser.parseCacheHeaders(response))
        }
    }

    private val mLock = Any()

    override fun deliverResponse(response: ArtworkResponse?) {
        var listener: Response.Listener<ArtworkResponse>?
        synchronized(mLock) { listener = ok }
        listener?.onResponse(response)
    }
}

class MusicSyncHttpClient(private val queue: RequestQueue, private val serverUri: Uri) {
    companion object {
        fun from(queue: RequestQueue, sharedPreferences: SharedPreferences): Result<MusicSyncHttpClient> {
            val settingsServerAddress =
                sharedPreferences.getString(SettingsActivity.KEY_PREF_SERVER_ADDRESS, null)

            val serverUri = settingsServerAddress?.let { Uri.parse(it) }
                ?: return Result.failure(IllegalArgumentException("Invalid server url. Set a valid URL in Settings"))

            return Result.success(MusicSyncHttpClient(queue, serverUri))
        }
    }

    private val moshi = Moshi.Builder().build()

    fun trackAudioUri(albumId: Int, trackId: Int): Uri {
        return serverUri.buildUpon().appendEncodedPath("albums/${albumId}/tracks/${trackId}/audio").build()
    }

    private fun albumCoverUri(albumId: Int): Uri {
        return serverUri.buildUpon().appendEncodedPath("albums/${albumId}/artwork").build()
    }

    suspend fun fetchAlbumArtwork(albumId: Int) = suspendCoroutine<Result<ArtworkResponse>> { cont ->
        val req = ArtworkRequest(albumCoverUri(albumId).toString(),
            Response.Listener {
                cont.resume(Result.success(it))
            },
            Response.ErrorListener { error: VolleyError ->
                Timber.e(error)
                cont.resume(Result.failure(error))
            }
        )

        queue.add(req)
    }

    suspend fun albumTracks(albumId: Int) = suspendCoroutine<Result<List<Track>>> { cont ->
        val type = Types.newParameterizedType(JsonResponse::class.java, Track::class.java)
        val adp = moshi.adapter<JsonResponse<Track>>(type)

        val jsonObjectRequest = MoshiJsonRequest(
            Request.Method.GET, serverUri.buildUpon().appendEncodedPath("albums/${albumId}/tracks").build(), adp,
            Response.Listener { response ->
                cont.resume(Result.success(response.values))
            },
            Response.ErrorListener { error: VolleyError ->
                Timber.e(error)
                cont.resume(Result.failure(error))
            }
        )

        queue.add(jsonObjectRequest)
    }

    suspend fun fetchArtistAlbums(artist: Int) = suspendCoroutine<Result<List<FullAlbum>>> { cont ->
        val type = Types.newParameterizedType(JsonResponse::class.java, FullAlbum::class.java)
        val adp = moshi.adapter<JsonResponse<FullAlbum>>(type)

        val u = serverUri.buildUpon()
            .appendPath("artists")
            .appendPath(artist.toString())
            .appendPath("full-albums")
            .build()

        val jsonObjectRequest = MoshiJsonRequest(
            Request.Method.GET, u, adp,
            Response.Listener { response ->
                cont.resume(Result.success(response.values))
            },
            Response.ErrorListener { error ->
                Timber.e(error)
                cont.resume(Result.failure(error))
            }
        )

        queue.add(jsonObjectRequest)
    }

    suspend fun fetchArtists() = suspendCoroutine<Result<List<Artist>>> { cont ->
        val type = Types.newParameterizedType(JsonResponse::class.java, Artist::class.java)
        val adp = moshi.adapter<JsonResponse<Artist>>(type)

        val jsonObjectRequest = MoshiJsonRequest(
            Request.Method.GET, serverUri.buildUpon().appendPath("artists").build(), adp,
            Response.Listener { response ->
                cont.resume(Result.success(response!!.values))
            },
            Response.ErrorListener { error ->
                Timber.e(error)
                cont.resume(Result.failure(error))
            }
        )

        queue.add(jsonObjectRequest)
    }
}