package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class FingerprintManager implements FingerprintHelper.FingerprintHelperListener {

    private static final String KEY_ALIAS = "sitepoint";
    private static final String KEYSTORE = "AndroidKeyStore";
    private KeyGenerator generator;
    private Cipher cipher;
    private android.hardware.fingerprint.FingerprintManager fingerprintManager;
    private final KeyguardManager keyguardManager;
    private android.hardware.fingerprint.FingerprintManager.CryptoObject cryptoObject;
    private final SharedPreferences sharedPreferences;
    private FingerprintHelper fingerprintHelper;
    private KeyStore keyStore;
    private final Context context;

    private final String TAG = "FingerprintManager";
    private static FingerprintManager instance;

    public static synchronized FingerprintManager getInstance(Context context) {
        if (instance == null)
            instance = new FingerprintManager(context.getApplicationContext());
        return instance;
    }

    private FingerprintManager(Context context) {
        this.context = context.getApplicationContext();
        keyguardManager =
                (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setFingerprintManager((android.hardware.fingerprint.FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE));
            testFingerPrintSettings();
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (Build.VERSION.SDK_INT < 23) {

            Log.i(TAG, "API version does not support fingerprint scanning");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean getCipher() {
        Log.i(TAG, "Getting cipher...");
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.i(TAG, e.getMessage());
        }

        return false;
    }

    private static final String PREFERENCES_KEY_IV = "iv";

    @TargetApi(Build.VERSION_CODES.M)
    private boolean initCipher(int mode) {
        Log.i(TAG, "Initializing cipher...");
        try {
            keyStore.load(null);
            SecretKey keyspec = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

            if (mode == Cipher.ENCRYPT_MODE) {
                cipher.init(mode, keyspec);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREFERENCES_KEY_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP));
                editor.apply();
            } else {
                byte[] iv = Base64.decode(sharedPreferences.getString(PREFERENCES_KEY_IV, ""), Base64.NO_WRAP);
                IvParameterSpec ivspec = new IvParameterSpec(iv);
                cipher.init(mode, keyspec, ivspec);
            }

            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            Log.i(TAG, e.getMessage());
            createNewKey(true); // Retry after clearing entry
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean initCryptObject() {
        Log.i(TAG, "Initializing crypt object...");
        try {
            cryptoObject = new android.hardware.fingerprint.FingerprintManager.CryptoObject(cipher);
            return true;
        } catch (Exception ex) {
            Log.i(TAG, ex.getMessage());
        }
        return false;
    }

    private boolean getKeyStore() {
        Log.i(TAG, "Getting keystore...");
        try {
            keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null); // Create empty keystore
            return true;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            Log.i(TAG, e.getMessage());
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean createNewKey(boolean forceCreate) {
        Log.i(TAG, "Creating new key...");
        try {
            if (forceCreate)
                keyStore.deleteEntry(KEY_ALIAS);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);

                generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .build()
                );

                generator.generateKey();
                Log.i(TAG, "Key created.");
            } else
                Log.i(TAG, "Key exists.");

            return true;
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean testFingerPrintSettings() {
        Log.i(TAG, "Testing Fingerprint Settings:");
        boolean all_ok = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.i(TAG, "This Android version does not support fingerprint authentication.");
            all_ok = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (!keyguardManager.isKeyguardSecure()) {
                Log.i(TAG, "User hasn't enabled Lock Screen security (keyguardManager.isKeyguardSecure())");
                all_ok = false;
            }
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "User hasn't granted permission to use Fingerprint");
            all_ok = false;
        }

        if (!fingerprintManager.hasEnrolledFingerprints()) {
            Log.i(TAG, "User hasn't registered any fingerprints");
            all_ok = false;
        }

        if (!all_ok)
            return false;
        Log.i(TAG, "Fingerprint authentication is set (ok).\n");

        return true;

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void attemptFingerprintLogin() {
        if (!testFingerPrintSettings())
            return;

        fingerprintHelper = new FingerprintHelper(this);

        if (!getKeyStore())
            return;

        if (!createNewKey(false))
            return;

        if (!getCipher())
            return;

        if (!initCipher(Cipher.DECRYPT_MODE))
            return;

        if (!initCryptObject())
            return;

        fingerprintHelper.startAuth(this.fingerprintManager, cryptoObject);
        Log.i(TAG, "Authenticate using fingerprint!");

    }

    @Override
    public void authenticationFailed(String error) {
        Log.i(TAG, error);
    }

    @Override
    public void authenticationSucceeded(android.hardware.fingerprint.FingerprintManager.AuthenticationResult result) {
        Log.i(TAG, result.toString());
    }

    public android.hardware.fingerprint.FingerprintManager getFingerprintManager() {
        return fingerprintManager;
    }

    private void setFingerprintManager(android.hardware.fingerprint.FingerprintManager fingerprintManager) {
        this.fingerprintManager = fingerprintManager;
    }
}
