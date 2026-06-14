package com.example.myapplication.core;

import android.util.Log;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ExamClient {
    private static ExamClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean isConnected = false;

    private final LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private volatile PushListener pushListener;
    private Thread readerThread;

    private ExamClient() {}

    public static ExamClient getInstance() {
        if (instance == null) instance = new ExamClient();
        return instance;
    }

    public void setPushListener(PushListener listener) { this.pushListener = listener; }

    public void connect(String ip, int port, ConnectCallback callback) {
        new Thread(() -> {
            try {
                closeConnection();
                String cleanIp = ip.replace(" ", "").replace("\n", "").trim();
                socket = new Socket(cleanIp, port);
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                isConnected = true;
                startReaderThread();
                callback.onSuccess();
            } catch (IOException e) {
                isConnected = false;
                callback.onFailure(e.getMessage());
            }
        }).start();
    }

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            while (isConnected && socket != null && !socket.isClosed()) {
                try {
                    String line = in.readLine();
                    if (line == null) { isConnected = false; break; }
                    if (line.startsWith("PUSH_")) handleServerPush(line);
                    else responseQueue.offer(line);
                } catch (IOException e) { isConnected = false; break; }
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void handleServerPush(String line) {
        if (line.startsWith("PUSH_RANKING|")) {
            String rankingData = line.substring(13);
            if (pushListener != null) pushListener.onRankingPushed(rankingData);
        }
    }

    public void sendCommand(String cmd, CommandCallback callback) {
        if (!isConnected()) { callback.onResult("ERR:連線已中斷"); return; }
        new Thread(() -> {
            try {
                out.println(cmd);
                String response = responseQueue.poll(30, TimeUnit.SECONDS);
                callback.onResult(response != null ? response : "ERR:回應逾時");
            } catch (InterruptedException e) { callback.onResult("ERR:Interrupted"); }
        }).start();
    }

    private void closeConnection() {
        isConnected = false;
        responseQueue.clear();
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public interface ConnectCallback { void onSuccess(); void onFailure(String error); }
    public interface CommandCallback { void onResult(String result); }
    public interface PushListener    { void onRankingPushed(String rankingData); }
}
