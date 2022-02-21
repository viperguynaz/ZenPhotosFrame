package dev.zenrg.zenphotoframe

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var credential: SignInCredential
    private lateinit var androidAccount: Account

    private val tag = "zenx"
    private val signIn: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            credential = oneTapClient.getSignInCredentialFromIntent(activityResult.data)
            Log.d(tag, "signIn.credential token: ${credential.googleIdToken}")
            try {
                // As documented, we return a completed Task in this case and it's safe to directly call
                // getResult(Class<ExceptionType>) here (without need to worry about IllegalStateException).
                val googleAccount = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data).getResult(ApiException::class.java)
                getAuthTokenForAccount(googleAccount!!)
            } catch (apiException: ApiException) {
                Log.wtf(tag, "Unexpected error parsing sign-in result")
            }
        }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Log.d(tag, "handler.looper: $msg")
        }
    }

    private var token: String? by Delegates.observable(null) {
            property, oldValue, newValue ->  Log.d(tag, "${property.name}: $oldValue -> $newValue")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (googleAccount != null) {
            getAuthTokenForAccount(googleAccount)
        } else {
            beginOneTapSignIn()
        }
    }

    private fun getAuthTokenForAccount(googleAccount: GoogleSignInAccount) {
        Log.d(tag, "googleAccount: ${googleAccount.email}")
        androidAccount = googleAccount.account!!
        val options = Bundle()
        val accountManager = AccountManager.get(this)
        accountManager.getAuthToken(
            androidAccount,                     // Account retrieved using getAccountsByType()
            "oauth2:https://www.googleapis.com/auth/photoslibrary.readonly",            // Auth scope
            options,                        // Authenticator-specific options
            this,                           // Your activity
            OnTokenAcquired(),              // Callback called when a token is successfully acquired
            handler,              // Callback called if an error occurs
        )
    }

    private fun beginOneTapSignIn() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.client_id_web))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) {
                try {
                    Log.d(tag, "oneTapClient.beginSignIn.addOnSuccessListener: no account, launch one-tap")
                    signIn.launch(
                        IntentSenderRequest
                            .Builder(it.pendingIntent.intentSender)
                            .build()
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(tag, "oneTapClient.beginSignIn.addOnSuccessListener Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(tag, "oneTapClient.beginSignIn.addOnFailureListener: ${e.localizedMessage!!}")
            }
            .addOnCanceledListener {
                Log.d(tag, "oneTapClient.beginSignIn.addOnCanceledListener: cancelled")
            }
    }

    private class OnTokenAcquired : AccountManagerCallback<Bundle> {
        override fun run(result: AccountManagerFuture<Bundle>) {
            // Get the result of the operation from the AccountManagerFuture.
            val bundle: Bundle = result.result

            // The token is a named value in the bundle. The name of the value
            // is stored in the constant AccountManager.KEY_AUTHTOKEN.
            MainActivity().token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
        }
    }
}