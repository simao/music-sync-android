package eu.zio.musicsync

import android.net.Uri
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import eu.zio.musicsync.model.*
import org.json.JSONException
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@JsonClass(generateAdapter = true)
class JsonResponse<T>(val values: List<T>)

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

class ArtworkResponse(val mimetype: String?, val bytes: ByteBuffer)

class ArtworkRequest(url: String, private val ok: Response.Listener<ArtworkResponse>, error: Response.ErrorListener) : Request<ArtworkResponse>(Method.GET, url, error) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<ArtworkResponse> {
        val bytes = response?.data?.let {
            ByteBuffer.wrap(it)
        }

        return if (bytes == null) {
            Response.error(VolleyError("Empty response received"))
        } else {
            val contentType = response.headers["content-type"]
            Response.success(ArtworkResponse(contentType, bytes), HttpHeaderParser.parseCacheHeaders(response))
        }
    }

    private val mLock = Any()

    override fun deliverResponse(response: ArtworkResponse?) {
        var listener: Response.Listener<ArtworkResponse>?
        synchronized(mLock) { listener = ok }
        listener?.onResponse(response)
    }
}

class MusicSyncHttpClient(private val queue: RequestQueue) {
    private val url = Uri.parse("http://192.168.1.101:3030/")!!

    private val moshi = Moshi.Builder().build()

    fun trackAudioUri(albumId: Int, trackId: String): Uri {
        return url.buildUpon().appendEncodedPath("albums/${albumId}/tracks/${trackId}/audio").build()
    }

    private fun albumCoverUri(albumId: Int): Uri {
        return url.buildUpon().appendEncodedPath("albums/${albumId}/artwork").build()
    }

    suspend fun fetchAlbumArtwork(album: Album) = suspendCoroutine<Result<ArtworkResponse>> { cont ->
        val req = ArtworkRequest(albumCoverUri(album.id).toString(),
            Response.Listener {
                cont.resume(Result.success(it))
            },
            Response.ErrorListener { error: VolleyError ->
                Timber.e(error)
                cont.resumeWithException(error)
            }
        )

        queue.add(req)
    }

    suspend fun albumTracks(albumId: Int) = suspendCoroutine<Result<List<Track>>> { cont ->
        val type = Types.newParameterizedType(JsonResponse::class.java, JsonTrack::class.java)
        val adp = moshi.adapter<JsonResponse<JsonTrack>>(type)

        val jsonObjectRequest = MoshiJsonRequest(
            Request.Method.GET, url.buildUpon().appendEncodedPath("albums/${albumId}/tracks").build(), adp,
            Response.Listener { response ->
                val res = response.values.map {
                    Track(it.id, it.name, it.filename, it.album.id, it.album.name)
                }

                cont.resume(Result.success(res))
            },
            Response.ErrorListener { error: VolleyError ->
                Timber.e(error)
                cont.resume(Result.failure(error))
            }
        )

        queue.add(jsonObjectRequest)
    }

    suspend fun fetchArtistAlbums(artist: String?) = suspendCoroutine<Result<List<Album>>> { cont ->
        val type = Types.newParameterizedType(JsonResponse::class.java, JsonAlbum::class.java)
        val adp = moshi.adapter<JsonResponse<JsonAlbum>>(type)

        val u = url.buildUpon().appendPath("albums").appendQueryParameter("artist", artist).build()

        val jsonObjectRequest = MoshiJsonRequest(
            Request.Method.GET, u, adp,
            Response.Listener { response ->
                val res = response.values.map {
                    Album(it.id, it.name, it.year, it.artist.name)
                }

                cont.resume(Result.success(res))
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
            Request.Method.GET, url.buildUpon().appendPath("artists").build(), adp,
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