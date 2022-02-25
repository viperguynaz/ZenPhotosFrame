package dev.zenrg.zenphotoframe.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.MutableLiveData
import dev.zenrg.zenphotoframe.models.*
import dev.zenrg.zenphotoframe.network.GooglePhotosApi
import dev.zenrg.zenphotoframe.network.GooglePhotosApi.errorConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.*

@Suppress("BlockingMethodInNonBlockingContext")
class GooglePhotosRepository {
    private val client = GooglePhotosApi.retrofitClient
    val mediaItemsLive = MutableLiveData<MutableList<MediaItem>>()
    val bitmapLive = MutableLiveData<Bitmap>()
    private val mediaItems = mutableListOf<MediaItem>()
    private val albums:List<Album> = listOf(
        Album(
            id = "AOUbmae2mrHDBRKIyxpZDCFomIZIG3X8cEJSVCq8eFPqLqHUpkwqD-KcwIOY_-AhXVWcha5WBJdk",
            title = "Groot",
            productUrl = "https://photos.google.com/lr/album/AOUbmae2mrHDBRKIyxpZDCFomIZIG3X8cEJSVCq8eFPqLqHUpkwqD-KcwIOY_-AhXVWcha5WBJdk",
            mediaItemsCount = "128",
            coverPhotoBaseUrl = "https://lh3.googleusercontent.com/lr/AFBm1_ZdfbqewR0r9ugwqYnFOI7aTXRMNyEbxnW_0DZcffQSPqmJmbn1kqsUNLz_XTKr2dmr-dZpfe-wJi4W3E1FVM1itqGDosuXV3i61P4NkjDTmaRI968G0NyOKIwtfdvg7em6vuefSdWou_dyFMB92cscAbKydXlip7tOZQ14i02_3vn0ATWIM4abGr1Ua63xS9rOHU-GeprfOMiUmKNEpGRuX0B5O_HjMhg4_dA7eNlG2KSCO1eXUylEPZfldnypV89WRcjqD2WkepcrO3P3b-jUkTIPsMq9GCINSpsow9efayCnoiqCHGXqk7csKp2acd_emoIzfWM-pONOkWENoNqtwxEfVjtNeKLhgzas4I6aHVPH4oQgInz57ajbD0yYkVe7oazpm-0WsC3YizXAeOzQvO_XLF8TYXbi2oHCKZxg_BJ57UtAIBa2hpMRXIRtp3Taw_ZX1FI3LQu7FHevHtbxgJP8AKQ-buWnG_u0Ngd7gFv8W9dfT_EtV8H_Y-1_kphiKSzR_6tZGxulfqlte6nvSod3ApYTeGcY2UpFhF3u58TV8yV23l0gSdaqdpA6JiM8_qE2EnGGokxbW8rUgu9zbcHk0_MTFx5ETsK5kS-lXbrCqywqwM9E1PTj1E6y2bSrd-j-OmLKCWBq6hQwfMbG4-pNgiJVA8wOjattsm3y11C1djYcBO2XoT_juwAtru-RxarRECA65uNdfSPPTT-UATz_3L-Us7Q_LjLG9fBDgNvTFooRomwbMgzAVlRzK-Mdukvs_lNhzjF_spIg-M4GcHt-blPrgyRRySY",
            coverPhotoMediaItemId = "AOUbmafa841J1dUfcSlm_OBV4yqrA9HMKNfUQxP1f9C26tWpGlsOh3yezLvzXblQ0_n-ANnEmQbewOHAukwEqjLk9a5ywOjVVA"
        ),
        Album(
            id = "AOUbmae6_j2_W6dHgo9L6ZBv_Fk-Q3w8r4Bvr6pit_Y_CsQdrouVypJCaI2q_62zm4sJUQZGwExb",
            title = "Roxie",
            productUrl = "https://photos.google.com/lr/album/AOUbmae6_j2_W6dHgo9L6ZBv_Fk-Q3w8r4Bvr6pit_Y_CsQdrouVypJCaI2q_62zm4sJUQZGwExb",
            mediaItemsCount = "205",
            coverPhotoBaseUrl = "https://lh3.googleusercontent.com/lr/AFBm1_bF8D7Nps9hVUcR4PyfTMOC7KDi7ZedewIZcUVkzPwmYd5a3Y-qGKMgHuFj2Ja6NwekYDVwFW4ozVmV21huZscJ3QiZSJLgKG7-XflHt-rsN4EMpNhx8EY-G0NcP3X5RfCVuTh70BEr6PONwo4OqOXju9tpO7JfOZ4R8N64nFz-Egz-rrwssvu5_gYhIZdwMZjp9rm8U0tBWb3QtINhPIXkp5fpuF7kVb1TovU87k3zcsWQ6NQpsCGxxXcWaiUY6s36Q57PN2JXRUWCv88N4p_yerd5aiuiWUGIt936-QNefSNPxI4X_DFBLDh7tp4E-I6OTyRoV8CN5ZC7SkXlzpc-FUSu9kt7NEDLiJMZbtzJjBN37a9k0RgUjMGXoCldLDle34BMyJfXgAWscnU--jRkQSdYQrAW0ysShByetza0_YHp40XTsl7n3tPbYKJuVZ5UpqxlehnTpGEtH-9B18HMyms_kCQqTJ2L5JsXwcB-NKrt7ooofvABQY9f_LrlXVeMlnsNYgMJtPS-pZCX85g4mFrJjb9Xl8yJ8-L-g1U98s9tPTtjIfHX8_-scgiMPSHCMX-Frv-KgBEU9Mtp2kZKfrK49IqXPsvcDueGBaZoByBxF1_FFSR5jIZaf9hgqw5ARL0j_AE65F4Yc1kHZQULa0duN47dXtNtpmH78oLEWaMG0WdOjGFWcb8uzjXMAxN6c0tjskSmysDQ36tnRCHUwUiUyUIEOHqxYfF8nkHfBAF0iUS8qMBttPdPH7-AfHuRB2o58CVX26hEiaoelQ-63enzTsa6TKsDnVs",
            coverPhotoMediaItemId = "AOUbmadElNCtYZ1ipj0jlynE3Mz0q-5loSsc6vRo_hziRZzlB2Y8dv3J-j7qNZ5qXRgvLqhTaPziplGbW50DZCz6dIUDNSZluA"
        ),
        Album(
            id = "AOUbmafP5V-h-LJVEyUgVUS0Oc1FKOBNh0v2_O4QY3HASFBkRvSeeqf8So0dH6yVzKXy_1W8pi2m",
            title = "Sophia",
            productUrl = "https://photos.google.com/lr/album/AOUbmafP5V-h-LJVEyUgVUS0Oc1FKOBNh0v2_O4QY3HASFBkRvSeeqf8So0dH6yVzKXy_1W8pi2m",
            mediaItemsCount = "214",
            coverPhotoBaseUrl = "https://lh3.googleusercontent.com/lr/AFBm1_ah1Ie2geZkBIlgwxs4luGMv8OU-kB579gIcz1_hFeA4DFD_aj89z2UoeKSLC8uGou2yxGMah6IbLqLtaD9e9JAHn3FT0wYSi-oibtE0Fdguylrb3ySRICZ-dY74sDWdI7BybV8ciGPlcITZ9q4Q3nSCD2F3cuBZQCHIZNy25dLjHxUdkvI6GUUUdIZJrtkv2AFnlGM70v8FU5dyKPyfI1ZyzEDUVIJVwsketyo8aVNis57O6hmxiauay1d2KijIYFtsNMJWIQv3bZXJR9QZM7NTxDcny5xNr62i31JpRu343CYXMDPGMWcMHZ_xl2vkXnBh2-MAysZHTeXZ227OGBzvNoAtuJJuZvO3Ojc3Cl90Yt4V2sUvtAaf6yVsuanVPLyxH4wl1sZwsWcqePC9u4WvDpXRSfPCKriOh387EFlHKUVA0CrNDOH_7K9TkHYOxpYC00wP4Wt49elK2VaLKMLlsJE8dVvqzvdtPHdaMfSQeJeo60PZJVQ1zRd9NpYak5izezaKCBdq6kLf1A4-7QFv7Ody7hsYGmBjBkzuEHkRI3aT3-GqV9R0K67ATF0lp_uiphWTUmXCl_BDsLYAkrGNKa33d2KWBG7h8bZJJiDoeAGlhHX4Hqf6GOKUJlbPOpABoPG6512ub_76pp8Rd93EGHoYXhBgnd2cowC8OCBs1ZN0vu56o4DvXWla5F5uwyPI8qEyfLMCDCwRnqbGNCoNortw_tOnLeOWoSmU02oVV1vuq4RHCqS-rdoPXmrOm0Yw4xV2G09cTaXGuXLVdUxGnbe_Q9FKoYVGug",
            coverPhotoMediaItemId = "AOUbmaea7mtpkgA0C8qSSVNOwAoAcJojYgD3MIfVlIvM-tvKLCoZQd3SxeBk2jHRnBQ-D0bJr5AkM1TRPTt7X_cK-C7ghF97Sw"
        ),
    )

