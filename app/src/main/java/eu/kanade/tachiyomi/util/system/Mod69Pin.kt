package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.text.InputType
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.security.MessageDigest
import java.security.SecureRandom

// KMK -->
/** 69 mod X2: a PIN that unlocks the app instead of (or next to) the phone's own lock. */
object Mod69Pin {

    private fun prefs(context: Context) = context.getSharedPreferences("mod69_pin", Context.MODE_PRIVATE)

    fun isSet(context: Context): Boolean = prefs(context).getString("hash", null) != null

    private fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun set(context: Context, pin: String) {
        val salt = ByteArray(8).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        prefs(context).edit().putString("salt", salt).putString("hash", hash(pin, salt)).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove("salt").remove("hash").apply()
    }

    fun verify(context: Context, pin: String): Boolean {
        val salt = prefs(context).getString("salt", null) ?: return false
        return prefs(context).getString("hash", null) == hash(pin, salt)
    }

    private fun pinInput(context: Context) = EditText(context).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        setPadding(64, 32, 64, 32)
    }

    /** Asks for the PIN. [onCorrect] runs when it is right, [onFail] when the person gives up. */
    fun ask(
        context: Context,
        title: String,
        phoneLockText: String?,
        onCorrect: () -> Unit,
        onPhoneLock: (() -> Unit)?,
        onFail: () -> Unit,
    ) {
        val input = pinInput(context)
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("OK", null)
            .setNegativeButton(android.R.string.cancel) { _, _ -> onFail() }
        if (phoneLockText != null && onPhoneLock != null) {
            builder.setNeutralButton(phoneLockText) { _, _ -> onPhoneLock() }
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (verify(context, input.text.toString())) {
                    dialog.dismiss()
                    onCorrect()
                } else {
                    input.error = "✗"
                    input.text.clear()
                }
            }
        }
        dialog.show()
    }

    /** Asks for a new PIN (4 to 8 digits), twice. */
    fun askNew(context: Context, title: String, again: String, mismatch: String, onSet: () -> Unit) {
        val first = pinInput(context)
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(first)
            .setPositiveButton("OK") { _, _ ->
                val pin = first.text.toString()
                if (pin.length !in 4..8) {
                    android.widget.Toast.makeText(context, "4-8", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val second = pinInput(context)
                MaterialAlertDialogBuilder(context)
                    .setTitle(again)
                    .setView(second)
                    .setPositiveButton("OK") { _, _ ->
                        if (second.text.toString() == pin) {
                            set(context, pin)
                            onSet()
                        } else {
                            android.widget.Toast.makeText(context, mismatch, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
// KMK <--
