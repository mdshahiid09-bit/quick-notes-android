package com.quicknotes.shahid.app.data.local.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.quicknotes.shahid.app.data.local.preferences.UserPreferencesRepository
import com.quicknotes.shahid.app.data.local.vault.model.ApiKeyEntry
import com.quicknotes.shahid.app.data.local.vault.model.DecryptedApiKey
import com.quicknotes.shahid.app.data.local.vault.model.VaultState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val securityManager: VaultSecurityManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val applicationScope: CoroutineScope
) : DefaultLifecycleObserver {

    private val vaultFile: File by lazy {
        val dir = File(context.filesDir, "vault")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "vault_keys.json")
    }

    private val _vaultState = MutableStateFlow<VaultState>(VaultState.Locked())
    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    private var clipboardClearJob: Job? = null
    private var lastCopiedKey: String? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        applicationScope.launch {
            checkInitialVaultState()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        lockVault()
    }

    suspend fun checkInitialVaultState() = withContext(Dispatchers.IO) {
        val info = preferencesRepository.getVaultSecurityInfo()
        if (info.passcodeHash == null || info.passcodeSalt == null) {
            _vaultState.value = VaultState.NotConfigured
            return@withContext
        }

        val now = System.currentTimeMillis()
        if (info.lockoutUntil > now) {
            val remainingSec = ((info.lockoutUntil - now) / 1000).toInt()
            _vaultState.value = VaultState.Locked(
                failedAttempts = info.failedAttempts,
                lockoutRemainingSeconds = remainingSec
            )
        } else {
            _vaultState.value = VaultState.Locked(
                failedAttempts = info.failedAttempts,
                lockoutRemainingSeconds = 0
            )
        }
    }

    suspend fun setupVault(passcode: String): String = withContext(Dispatchers.IO) {
        val passcodeSalt = securityManager.generateSalt()
        val passcodeHash = securityManager.hashWithPbkdf2(passcode, passcodeSalt)

        val recoveryKey = securityManager.generateRecoveryKey()
        val cleanRecovery = recoveryKey.replace("-", "")
        val recoverySalt = securityManager.generateSalt()
        val recoveryHash = securityManager.hashWithPbkdf2(cleanRecovery, recoverySalt)

        val passcodeHashB64 = Base64.encodeToString(passcodeHash, Base64.NO_WRAP)
        val passcodeSaltB64 = Base64.encodeToString(passcodeSalt, Base64.NO_WRAP)
        val recoveryHashB64 = Base64.encodeToString(recoveryHash, Base64.NO_WRAP)
        val recoverySaltB64 = Base64.encodeToString(recoverySalt, Base64.NO_WRAP)

        preferencesRepository.updateVaultCredentials(
            passcodeHash = passcodeHashB64,
            passcodeSalt = passcodeSaltB64,
            recoveryHash = recoveryHashB64,
            recoverySalt = recoverySaltB64
        )

        saveRawEntries(emptyList())
        _vaultState.value = VaultState.Unlocked(emptyList())

        recoveryKey
    }

    suspend fun unlockWithPasscode(passcode: String): Boolean = withContext(Dispatchers.IO) {
        val info = preferencesRepository.getVaultSecurityInfo()
        if (info.passcodeHash == null || info.passcodeSalt == null) {
            _vaultState.value = VaultState.NotConfigured
            return@withContext false
        }

        val now = System.currentTimeMillis()
        if (info.lockoutUntil > now) {
            val remaining = ((info.lockoutUntil - now) / 1000).toInt()
            _vaultState.value = VaultState.Locked(
                failedAttempts = info.failedAttempts,
                lockoutRemainingSeconds = remaining
            )
            return@withContext false
        }

        val saltBytes = Base64.decode(info.passcodeSalt, Base64.NO_WRAP)
        val computedHash = securityManager.hashWithPbkdf2(passcode, saltBytes)
        val computedHashB64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)

        if (computedHashB64 == info.passcodeHash) {
            preferencesRepository.resetVaultFailures()
            val decryptedList = loadAndDecryptKeys()
            _vaultState.value = VaultState.Unlocked(decryptedList)
            true
        } else {
            val newFailures = info.failedAttempts + 1
            var lockoutUntil = 0L
            var lockoutSeconds = 0

            if (newFailures >= 5) {
                val multiplier = 1 shl (newFailures - 5).coerceAtMost(6)
                lockoutSeconds = 30 * multiplier
                lockoutUntil = now + (lockoutSeconds * 1000L)
            }

            preferencesRepository.recordVaultFailure(newFailures, lockoutUntil)
            _vaultState.value = VaultState.Locked(
                failedAttempts = newFailures,
                lockoutRemainingSeconds = lockoutSeconds,
                errorMessage = "Incorrect passcode"
            )
            false
        }
    }

    suspend fun verifyRecoveryKey(recoveryKey: String): Boolean = withContext(Dispatchers.IO) {
        val info = preferencesRepository.getVaultSecurityInfo()
        if (info.recoveryHash == null || info.recoverySalt == null) return@withContext false

        val cleanKey = recoveryKey.replace("-", "").trim().uppercase()
        val saltBytes = Base64.decode(info.recoverySalt, Base64.NO_WRAP)
        val computedHash = securityManager.hashWithPbkdf2(cleanKey, saltBytes)
        val computedHashB64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)

        computedHashB64 == info.recoveryHash
    }

    suspend fun resetPasscodeWithRecovery(newPasscode: String): Boolean = withContext(Dispatchers.IO) {
        val passcodeSalt = securityManager.generateSalt()
        val passcodeHash = securityManager.hashWithPbkdf2(newPasscode, passcodeSalt)

        preferencesRepository.updateVaultPasscodeOnly(
            passcodeHash = Base64.encodeToString(passcodeHash, Base64.NO_WRAP),
            passcodeSalt = Base64.encodeToString(passcodeSalt, Base64.NO_WRAP)
        )

        val keys = loadAndDecryptKeys()
        _vaultState.value = VaultState.Unlocked(keys)
        true
    }

    fun lockVault() {
        if (_vaultState.value is VaultState.Unlocked) {
            _vaultState.value = VaultState.Locked()
        }
    }

    suspend fun addApiKey(providerName: String, plainKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (cipherB64, ivB64) = securityManager.encrypt(plainKey)
            val now = System.currentTimeMillis()
            val newEntry = ApiKeyEntry(
                id = UUID.randomUUID().toString(),
                providerName = providerName.trim(),
                encryptedApiKey = cipherB64,
                iv = ivB64,
                createdAt = now,
                updatedAt = now
            )
            val entries = loadRawEntries().toMutableList()
            entries.add(newEntry)
            saveRawEntries(entries)

            val decrypted = loadAndDecryptKeys()
            _vaultState.value = VaultState.Unlocked(decrypted)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateApiKey(id: String, providerName: String, plainKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (cipherB64, ivB64) = securityManager.encrypt(plainKey)
            val now = System.currentTimeMillis()
            val entries = loadRawEntries().map { entry ->
                if (entry.id == id) {
                    entry.copy(
                        providerName = providerName.trim(),
                        encryptedApiKey = cipherB64,
                        iv = ivB64,
                        updatedAt = now
                    )
                } else {
                    entry
                }
            }
            saveRawEntries(entries)

            val decrypted = loadAndDecryptKeys()
            _vaultState.value = VaultState.Unlocked(decrypted)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteApiKey(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entries = loadRawEntries().filter { it.id != id }
            saveRawEntries(entries)

            val decrypted = loadAndDecryptKeys()
            _vaultState.value = VaultState.Unlocked(decrypted)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun copyKeyToClipboard(plainKey: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("API Key", plainKey)
        clipboard.setPrimaryClip(clip)
        lastCopiedKey = plainKey

        clipboardClearJob?.cancel()
        clipboardClearJob = applicationScope.launch {
            delay(30_000L)
            try {
                val currentClip = clipboard.primaryClip
                if (currentClip != null && currentClip.itemCount > 0) {
                    val currentText = currentClip.getItemAt(0).text?.toString()
                    if (currentText == lastCopiedKey) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadAndDecryptKeys(): List<DecryptedApiKey> {
        val rawEntries = loadRawEntries()
        val list = mutableListOf<DecryptedApiKey>()
        for (entry in rawEntries) {
            try {
                val plain = securityManager.decrypt(entry.encryptedApiKey, entry.iv)
                list.add(
                    DecryptedApiKey(
                        id = entry.id,
                        providerName = entry.providerName,
                        plainKey = plain,
                        createdAt = entry.createdAt,
                        updatedAt = entry.updatedAt
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    private fun loadRawEntries(): List<ApiKeyEntry> {
        if (!vaultFile.exists()) return emptyList()
        return try {
            val jsonStr = vaultFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ApiKeyEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ApiKeyEntry(
                        id = obj.getString("id"),
                        providerName = obj.getString("providerName"),
                        encryptedApiKey = obj.getString("encryptedApiKey"),
                        iv = obj.getString("iv"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRawEntries(entries: List<ApiKeyEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("providerName", entry.providerName)
            obj.put("encryptedApiKey", entry.encryptedApiKey)
            obj.put("iv", entry.iv)
            obj.put("createdAt", entry.createdAt)
            obj.put("updatedAt", entry.updatedAt)
            array.put(obj)
        }
        vaultFile.writeText(array.toString(2))
    }
}