    suspend fun searchMediaItems(token: String) {
        for (album in albums) {
            val mediaSearchRequest = MediaSearchRequest(albumId = album.id)
            var mediaSearchResponse: MediaSearchResponse
            var moreToRead = true
            while (moreToRead) {
                val response = client.searchMediaItems(token, mediaSearchRequest)
                if (response.isSuccessful) {
                    if (response.body() != null) {
                        mediaSearchResponse = response.body()!!
                        mediaSearchResponse.let {
                            it.mediaItems?.let {
                                    items -> mediaItems.addAll(items)
                            }
                            mediaSearchRequest.pageToken = mediaSearchResponse.nextPageToken
                            moreToRead = !mediaSearchResponse.nextPageToken.isNullOrEmpty()
                        }
                    } else {
                        moreToRead = false
                        mediaSearchRequest.pageToken = null

                        withContext(Dispatchers.IO) {
                            val errorBody = errorConverter.convert(response.errorBody()!!)
                            Log.e(
                                "GooglePhotosRepository.searchMediaItems",
                                "error: ${errorBody?.error?.message}"
                            )
                        }
                    }
                }
            }
        }
        mediaItemsLive.postValue(mediaItems.filter{ item -> item.mediaMetadata?.photo != null}.toMutableList())
    }

    suspend fun getBitmap(token: String, url: String) {
        val response = client.getImage(token, url)
        if (response.isSuccessful) {
            if (response.body() != null) {
                bitmapLive.postValue(BitmapFactory.decodeStream(response.body()!!.byteStream()))
            }
        }
    }
}
