package com.quicknotes.shahid.app.data.local.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
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
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

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

    private var currentMasterKey: SecretKey? = null
    private var clipboardClearJob: Job? = null
    private var lastCopiedKey: String? = null

    init {
        // Observe app process lifecycle for auto-locking when app enters background
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        applicationScope.launch {
            checkInitialVaultState()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // Auto-lock vault immediately when app goes to background
        lockVault()
    }

    private fun getPersistentVaultFile(): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val vaultDir = File(docsDir, "QuickNotes/.vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        return File(vaultDir, "vault_secure.json")
    }

    suspend fun checkAndRestoreFromPersistentStorage(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getPersistentVaultFile()
            if (!file.exists() || file.length() == 0L) return@withContext false

            val jsonStr = file.readText()
            if (jsonStr.isBlank()) return@withContext false
            val obj = JSONObject(jsonStr)

            val passcodeHash = obj.optString("passcodeHash", "")
            val passcodeSalt = obj.optString("passcodeSalt", "")
            val recoveryHash = obj.optString("recoveryHash", "")
            val recoverySalt = obj.optString("recoverySalt", "")

            if (passcodeHash.isNotBlank() && passcodeSalt.isNotBlank()) {
                preferencesRepository.updateVaultCredentials(
                    passcodeHash = passcodeHash,
                    passcodeSalt = passcodeSalt,
                    recoveryHash = recoveryHash,
                    recoverySalt = recoverySalt
                )
                val info = preferencesRepository.getVaultSecurityInfo()
                _vaultState.value = VaultState.Locked(
                    failedAttempts = info.failedAttempts,
                    lockoutRemainingSeconds = 0
                )
                return@withContext true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun checkInitialVaultState() = withContext(Dispatchers.IO) {
        var info = preferencesRepository.getVaultSecurityInfo()
        if (info.passcodeHash == null || info.passcodeSalt == null) {
            val restored = checkAndRestoreFromPersistentStorage()
            if (!restored) {
                _vaultState.value = VaultState.NotConfigured
                return@withContext
            }
            info = preferencesRepository.getVaultSecurityInfo()
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
        val cleanRecovery = recoveryKey.replace("-", "").trim().uppercase()
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

        // Generate persistent 256-bit AES master key
        val masterKeyBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val masterKey = SecretKeySpec(masterKeyBytes, "AES")
        currentMasterKey = masterKey

        val passKey = securityManager.deriveKey(passcode, passcodeSalt)
        val (encMasterPass, masterPassIv) = securityManager.encryptWithKey(
            Base64.encodeToString(masterKeyBytes, Base64.NO_WRAP),
            passKey
        )

        val recKey = securityManager.deriveKey(cleanRecovery, recoverySalt)
        val (encMasterRec, masterRecIv) = securityManager.encryptWithKey(
            Base64.encodeToString(masterKeyBytes, Base64.NO_WRAP),
            recKey
        )

        val (encPayload, payloadIv) = securityManager.encryptWithKey("[]", masterKey)

        try {
            val pFile = getPersistentVaultFile()
            val persistentObj = JSONObject().apply {
                put("version", 1)
                put("passcodeHash", passcodeHashB64)
                put("passcodeSalt", passcodeSaltB64)
                put("recoveryHash", recoveryHashB64)
                put("recoverySalt", recoverySaltB64)
                put("encMasterKeyWithPasscode", encMasterPass)
                put("masterKeyPasscodeIv", masterPassIv)
                put("encMasterKeyWithRecovery", encMasterRec)
                put("masterKeyRecoveryIv", masterRecIv)
                put("encPayload", encPayload)
                put("payloadIv", payloadIv)
            }
            pFile.writeText(persistentObj.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize empty local keys file
        saveRawEntries(emptyList())

        // Unlock right away
        _vaultState.value = VaultState.Unlocked(emptyList())

        recoveryKey
    }

    suspend fun unlockWithPasscode(passcode: String): Boolean = withContext(Dispatchers.IO) {
        var info = preferencesRepository.getVaultSecurityInfo()
        if (info.passcodeHash == null || info.passcodeSalt == null) {
            checkAndRestoreFromPersistentStorage()
            info = preferencesRepository.getVaultSecurityInfo()
        }

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

            // Derive master key from persistent vault file if available
            try {
                val passKey = securityManager.deriveKey(passcode, saltBytes)
                val pFile = getPersistentVaultFile()
                if (pFile.exists() && pFile.length() > 0L) {
                    val pObj = JSONObject(pFile.readText())
                    val encMaster = pObj.optString("encMasterKeyWithPasscode", "")
                    val ivMaster = pObj.optString("masterKeyPasscodeIv", "")
                    if (encMaster.isNotBlank() && ivMaster.isNotBlank()) {
                        val masterB64 = securityManager.decryptWithKey(encMaster, ivMaster, passKey)
                        val masterBytes = Base64.decode(masterB64, Base64.NO_WRAP)
                        currentMasterKey = SecretKeySpec(masterBytes, "AES")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var decryptedList = loadAndDecryptKeys()

            // If local keys are empty (e.g. fresh reinstall) but persistent file has encrypted keys, restore them!
            if (decryptedList.isEmpty() && currentMasterKey != null) {
                try {
                    val pFile = getPersistentVaultFile()
                    if (pFile.exists() && pFile.length() > 0L) {
                        val pObj = JSONObject(pFile.readText())
                        val encPayload = pObj.optString("encPayload", "")
                        val payloadIv = pObj.optString("payloadIv", "")
                        if (encPayload.isNotBlank() && payloadIv.isNotBlank()) {
                            val plainJson = securityManager.decryptWithKey(encPayload, payloadIv, currentMasterKey!!)
                            val arr = JSONArray(plainJson)
                            val restoredList = mutableListOf<DecryptedApiKey>()
                            val rawEntries = mutableListOf<ApiKeyEntry>()

                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                val id = item.getString("id")
                                val providerName = item.getString("providerName")
                                val plainKey = item.getString("plainKey")
                                val createdAt = item.optLong("createdAt", now)
                                val updatedAt = item.optLong("updatedAt", now)

                                restoredList.add(
                                    DecryptedApiKey(
                                        id = id,
                                        providerName = providerName,
                                        plainKey = plainKey,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )

                                val (cipherB64, ivB64) = securityManager.encrypt(plainKey)
                                rawEntries.add(
                                    ApiKeyEntry(
                                        id = id,
                                        providerName = providerName,
                                        encryptedApiKey = cipherB64,
                                        iv = ivB64,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }

                            if (restoredList.isNotEmpty()) {
                                saveRawEntries(rawEntries)
                                decryptedList = restoredList
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _vaultState.value = VaultState.Unlocked(decryptedList)
            true
        } else {
            val newFailures = info.failedAttempts + 1
            var lockoutUntil = 0L
            var lockoutSeconds = 0

            if (newFailures >= 5) {
                // Lock for 30s after 5, then exponential (30s, 60s, 120s...)
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
        var info = preferencesRepository.getVaultSecurityInfo()
        if (info.recoveryHash == null || info.recoverySalt == null) {
            checkAndRestoreFromPersistentStorage()
            info = preferencesRepository.getVaultSecurityInfo()
        }
        if (info.recoveryHash == null || info.recoverySalt == null) return@withContext false

        val cleanKey = recoveryKey.replace("-", "").trim().uppercase()
        val saltBytes = Base64.decode(info.recoverySalt, Base64.NO_WRAP)
        val computedHash = securityManager.hashWithPbkdf2(cleanKey, saltBytes)
        val computedHashB64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)

        if (computedHashB64 == info.recoveryHash) {
            try {
                val recKey = securityManager.deriveKey(cleanKey, saltBytes)
                val pFile = getPersistentVaultFile()
                if (pFile.exists() && pFile.length() > 0L) {
                    val pObj = JSONObject(pFile.readText())
                    val encMaster = pObj.optString("encMasterKeyWithRecovery", "")
                    val ivMaster = pObj.optString("masterKeyRecoveryIv", "")
                    if (encMaster.isNotBlank() && ivMaster.isNotBlank()) {
                        val masterB64 = securityManager.decryptWithKey(encMaster, ivMaster, recKey)
                        val masterBytes = Base64.decode(masterB64, Base64.NO_WRAP)
                        currentMasterKey = SecretKeySpec(masterBytes, "AES")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true
        } else {
            false
        }
    }

    suspend fun resetPasscodeWithRecovery(newPasscode: String): Boolean = withContext(Dispatchers.IO) {
        val passcodeSalt = securityManager.generateSalt()
        val passcodeHash = securityManager.hashWithPbkdf2(newPasscode, passcodeSalt)

        val passcodeHashB64 = Base64.encodeToString(passcodeHash, Base64.NO_WRAP)
        val passcodeSaltB64 = Base64.encodeToString(passcodeSalt, Base64.NO_WRAP)

        preferencesRepository.updateVaultPasscodeOnly(
            passcodeHash = passcodeHashB64,
            passcodeSalt = passcodeSaltB64
        )

        // If master key is present, re-encrypt it with new passcode key in persistent file
        val masterKey = currentMasterKey
        if (masterKey != null) {
            try {
                val passKey = securityManager.deriveKey(newPasscode, passcodeSalt)
                val (encMasterPass, masterPassIv) = securityManager.encryptWithKey(
                    Base64.encodeToString(masterKey.encoded, Base64.NO_WRAP),
                    passKey
                )
                val pFile = getPersistentVaultFile()
                if (pFile.exists() && pFile.length() > 0L) {
                    val pObj = JSONObject(pFile.readText())
                    pObj.put("passcodeHash", passcodeHashB64)
                    pObj.put("passcodeSalt", passcodeSaltB64)
                    pObj.put("encMasterKeyWithPasscode", encMasterPass)
                    pObj.put("masterKeyPasscodeIv", masterPassIv)
                    pFile.writeText(pObj.toString(2))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var keys = loadAndDecryptKeys()

        // If local keys are empty, restore from persistent payload
        if (keys.isEmpty() && masterKey != null) {
            try {
                val pFile = getPersistentVaultFile()
                if (pFile.exists() && pFile.length() > 0L) {
                    val pObj = JSONObject(pFile.readText())
                    val encPayload = pObj.optString("encPayload", "")
                    val payloadIv = pObj.optString("payloadIv", "")
                    if (encPayload.isNotBlank() && payloadIv.isNotBlank()) {
                        val plainJson = securityManager.decryptWithKey(encPayload, payloadIv, masterKey)
                        val arr = JSONArray(plainJson)
                        val restoredList = mutableListOf<DecryptedApiKey>()
                        val rawEntries = mutableListOf<ApiKeyEntry>()
                        val now = System.currentTimeMillis()

                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val id = item.getString("id")
                            val providerName = item.getString("providerName")
                            val plainKey = item.getString("plainKey")
                            val createdAt = item.optLong("createdAt", now)
                            val updatedAt = item.optLong("updatedAt", now)

                            restoredList.add(
                                DecryptedApiKey(
                                    id = id,
                                    providerName = providerName,
                                    plainKey = plainKey,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            )

                            val (cipherB64, ivB64) = securityManager.encrypt(plainKey)
                            rawEntries.add(
                                ApiKeyEntry(
                                    id = id,
                                    providerName = providerName,
                                    encryptedApiKey = cipherB64,
                                    iv = ivB64,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            )
                        }

                        if (restoredList.isNotEmpty()) {
                            saveRawEntries(rawEntries)
                            keys = restoredList
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _vaultState.value = VaultState.Unlocked(keys)
        true
    }

    fun lockVault() {
        currentMasterKey = null
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
            syncPersistentStorage(decrypted)
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
            syncPersistentStorage(decrypted)
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
            syncPersistentStorage(decrypted)
            _vaultState.value = VaultState.Unlocked(decrypted)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun syncPersistentStorage(plainKeys: List<DecryptedApiKey>) {
        val masterKey = currentMasterKey ?: return
        try {
            val pFile = getPersistentVaultFile()
            val existingObj = if (pFile.exists() && pFile.length() > 0L) {
                JSONObject(pFile.readText())
            } else {
                JSONObject()
            }
            val arr = JSONArray()
            for (key in plainKeys) {
                val item = JSONObject().apply {
                    put("id", key.id)
                    put("providerName", key.providerName)
                    put("plainKey", key.plainKey)
                    put("createdAt", key.createdAt)
                    put("updatedAt", key.updatedAt)
                }
                arr.put(item)
            }
            val (encPayload, payloadIv) = securityManager.encryptWithKey(arr.toString(), masterKey)
            existingObj.put("encPayload", encPayload)
            existingObj.put("payloadIv", payloadIv)
            pFile.writeText(existingObj.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyKeyToClipboard(plainKey: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("API Key", plainKey)
        clipboard.setPrimaryClip(clip)
        lastCopiedKey = plainKey

        // Cancel previous clear job if any
        clipboardClearJob?.cancel()
        // Schedule auto-clear after 30 seconds
        clipboardClearJob = applicationScope.launch {
            delay(30_000L)
            // Clear only if clipboard still contains this key
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