package com.haddadian.universalremote.security
import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSecretStore(private val context: Context) {
    private val alias="UniversalTVRemote.Secret.v1"
    private val prefs=context.getSharedPreferences("secure_secrets",Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (ks.getKey(alias,null) as? SecretKey)?.let{return it}
        val kg=KeyGenerator.getInstance("AES","AndroidKeyStore")
        kg.init(android.security.keystore.KeyGenParameterSpec.Builder(alias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
            android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return kg.generateKey()
    }
    fun put(name:String,value:String){
        val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key())
        val data=Base64.encodeToString(c.doFinal(value.toByteArray(StandardCharsets.UTF_8)),Base64.NO_WRAP)
        val iv=Base64.encodeToString(c.iv,Base64.NO_WRAP)
        prefs.edit().putString(name,"$iv:$data").apply()
    }
    fun get(name:String):String?=runCatching{
        val p=prefs.getString(name,null)?.split(":",limit=2)?:return null
        val c=Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)))
        String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),StandardCharsets.UTF_8)
    }.getOrNull()
    fun remove(name:String){prefs.edit().remove(name).apply()}
}