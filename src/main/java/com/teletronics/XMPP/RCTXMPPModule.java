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
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

public class RCTXMPPModule extends ReactContextBaseJavaModule {

    private static final String TAG = "XMMPHandler";

    private final ReactApplicationContext _reactContext;

    public RCTXMPPModule(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void connect(String serverName, String userName, String password) {
        SSLContext sc = null;
        XMPPTCPConnectionConfiguration conf = null;
        try {
            sc = SSLContext.getInstance("TLS");
            MemorizingTrustManager mtm = new MemorizingTrustManager(getCurrentActivity());
            sc.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
            conf = XMPPTCPConnectionConfiguration.builder()
                    .setServiceName(serverName).setUsernameAndPassword(userName, password)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                    .setConnectTimeout(15000)
                    .setDebuggerEnabled(true)
                    .setCustomSSLContext(sc)
                    .setHostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()))
                    .build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        XMPPTCPConnection connection = new XMPPTCPConnection(conf);

        try {
            XMPPTCPConnection con2 = (XMPPTCPConnection) connection.connect();
            con2.login();
            Chat chat = ChatManager.getInstanceFor(connection)
                    .createChat(userName, new ChatMessageListener() {
                        @Override
                        public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                            Log.d("TAG", "Got message: " + message.getBody());
                        }
                    });
            chat.sendMessage("Howdy!");
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
