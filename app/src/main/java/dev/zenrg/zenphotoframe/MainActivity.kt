package dev.zenrg.zenphotoframe

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import dev.zenrg.zenphotoframe.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.System.currentTimeMillis
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {

    private lateinit var signInClient: GoogleSignInClient
    private lateinit var imgSwitcher: ImageSwitcher
    private lateinit var bitmapDrawable: BitmapDrawable
    private lateinit var timer: Timer
    private lateinit var sharedPreferences: SharedPreferences
    //    private lateinit var wakeLock: PowerManager.WakeLock
    // add to manifest---> <uses-permission android:name="android.permission.WAKE_LOCK" />
    //    private lateinit var wifiLock: WifiManager.WifiLock

    private var random = Random()
    private val viewModel: MainViewModel by viewModels()
    private val tag = "zenx"
    private val scope = "oauth2:https://www.googleapis.com/auth/photoslibrary.readonly"
    private val job = Job()
    private val scopeIO = CoroutineScope(job + Dispatchers.IO)
    private var googleAccount: GoogleSignInAccount? = null

    // used to bootstrap mediaItems while we get signed in
    private var mediaItems = mutableSetOf<String>(
        "AOUbmad5S_95sSPRn8RZldu02PDsD3z53z9XOEYHX8DY02XhDh2nAYjU_MWh9Jq4qdooFenMHqga_wLN8DhSHCXl9AvpofnqlQ",
        "AOUbmadZiypM2-kAwAZKT_gtMCl_ixFOOokvMb8oPqPsFg82swVg9qH2vf6kD04RcGYaaIkUTrv8sxa9tbmIXR4mHLxFgxRaDg",
        "AOUbmadzOHc5ab14broWyh63-MbodGLNSARhnCRMhLdd4F0ibwAG-7b7RlnD48LM8Pzc2Prp_SRVFwXyOVAHrxQ22varVw3N7g",
        "AOUbmaeDKGO7RQOhZzraPDu8Xi-nbRSaS-11E_ng_xXADZPoWsosOjEVQW2OGd5rE1cT37KXsqCGKB5F5fImNdVrzw89aUT2fg",
        "AOUbmae7Ul1YfwTswd6tDIwbyiG_TlxNKR8wndsWMCRDye_2nM2Itn-bsNYmA_PNcBl4_Iw5etfVW7FmSosPme32HKt6tzrMKQ",
        "AOUbmafRU-TX0985ngDZC2G_NWqRlya5c3k0w50hM4MBTem-FPZPbZLhFGyAcxxvqF2UxXKFwbjRQT7RtP5gZqHsiWjYpQaAzg",
        "AOUbmacZMxMoW3ni4d0uQUXqq02ATAC8zhuGheJvL6f4Ilyok5-r0R6lXUqZ2vyb5ZbnFitYHBgZ7YQyACN9X3tV2jYw74xSWQ",
        "AOUbmafoVyhquIu42OVv_veIiagp8QdQgiP_gv_R4py_LX1-fzDqNWlSX57j29WnRxejLEE9tygdGNzs3BihkQMnjB8fO5A6cw",
        "AOUbmaccjMEue3qFn3FP-I1U3HAsFOntBahu0aVtpbSzTs-wx6WL--wa4eSXnoFsYzFiHXSTbFAZi1WGrIGh9TqCsxOhbno73w",
        "AOUbmadgZH0a19FKzbFvBThlWqQc36gU4wtlTUp1qiHYRM05sP1kWJLSSDONCsbe0iZuW6pv4C5JeKJ_caW7D5dXt_-GL7Ko0w",
        "AOUbmaeHtJT5d_FV9Qbsfdmgv9dosbiD1HkSVkdeKqkECwB44puBQRBV9mfUofTuAFiiLeyIGC3mfw-vaXUkLTf1LRBYTby5Cg",
        "AOUbmafQV324DW4UrvmKgMBrYtndfAjZ4Dqhqdz-Nk_E8oyvBqmdERxyhRIs5Uhnh6evGEiWpFwAHAeGhxnYJhGBpb1Hy4HVRA",
    )

    private var signInActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }
    }

    private var token: String? by Delegates.observable(null) { _, _, newValue ->
        newValue?.let {
            viewModel.setToken(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getCachedItems()
        hideSystemBars()
        signInWithGoogle()
        setupViewModel()
        buildImageSwitcher()
    }

    private fun getCachedItems() {
        sharedPreferences = this.getPreferences(MODE_PRIVATE)
        val mediaItemsCached: Set<String>? = sharedPreferences.getStringSet("zen_media_items", null)
        if (mediaItemsCached != null) {
            mediaItems = mediaItemsCached.toMutableSet()
        }
    }

    private fun setupViewModel() {
        // setup viewModel observers
        viewModel.mediaItemIdsLive.observe(this) { items ->
            items?.let {
                mediaItems = it
                with(sharedPreferences.edit()) {
                    putStringSet("zen_media_items", mediaItems)
                    apply()
                }
                findViewById<TextView>(R.id.loadingText).visibility = View.GONE
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                imgSwitcher.visibility = View.VISIBLE
                timer = fixedRateTimer(name = "imageLooper", initialDelay = 20000, period = 20000, action = {
                    runOnUiThread {
                        if (googleAccount?.expired()!!) {
                            Log.d(tag, "account expired - trying silent sign-on")
                            silentSignIn()
                        }
                        imgSwitcher.setImageDrawable(bitmapDrawable)
                        viewModel.getBitmap(mediaItems.elementAt(random.nextInt(mediaItems.size)))
                    }
                })

            }
        }
        viewModel.authToken.observe(this) {
            Log.d(tag, "viewModel.authToken set -- token: $it")
            viewModel.getBitmap(mediaItems.elementAt(random.nextInt(mediaItems.size)))
            viewModel.getMediaItems()
        }

        // updates the imageswitcher image when a new bitmap is received
        viewModel.bitmapLive.observe(this) {
            bitmapDrawable = BitmapDrawable(resources, it)
        }
    }

    private fun buildImageSwitcher() {
        imgSwitcher = findViewById(R.id.imageSwitcher)
        bitmapDrawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources ,R.drawable.p1))
        imgSwitcher.setFactory {
            val imgView = ImageView(applicationContext)
            imgView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imgView
        }
        imgSwitcher.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        imgSwitcher.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        imgSwitcher.setImageDrawable(bitmapDrawable)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
//        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
//            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ZenPhotoFrame::ZenWakelockTag").apply {
//                acquire()
//            }
//        }
//        wifiLock = (getSystemService(Context.WIFI_SERVICE) as WifiManager).run {
//            createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ZenPhotoFrame:ZenWifiLock").apply {
//                acquire()
//            }
//        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. Here the system is setting
            // only the bottom, left, and right dimensions, but apply whichever insets are
            // appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(
                    left = insets.left,
                    bottom = insets.right,
                    right = insets.right,
                )
            }

            // Return CONSUMED if you don't want want the window insets to keep being
            // passed down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        val windowInsetsController: WindowInsetsControllerCompat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.getWindowInsetsController(window.decorView) ?: return   //findViewById(android.R.id.content)
        } else {
            ViewCompat.getWindowInsetsController(findViewById(android.R.id.content)) ?: return
        }

        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun signInWithGoogle() {
        // Build a GoogleSignInClient with the options specified by gso to refresh tokens
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.client_id_web))
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary.readonly"))
            .build()
        signInClient = GoogleSignIn.getClient(this, gso)
        googleAccount = GoogleSignIn.getAccountForScopes(this, Scope(scope))
        when {
            googleAccount == null -> {
                silentSignIn()
            }
            googleAccount!!.isExpired -> {
                signInActivity.launch(signInClient.signInIntent)
            }
            else -> {
                setAccount(googleAccount)
                Log.d(tag, "GoogleSignIn.getAccountForScopes email: ${googleAccount!!.email}")
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun setAccount(signedInAccount: GoogleSignInAccount?) {
        try {
            googleAccount = signedInAccount
            when {
                googleAccount == null -> {
                    signInActivity.launch(signInClient.signInIntent)
                }
                googleAccount!!.expired() -> {
                    silentSignIn()
                }
                else -> {
                    scopeIO.launch {
                        try {
                            token = GoogleAuthUtil.getToken(this@MainActivity, googleAccount!!.account!!, scope)
                        } catch (transientEx: IOException) {
                            // network or server error, the call is expected to succeed if you try again later.
                            // Don't attempt to call again immediately - the request is likely to
                            // fail, you'll hit quotas or back-off.
                        } catch (e: UserRecoverableAuthException) {
                            // Recover -- trying to access GoogleAuthUtil with expired account - launch sign-in
                            signInActivity.launch(e.intent)
                        } catch (authEx: GoogleAuthException) {
                            // Failure. The call is not expected to ever succeed so it should not be retried.
                            authEx.printStackTrace()
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, e.message.toString())
        }
    }

    private fun silentSignIn() {
        try {
            val task: Task<GoogleSignInAccount> = signInClient.silentSignIn()
            if (task.isSuccessful) {
                setAccount(task.result)
            } else {
                task.addOnCompleteListener {
                    try {
                        setAccount(it.result)
                    } catch (e: ApiException) {
                        Log.w(tag, "silentSignIn:addOnCompleteListener API Exception statusCode: ${e.statusCode} | message: ${e.message}")
                    }
                }
            }
        } catch(e: ApiException) {
            Log.w(tag, "silentSignIn:task.isSuccessful API Exception statusCode: ${e.statusCode} | message: ${e.message}")
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            googleAccount = completedTask.getResult(ApiException::class.java)
            setAccount(googleAccount)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(tag, "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun GoogleSignInAccount.expired() : Boolean {
        return javaClass.getDeclaredField("zaj").let {
            it.isAccessible = true
            val zaj = it.getLong(this)
            return@let currentTimeMillis() / 1000L >= zaj;
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        timer.cancel()
//        wakeLock.release()
//        wifiLock.release()
    }
}