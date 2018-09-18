package lionbox.jp.csssample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import lionbox.jp.csssample.service.DeliveryService;
import lionbox.jp.csssample.utils.Utilities;

import static lionbox.jp.csssample.Constants.ACTION_FORWARD;
import static lionbox.jp.csssample.Constants.ACTION_SERVER;
import static lionbox.jp.csssample.Constants.F_STATUS_CAMERA_OPEN;
import static lionbox.jp.csssample.Constants.F_STATUS_SHUTTER;
import static lionbox.jp.csssample.Constants.F_STATUS_STANDBY;
import static lionbox.jp.csssample.Constants.F_STATUS_UPLOAD_OK;
import static lionbox.jp.csssample.Constants.PARAM_CODE;
import static lionbox.jp.csssample.Constants.PARAM_IP_V4;
import static lionbox.jp.csssample.Constants.PARAM_STATUS;
import static lionbox.jp.csssample.Constants.S_STATUS_OPEN;

public class MainActivity extends AppCompatActivity {
    /**
     * local broadcast manager
     */
    private LocalBroadcastManager mBroadcastReceiver;

    /**
     * CentralReceiver
     */
    private CentralReceiver mCentralReceiver;

    /**
     * edit text
     */
    private EditText mCodeEdit;

    /**
     * status view
     */
    private TextView mStatusView;

    private Button mOnButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCodeEdit = findViewById(R.id.device_code);
        mStatusView = findViewById(R.id.status);
        mOnButton = findViewById(R.id.on_service_button);
        initialize();
    }

    private void initialize() {
        // バックグラウンド起動
        startService(new Intent(getBaseContext(),DeliveryService.class));

        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = LocalBroadcastManager.getInstance(getApplicationContext());
            mCentralReceiver = new CentralReceiver();
            // レシーバのフィルタをインスタンス化
            final IntentFilter filter = new IntentFilter();
            // フィルタのアクション名を設定する（文字列の内容は任意）
            filter.addAction(ACTION_FORWARD);
            // 登録
            mBroadcastReceiver.registerReceiver(mCentralReceiver, filter);
        }
    }

    public void onService(View view) {
        String code = mCodeEdit.getText().toString();
        Intent intent = new Intent();
        intent.setAction(ACTION_SERVER);
        intent.putExtra(PARAM_STATUS, S_STATUS_OPEN);
        intent.putExtra(PARAM_IP_V4, Utilities.getIpV4Address());
        intent.putExtra(PARAM_CODE, code);
        mBroadcastReceiver.sendBroadcast(intent);
    }

    /**
     * local broadcast receiver
     */
    private class CentralReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (ACTION_FORWARD.equals(intent.getAction())) {
                    int status = intent.getIntExtra(PARAM_STATUS, 0);
                    switch (status) {
                        case F_STATUS_STANDBY: // スタンバイOK
                            mStatusView.setText("スタンバイ OK!");
                            break;
                        case F_STATUS_CAMERA_OPEN: // カメラOPEN
                            Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                            startActivity(cameraIntent);
                            break;
                        case F_STATUS_SHUTTER: // カメラシャッター
                            break;
                        case F_STATUS_UPLOAD_OK: // UPLOAD OK
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
