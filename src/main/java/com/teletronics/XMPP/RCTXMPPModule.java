package com.teletronics.XMPP;

import android.content.res.AssetManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.thoughtcrime.ssl.pinning.PinningTrustManager;
import org.thoughtcrime.ssl.pinning.SystemKeyStore;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

public class RCTXMPPModule extends ReactContextBaseJavaModule {

    private static final String TAG = "XMPPHandler";

    private final ReactApplicationContext _reactContext;

    private XMPPTCPConnection connection;

    ChatManager incomingChat;

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
        String pin = "6d77eb40a81dde12001dc32f9c946a3c73caec16";
        String pins[] = {pin};

        XMPPTCPConnectionConfiguration conf = null;
        try {

            KeyStore trustStore = null;
            AssetManager assetManager = getCurrentActivity().getAssets();
            try {
                InputStream keyStoreInputStream = assetManager.open("yourapp.store");
                trustStore = KeyStore.getInstance("BKS");
                trustStore.load(keyStoreInputStream, "test123".toCharArray());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            }

            sc = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            try {
                tmf.init(trustStore);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }

            //MemorizingTrustManager mtm = new MemorizingTrustManager(getCurrentActivity());
            Log.d(TAG, "Using pin: " + pin);
            X509TrustManager pinning = new PinningTrustManager(SystemKeyStore.getInstance(getCurrentActivity()),
                    new String[]{pin}, 0);
            MemorizingTrustManager mtm = new MemorizingTrustManager(getCurrentActivity(), pinning);

            //sc.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
            sc.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());


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
            //builder.setHostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));
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

        incomingChat = ChatManager.getInstanceFor(connection);

        incomingChat.addChatListener(new ChatManagerListener() {

            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {
                chat.addMessageListener(new ChatMessageListener() {
                    @Override
                    public void processMessage(Chat chat, Message message) {
                        Log.d("XMPPTEST", "Got message: " + message.getBody());
                        WritableMap params = Arguments.createMap();
                        params.putString("thread", message.getThread());
                        params.putString("subject", message.getSubject());
                        params.putString("body", message.getBody());
                        params.putString("from", message.getFrom());
                        sendEvent(_reactContext, "XMPPMessage", params);
                    }
                });
            }
        });

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

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
}