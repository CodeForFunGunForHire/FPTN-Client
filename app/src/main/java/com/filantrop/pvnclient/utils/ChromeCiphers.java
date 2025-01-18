package com.filantrop.pvnclient.utils;

import android.util.Log;

import com.filantrop.pvnclient.vpnclient.exception.EmptyCiphersException;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

public class ChromeCiphers
{
    private static final String[] CHROME_CIPHERS = new String[]{
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_PSK_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_PSK_WITH_AES_256_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
            "TLS_FALLBACK_SCSV"
    };

    private final SSLContext context;
    public ChromeCiphers(SSLContext context) {
        this.context = context;
    }

    public String[] getAvailableCiphers() throws EmptyCiphersException {
        String[] supportedCiphers = context.getSocketFactory().getSupportedCipherSuites();
        for (String cipher : supportedCiphers) {
            Log.d(getTag(), "> " + cipher);
        }
        List<String> chiphersResult = new ArrayList<>();

        for (String cipher : CHROME_CIPHERS) {
            for (String supported : supportedCiphers) {
                if (cipher.equals(supported)) {
                    chiphersResult.add(cipher);
                    break;
                }
            }
        }
        if (chiphersResult.isEmpty()) {
            throw new EmptyCiphersException("The list of supported ciphers is empty.");
        }
        // Create an SSL socket factory with our all-trusting manager
        return chiphersResult.toArray(new String[0]);
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }
}
