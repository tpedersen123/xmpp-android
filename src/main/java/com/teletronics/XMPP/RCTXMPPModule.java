package com.teletronics.XMPP;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.thoughtcrime.ssl.pinning.PinningTrustManager;
import org.thoughtcrime.ssl.pinning.SystemKeyStore;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

public class RCTXMPPModule extends ReactContextBaseJavaModule {

    private static final String TAG = "XMPPHandler";

    private final ReactApplicationContext _reactContext;

    private XMPPTCPConnection connection;

    public RCTXMPPModule(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void connect(String serverName, String host, int port, String userName, String password, boolean testServerMode) {
        Log.d(TAG, "Connect called");
        Log.d(TAG, "-----------------------------------");
        Log.d(TAG, "Got: " + serverName + "," + host + "," + port + "," + userName + "," + password + "," + testServerMode);
        Log.d(TAG, "-----------------------------------");

        SSLContext sc = null;
        XMPPTCPConnectionConfiguration conf = null;
        try {
            sc = SSLContext.getInstance("TLS");
            //MemorizingTrustManager mtm = new MemorizingTrustManager(getCurrentActivity());

            X509TrustManager pinning = new PinningTrustManager(SystemKeyStore.getInstance(getCurrentActivity()),
                    new String[]{"6D77EB40A81DDE12001DC32F9C946A3C73CAEC16"}, 0);
            MemorizingTrustManager mtm = new MemorizingTrustManager(getCurrentActivity(), pinning);
            sc.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
            XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
            builder.setServiceName(serverName);
            if (host != null) {
                Log.d(TAG, "Setting host name " + host);
                builder.setHost(serverName);
            }
            if (port != 0) {
                Log.d(TAG, "Setting port " + port);
                builder.setPort(port);
            }
            builder.setUsernameAndPassword(userName, password);

            builder.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
            builder.setConnectTimeout(15000);
            builder.setDebuggerEnabled(true);
            if (testServerMode) {
                Log.d(TAG, "Test server mode");
                builder.setCustomSSLContext(sc);
            }
            builder.setHostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));
            conf = builder.build();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        connection = new XMPPTCPConnection(conf);
        try {
            XMPPTCPConnection con2 = (XMPPTCPConnection) connection.connect();
            con2.login();
        } catch (XMPPException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (SmackException e) {
            Log.d(TAG, e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }


    }

    @ReactMethod
    public void sendMessage(String receiver, String message) {
        try {
            Chat chat = ChatManager.getInstanceFor(connection).createChat(receiver);
            chat.sendMessage(message);
        } catch (SmackException e) {
            Log.d(TAG, e.getMessage());
        }
    }
}