package eu.zio.musicsync

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import eu.zio.musicsync.model.Album
import eu.zio.musicsync.model.Artist
import eu.zio.musicsync.model.Track
import org.json.JSONException
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@JsonClass(generateAdapter = true)
class JsonResponse<T>(val values: List<T>)

class MoshiJsonRequest<T>(method: Int, url: String, private val adapter: JsonAdapter<T>,
                          ok: Response.Listener<T>,
                          error: Response.ErrorListener) : JsonRequest<T>(method, url, null, ok, error) {

    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            val cs = Charset.forName(HttpHeaderParser.parseCharset(response!!.headers, PROTOCOL_CHARSET))
            val jsonString = String(response!!.data, cs)

            val r = adapter.fromJson(jsonString)

            Response.success(r, HttpHeaderParser.parseCacheHeaders(response))

        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (je: JSONException) {
            Response.error(ParseError(je))
        }
    }
}

class ArtworkRequest(url: String, private val file: File, private val ok: Response.Listener<File>, error: Response.ErrorListener) : Request<File>(Method.GET, url, error) {
    private fun parseFilename(headers: Map<String, String>): String? {
        val contentType = headers["content-disposition"]
        if (contentType != null) {
            val params =
                contentType.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in 1 until params.size) {
                val pair =
                    params[i].trim { it <= ' ' }.split("=".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                if (pair.size == 2) {
                    if (pair[0] == "filename") {
                        return pair[1].replace("\"", "")
                    }
                }
            }
        }

        return null
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<File> {
        val n = response?.headers?.let { parseFilename(it) }
        val p = file.resolve(n ?: "artwork.jpg") // TODO: Always jpg? Check headers?

        p.parentFile!!.mkdirs() // Check result

        response?.data?.let {
            val out = FileOutputStream(p)
            out.write(it)
            out.close()
            // Files.write(p.toPath(), it, StandardOpenOption.CREATE)
        }

        return Response.success(p, HttpHeaderParser.parseCacheHeaders(response))
    }

    private val mLock = Any()

    override fun deliverResponse(response: File?) {
        var listener: Response.Listener<File>?
        synchronized(mLock) { listener = ok }
        listener?.onResponse(response)
    }
}

class MusicSyncHttpClient(private val queue: RequestQueue) {
    private val url = "http://192.168.1.109:3030/"

    private val moshi = Moshi.Builder().build()

    fun trackAudioUri(albumId: Int, trackId: String): Uri {
        return Uri.parse(url + "albums/${albumId}/tracks/${trackId}/audio")
    }

    fun albumCoverUri(albumId: Int): Uri {
        return Uri.parse(url + "albums/${albumId}/artwork")
    }

    suspend fun fetchAlbumArtwork(album: Album, destDir: File) = suspendCoroutine<Result<File>> { cont ->
        val req = ArtworkRequest(albumCoverUri(album.id).toString(), destDir,
            Response.Listener { response: File ->
                cont.resume(Result.success(response))
            },
            Response.ErrorListener { error: VolleyError ->
                Timber.e(error)
                cont.resumeWithException(error)
            }
        )

        queue.add(req)
    }

    suspend fun albumTracks(albumId: Int) = suspendCoroutine<Result<List<Track>>> { cont ->
        val type = Types.newParameterizedType(JsonResponse::class.java, Track::class.java)
        val adp = moshi.adapter<JsonResponse<Track>>(type)

        val jsonObjectRequest = MoshiJsonRequest(
            Request.Method.GET, url + "albums/${albumId}/tracks", adp,
            Response.Listener { response ->
                cont.resume(Result.success(response!!.values)) // TODO: !!
            },
            Response.ErrorListener { error: VolleyError ->
                Timber.e(error)
                cont.resumeWithException(error)
            }
        )

        queue.add(jsonObjectRequest)
    }

    suspend fun fetchArtistAlbums(artist: String?) = suspendCoroutine<Result<List<Album>>> { cont ->
        val type = Types.newParameterizedType(JsonResponse::class.java, Album::class.java)
        val adp = moshi.adapter<JsonResponse<Album>>(type)

        val u = Uri.parse(url).buildUpon().appendPath("albums").appendQueryParameter("artist", artist)

        val jsonObjectRequest = MoshiJsonRequest(
            Request.Method.GET, u.toString(), adp,
            Response.Listener { response ->

                cont.resume(Result.success(response!!.values))
            },
            Response.ErrorListener { error ->
                cont.resumeWithException(error)
                Timber.e(error)
            }
        )

        queue.add(jsonObjectRequest)
    }

    fun fetchArtists(livedata: MutableLiveData<List<Artist>>, errorMessage: MutableLiveData<String>) {
        val type = Types.newParameterizedType(JsonResponse::class.java, Artist::class.java)
        val adp = moshi.adapter<JsonResponse<Artist>>(type)

        val jsonObjectRequest = MoshiJsonRequest(
            Request.Method.GET, url + "artists", adp,
            Response.Listener { response ->
                livedata.postValue(response!!.values)
            },
            Response.ErrorListener { error ->
                errorMessage.value = "Could not fetch artists"
                Timber.e(error)
            }
        )

        queue.add(jsonObjectRequest)
    }
}