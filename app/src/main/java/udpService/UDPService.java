
package udpService;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import database.DatabaseHelper;
import utility.FrameData;
import utility.Trace;

public class UDPService extends Service implements Runnable {

    private static final String TAG = "udpService";
    private final Binder binder_ = new UDPService.UDPBinder();
    public DatagramSocket localSocket = null;
    public InetAddress remoteIPAddress = null;
    public int remotePort = 5000;
    public int localPort = 55555;
    //this IP need to be changed if you change your WiFi connection
    String remoteIPName = "";
    private Boolean UDPThreadRunning = null;


    public class UDPBinder extends Binder {
        public UDPService getService() {
            return UDPService.this;
        }
        public void sendData(FrameData data, InetAddress remoteIPAddress, int remotePort){
            send(data, remoteIPAddress, remotePort);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        return binder_;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    WifiManager wifiManager;
    WifiManager.WifiLock lockHigh;

    private void startService() {

        wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        lockHigh = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HIGH_WIFI");
        lockHigh.acquire();

        Log.d(TAG,"Start UDP server");
        try {
            localSocket = new DatagramSocket(localPort);
            remoteIPAddress = InetAddress.getByName(remoteIPName);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        (new Thread(this)).start();
    }



    public void onDestroy() {
        Log.d(TAG,"udpserver connection is closed");
        stopSelf();
        UDPThreadRunning = false;

        localSocket.close();
        localSocket = null;

        lockHigh.release();
    }


    public void run() {
        // TODO Auto-generated method stub
        Log.d(TAG, "start receiving thread");
        byte[] receiveData = new byte[65555];
        UDPThreadRunning = true;
        while (UDPThreadRunning.booleanValue()) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                localSocket.receive(receivePacket);
                long roundBackTime = System.currentTimeMillis();

                String sentence = new String(receiveData, 0, receivePacket.getLength());
                //get UDPClient ip and port
                remoteIPAddress = receivePacket.getAddress();
                remotePort = receivePacket.getPort();

                if (sentence.length()!=0) {
                    sendFrame(frameProcess(sentence,roundBackTime));
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //process data
    private String frameProcess(String frame, long roundBackTime){
        Gson gson = new Gson();
        FrameData frameData = gson.fromJson(frame, FrameData.class);
        frameData.roundLatency = roundBackTime - frameData.getVideoSendTime();

        Log.d(TAG, gson.toJson(frameData));

        return gson.toJson(frameData);
    }

    //send data back to UDPClient
    public void send(FrameData sendData, InetAddress remoteIPAddress, int remotePort) {
        Gson gson = new Gson();
        //sendData.frameData = "";
        Log.d(TAG, String.valueOf(sendData.getVideoSendTime()));

        String json = gson.toJson(sendData);
        Log.d(TAG, sendData.getDataSize() + " " + json.length());
        Log.d(TAG, gson.toJson(sendData));

        FrameData tmp = gson.fromJson(json, FrameData.class);
        Log.d(TAG, String.valueOf(tmp.getVideoSendTime()));

        DatagramPacket sendPacket = new DatagramPacket(gson.toJson(sendData).getBytes(), gson.toJson(sendData).getBytes().length, remoteIPAddress, remotePort);
        //Log.d(TAG,"gson.toJson(sendData) " + gson.toJson(sendData).toString());
        try {
            localSocket.send(sendPacket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //send received frame data to main
    private void sendFrame(String frameData) {
        Intent intent = new Intent("udp");
        intent.putExtra("latency", frameData);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
