package eu.kanade.tachiyomi.ui.security

import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.startAuthentication
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR

/**
 * Blank activity with a BiometricPrompt.
 */
class UnlockActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // KMK --> 69 mod X2: PIN first, with a button for the phone's own lock
        if (eu.kanade.tachiyomi.util.system.Mod69Pin.isSet(this)) {
            eu.kanade.tachiyomi.util.system.Mod69Pin.ask(
                context = this,
                title = stringResource(MR.strings.unlock_app_title, stringResource(MR.strings.app_name)),
                phoneLockText = stringResource(tachiyomi.i18n.kmk.KMR.strings.mod69_pin_use_phone_lock),
                onCorrect = {
                    SecureActivityDelegate.unlock()
                    finish()
                },
                onPhoneLock = { startPhoneAuthentication() },
                onFail = { finishAffinity() },
            )
            return
        }
        // KMK <--
        startPhoneAuthentication()
    }

    private fun startPhoneAuthentication() {
        startAuthentication(
            stringResource(MR.strings.unlock_app_title, stringResource(MR.strings.app_name)),
            confirmationRequired = false,
            callback = object : AuthenticatorUtil.AuthenticationCallback() {
                override fun onAuthenticationError(
                    activity: FragmentActivity?,
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(activity, errorCode, errString)
                    logcat(LogPriority.ERROR) { errString.toString() }
                    finishAffinity()
                }

                override fun onAuthenticationSucceeded(
                    activity: FragmentActivity?,
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(activity, result)
                    SecureActivityDelegate.unlock()
                    finish()
                }
            },
        )
    }
}
