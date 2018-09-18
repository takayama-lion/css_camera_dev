package lionbox.jp.csssample;

public class Constants {
    public static final String ACTION_SERVER = "ACTION_NAME_SERVER";
    public static final String ACTION_FORWARD = "ACTION_NAME_FORWARD";

    public static final String CAMERA_DIR = "camera_tmp";

    // param
    public static final String PARAM_STATUS = "PARAM_STATUS";
    public static final String PARAM_IP_V4  = "PARAM_IPV_4";
    public static final String PARAM_CODE   = "PARAM_CODE";
    public static final String PARAM_TOKEN  = "PARAM_TOKEN";
    public static final String PARAM_FILE_PATH  = "PARAM_FILE_PATH";

    // forwardからserviceへ
    public static final int S_STATUS_OPEN   = 1; // server登録
    public static final int S_STATUS_UPLOAD = 2; // 写真のアップロード依頼

    // serviceからforward
    public static final int F_STATUS_STANDBY     = 1; // スタンバイOK
    public static final int F_STATUS_CAMERA_OPEN = 2; // カメラOPEN
    public static final int F_STATUS_SHUTTER     = 3; // カメラシャッター
    public static final int F_STATUS_UPLOAD_OK   = 4; // スタンバイOK

    // json code
    public static final int J_STATUS_CAMERA_OPEN = 1;
    public static final int J_STATUS_SHUTTER     = 2;

}
