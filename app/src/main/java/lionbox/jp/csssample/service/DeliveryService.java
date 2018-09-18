package lionbox.jp.csssample.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import lionbox.jp.csssample.R;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static lionbox.jp.csssample.Constants.ACTION_FORWARD;
import static lionbox.jp.csssample.Constants.ACTION_SERVER;
import static lionbox.jp.csssample.Constants.F_STATUS_STANDBY;
import static lionbox.jp.csssample.Constants.J_STATUS_CAMERA_OPEN;
import static lionbox.jp.csssample.Constants.J_STATUS_SHUTTER;
import static lionbox.jp.csssample.Constants.PARAM_CODE;
import static lionbox.jp.csssample.Constants.PARAM_FILE_PATH;
import static lionbox.jp.csssample.Constants.PARAM_IP_V4;
import static lionbox.jp.csssample.Constants.PARAM_STATUS;
import static lionbox.jp.csssample.Constants.PARAM_TOKEN;
import static lionbox.jp.csssample.Constants.S_STATUS_OPEN;
import static lionbox.jp.csssample.Constants.S_STATUS_UPLOAD;
import static lionbox.jp.csssample.Constants.F_STATUS_CAMERA_OPEN;
import static lionbox.jp.csssample.Constants.F_STATUS_SHUTTER;
import static lionbox.jp.csssample.Constants.F_STATUS_UPLOAD_OK;

public class DeliveryService extends Service {

    /**
     * local broadcast manager
     */
    private LocalBroadcastManager mBroadcastReceiver;

    /**
     * CentralReceiver
     */
    private CentralReceiver mCentralReceiver;

    /**
     * token
     */
    private String Token;

    /**
     * ocde
     */
    private String Code;

    public DeliveryService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initialize();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("TAG", "--stop command");
    }

    /**
     * initialize
     */
    private void initialize() {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = LocalBroadcastManager.getInstance(getApplicationContext());
            mCentralReceiver = new CentralReceiver();
            // レシーバのフィルタをインスタンス化
            final IntentFilter filter = new IntentFilter();
            // フィルタのアクション名を設定する（文字列の内容は任意）
            filter.addAction(ACTION_SERVER);
            // 登録
            mBroadcastReceiver.registerReceiver(mCentralReceiver, filter);
        }

        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveServer();
            }
        });
        th.start();
    }
    /**
     * Receive Server
     */
    public void receiveServer() {
        try (ServerSocket listener = new ServerSocket();) {
            int portNo = getResources().getInteger(R.integer.port_no);
            listener.setReuseAddress(true);
            listener.bind(new InetSocketAddress(portNo));
            System.out.println("Server listening on port Constants.PORT_NO...");
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while (true) {
                byte[] buffer = new byte[1024];
                bout.reset();
                try (Socket socket = listener.accept();) {
                    InputStream from = socket.getInputStream();
                    int len = from.read(buffer);
                    if (len < 0) {
                        break;
                    }
                    bout.write(buffer, 0, len);
                    JSONObject json = new JSONObject(new String(bout.toByteArray(), "UTF-8"));
                    Log.d("TAG", json.toString());
                    onReceive(json);
//                    OutputStream to = socket.getOutputStream();
                }

            }
        } catch (JSONException je) {
            je.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onReceive(JSONObject json) {
        try {
            int status = 0;
            if (json.has("status")) {
                status = json.getInt("status");
                switch (status) {
                    case J_STATUS_CAMERA_OPEN:
                        Token = json.getString("token");
                        Intent intent = new Intent();
                        intent.setAction(ACTION_FORWARD);
                        intent.putExtra(PARAM_STATUS, F_STATUS_CAMERA_OPEN);
                        mBroadcastReceiver.sendBroadcast(intent);
                        break;
                    case J_STATUS_SHUTTER:
                        String token = json.getString("token");
                        if (token == null || !token.equals(Token)) {
                            return;
                        }
                        Intent intent1 = new Intent();
                        intent1.setAction(ACTION_FORWARD);
                        intent1.putExtra(PARAM_STATUS, F_STATUS_SHUTTER);
                        mBroadcastReceiver.sendBroadcast(intent1);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * local broadcast receiver
     */
    private class CentralReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (ACTION_SERVER.equals(intent.getAction())) {
                    int status = intent.getIntExtra(PARAM_STATUS, 0);
                    switch (status) {
                        case S_STATUS_OPEN: // server登録
                            String ipAddress = intent.getStringExtra(PARAM_IP_V4);
                            Code = intent.getStringExtra(PARAM_CODE);
                            regist(ipAddress, Code);

                            Intent intent1 = new Intent();
                            intent1.setAction(ACTION_FORWARD);
                            intent1.putExtra(PARAM_STATUS, F_STATUS_STANDBY);
                            mBroadcastReceiver.sendBroadcast(intent1);
                            break;
                        case S_STATUS_UPLOAD:  // 写真のアップロード依頼
                            String filePath = intent.getStringExtra(PARAM_FILE_PATH);
                            postUL(filePath, Code, Token);

                            Intent intent2 = new Intent();
                            intent2.setAction(ACTION_FORWARD);
                            intent2.putExtra(PARAM_STATUS, F_STATUS_UPLOAD_OK);
                            mBroadcastReceiver.sendBroadcast(intent2);
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * post regist
     * @param ipAddress
     * @param code
     */
    private void regist(final String ipAddress, final String code) {
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            String url = getResources().getString(R.string.server_path) + "/" + getResources().getString(R.string.regist_path);

            RequestBody formBody = new FormBody.Builder()
                    .add("code", code)
                    .add("ipaddress", ipAddress)
                    .build();

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            String result = null;
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                {
                    result = response.body().string();
                    Log.d("TAG", result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            emitter.onComplete();
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();

    }

    /**
     * post up load
     * @param filePath
     * @param code
     * @param token
     */
    private void postUL(final String filePath, final String code, final String token) {
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            String url = getResources().getString(R.string.server_path) + "/" + getResources().getString(R.string.upload_path);
            File file = new File(filePath);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("image/jpg"), file))
                    .addFormDataPart("code", code)
                    .addFormDataPart("token", token)
                    .build();

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            String result = null;
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                {
                    result = response.body().string();
                    Log.d("TAG", result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            emitter.onComplete();
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();

    }

}
