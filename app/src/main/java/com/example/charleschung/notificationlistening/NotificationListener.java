package com.example.charleschung.notificationlistening;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.RecognizerIntent;
import android.support.annotation.RequiresApi;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.content.Context;
import android.annotation.TargetApi;
import android.os.Build;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
//import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.net.Uri;

//import edu.nctu.minuku.DBHelper.DBHelper;
//import edu.nctu.minuku.manager.DBManager;
//import edu.nctu.minuku_2.NotificationReceiver;
import org.json.JSONObject;
import org.json.JSONException;
import android.telephony.TelephonyManager;
import android.content.Context;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

//import edu.nctu.minuku_2.R;
//import edu.nctu.minuku_2.Receiver.WifiReceiver;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
//import edu.nctu.minuku.streamgenerator.LocationStreamGenerator;
//import com.amplitude.api.Amplitude;
//import com.amplitude.api.Identify;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import static android.app.Activity.RESULT_OK;
import static android.support.v4.app.ActivityCompat.startActivityForResult;


/**
 * Created by kevchentw on 2018/1/20.
 * Modified by that_charles to enble auto reply.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    private NotificationManager mManager;
    private NotificationCompat.Builder mBuilder;
    //private String deviceId;
    private String title;
    private String text;
    private String subText;
    private String tickerText;
    private String app;
    private Boolean send_form;
    private String last_title;
    //Charles
    private String action;

    private SharedPreferences sharedPrefs;

    private Bundle bundle;
    private PendingIntent pendingIntent;

    private int MainActivityCallFlag = 0;

    private Messenger messageHandler;

    /** TTS 物件 */
    private TextToSpeech tts;
    private String IMComefrom;

    /** STT */
    private final int REQ_CODE_SPEECH_INPUT = 100;

    private static class notificationWear{
        public static Bundle bundle;
        public static PendingIntent pendingIntent;
        public static RemoteInput[] remoteInputs;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Notification start");
        Bundle extras = intent.getExtras();
        messageHandler = (Messenger) extras.get("MESSENGER");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Notification bind");

        return super.onBind(intent);
    }

    private static final class ApplicationPackageNames {
        public static final String FACEBOOK_MESSENGER_PACK_NAME = "com.facebook.orca";
        public static final String LINE_PACK_NAME = "jp.naver.line.android";
    }

    public static final class InterceptedNotificationCode {
        public static final int FACEBOOK_MESSENGER_CODE = 1;
        public static final int LINE_CODE = 2;
        public static final int OTHER_NOTIFICATIONS_CODE = 3;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        Log.d(TAG, "Notification received: " + sbn.getPackageName() + " Actions To conduct: " + sbn.getNotification().actions + ":" + sbn.getNotification().tickerText);

        Notification notification = sbn.getNotification();

        try {
            title = notification.extras.get("android.title").toString();
        } catch (Exception e) {
            title = "";
        }
        try {
            text = notification.extras.get("android.text").toString();
        } catch (Exception e) {
            text = "";
        }

        try {
            subText = notification.extras.get("android.subText").toString();
        } catch (Exception e) {
            subText = "";
        }

        try {
            tickerText = notification.tickerText.toString();
        } catch (Exception e) {
            tickerText = "";
        }
        //charles

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(sbn.getNotification());

        List<NotificationCompat.Action> actions = wearableExtender.getActions();
        for(NotificationCompat.Action act : actions) {
            if(act != null && act.getRemoteInputs() != null) {
                notificationWear.remoteInputs = act.getRemoteInputs();
                notificationWear.pendingIntent = act.actionIntent;
                break;
            }
        }

        notificationWear.bundle = sbn.getNotification().extras;

        int notificationCode = matchNotificationCode(sbn);

        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {

            if (sbn.getPackageName().equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME)) {
                app = "fb";
                if (title.contains("聊天大頭貼使用中") || tickerText.contains("傳送了") || tickerText.contains("You missed a call from") || tickerText.contains("你錯過了") || tickerText.contains("sent") || tickerText.contains("reacted") || tickerText.contains("送了一張貼圖") || tickerText.contains("Wi-Fi") || tickerText.isEmpty() || title.isEmpty() || text.isEmpty() || text.contains("：") || text.contains(": ")) {
                    return;
                }
            } else if (sbn.getPackageName().equals(ApplicationPackageNames.LINE_PACK_NAME)) {
                app = "line";
                if (tickerText.contains("貼圖") || tickerText.contains("LINE系統") || tickerText.contains("您有新訊息") || title.contains("LINE未接來電") || title.contains("Missed LINE call") || text.contains("Incoming LINE voice call") || tickerText.contains("LINE未接來電") || title.contains("LINE語音通話來電中") || text.contains("LINE語音通話來電中") || tickerText.contains("傳送了") || tickerText.contains("記事本") || tickerText.contains("已建立") || tickerText.contains("added a note") || tickerText.contains("sent") || tickerText.contains("語音訊息") || tickerText.contains("Wi-Fi") || tickerText.isEmpty() || title.isEmpty() || text.isEmpty() || !subText.isEmpty()) {
                    return;
                }
            }

            Log.d(TAG, "START CHECK");
            Log.d(TAG, "title:" + title);
            Log.d(TAG, "text:" + text);
            Log.d(TAG, "subText" + subText);

            // Filling the RemoteInput
            /*
            RemoteInput[] remoteInputs = new RemoteInput[notificationWear.remoteInputs.length];

            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle localBundle = notificationWear.bundle;
            int i = 0;

            for(RemoteInput remoteIn : notificationWear.remoteInputs){
                remoteInputs[i] = remoteIn;
                localBundle.putCharSequence(remoteInputs[i].getResultKey(), "系統測試中");//This work, apart from Hangouts as probably they need additional parameter (notification_tag?)
                i++;
            }
            RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
            try {
                notificationWear.pendingIntent.send(this.getApplicationContext(), 0, localIntent);
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "replyToLastNotification error: " + e.getLocalizedMessage());
            }
            */


            if (text.contains("測試") || text.contains("Hi")) {


                //Read text out
                if (app.equals("fb")) {
                    IMComefrom = "A new message on facebook messenger from";
                }else if (app.equals("line")) {
                    IMComefrom = "A new message on Line from";
                }

                // 建立 TTS
                createLanguageTTS();
                tts.speak( IMComefrom, TextToSpeech.QUEUE_FLUSH, null );
                while (tts.isSpeaking());
                tts.speak( title, TextToSpeech.QUEUE_FLUSH, null );
                while (tts.isSpeaking());
                tts.speak( text, TextToSpeech.QUEUE_FLUSH, null );
                while (tts.isSpeaking());
                tts.speak( "Are you replying?", TextToSpeech.QUEUE_FLUSH, null );
                while (tts.isSpeaking());

                /*
                if (MainActivityCallFlag == 0){
                    //compose the reply
                    Intent dialogIntent = new Intent(this, MainActivity.class);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);
                    MainActivityCallFlag = 1;
                }
                */

                sendMessage(1238910);
                //promptSpeechInput();

                //reply
                reply(notificationWear.remoteInputs, notificationWear.bundle, notificationWear.pendingIntent);

            }
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public void reply(RemoteInput[] remoteInputs, Bundle bundle, PendingIntent pendingIntent){
        RemoteInput[] remoteInputList = new RemoteInput[remoteInputs.length];

        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = bundle;
        int i = 0;

        for(RemoteInput remoteIn : remoteInputs){
            remoteInputList[i] = remoteIn;
            localBundle.putCharSequence(remoteInputList[i].getResultKey(), "系統測試中");
            i++;
        }
        RemoteInput.addResultsToIntent(remoteInputList, localIntent, localBundle);
        try {
            pendingIntent.send(this.getApplicationContext(), 0, localIntent);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "replyToLastNotification error: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int matchNotificationCode(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if (packageName.equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME)) {
            return (InterceptedNotificationCode.FACEBOOK_MESSENGER_CODE);
        } else if (packageName.equals(ApplicationPackageNames.LINE_PACK_NAME)) {
            return (InterceptedNotificationCode.LINE_CODE);
        } else {
            return (InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE);
        }
    }

    private void createLanguageTTS()
    {
        if( tts == null )
        {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
                @Override
                public void onInit(int arg0)
                {
                    // TTS 初始化成功
                    if( arg0 == TextToSpeech.SUCCESS )
                    {
                        // 指定的語系: 英文(美國)
                        Locale l = Locale.US;  // 不要用 Locale.ENGLISH, 會預設用英文(印度)

                        // 目前指定的【語系+國家】TTS, 已下載離線語音檔, 可以離線發音
                        if( tts.isLanguageAvailable( l ) == TextToSpeech.LANG_COUNTRY_AVAILABLE )
                        {
                            tts.setLanguage( l );
                        }
                    }
                }}
            );
        }
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (this.getApplicationContext() instanceof Activity) {
                ((Activity) this.getApplicationContext()).startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            } else {
                Log.e(TAG,"mContext should be an instanceof Activity.");
            }
            //startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    //@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //txtSpeechInput.setText(result.get(0));
                }
                break;
            }

        }
    }

    public void sendMessage(int data) {
        Message message = Message.obtain();
        message.arg1 = data;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}