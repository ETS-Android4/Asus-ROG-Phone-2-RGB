package terminal_heat_sink.asusrogphone2rgb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Set;

public class NotificationService extends NotificationListenerService {
    private String notifications_on_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.notifications_on";
    private String notifications_animation_on_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.notifications_animation";
    private String fab_on_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.fab_on";
    private String current_selected_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.current_selected";
    private String notifications_second_led_on_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.notifications_second_led";
    private String use_second_led_on_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.use_second_led";
    private String use_notifications_second_led_only_on_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.notifications_second_led_use_only";
    private String apps_selected_for_notifications_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.notifications_apps_selected";

    //timeout keys
    private String use_notifications_timeout_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.use_notifications_timeout_shared_preference_key";
    private String notifications_timeout_seconds_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.notifications_timeout_seconds_shared_preference_key";

    private String SAVED_PREFS_KEY_COLOR = "terminal_heat_sink.asusrogphone2rgb.saved_prefs_key_color";


    //per app_animation keys
    private String package_color_preference_pretext = "sharedPreferencePerAppColor";
    private String package_animation_preference_pretext = "sharedPreferencePerAppAnimationMode";

    //notification animation running? needed for the battery.
    private String notification_animation_running_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.notification_animation_running_shared_preference_key";

    //check if phone is rog 3 then run the loop
    private String isphone_rog3_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.isrog3";

    //check if magisk mode
    private String magisk_mode_shared_preference_key = "terminal_heat_sink.asusrogphone2rgb.magiskmode";
    private boolean magisk_mode;


    private String latest_notification = "";

    Context context ;

    private long time_to_finish_timer;


    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            if(System.currentTimeMillis() >= time_to_finish_timer){
                stopNotificationAndRestore();
            }else{
                timerHandler.postDelayed(this, 1000);
            }

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate () {



        super.onCreate() ;
        context = getApplicationContext() ;

        SharedPreferences prefs = context.getSharedPreferences(
                "terminal_heat_sink.asusrogphone2rgb", Context.MODE_PRIVATE);
        boolean test = prefs.getBoolean(notifications_on_shared_preference_key,false);
        magisk_mode = prefs.getBoolean(magisk_mode_shared_preference_key,false);
        if(test){
            String NOTIFICATION_CHANNEL_ID = "terminal_heat_sink.asusrogphone2rgb";
            String channelName = "Notification Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.RED);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);



            Log.i( "AsusRogPhone2RGBNotificationService" , "Creating Service Notification");

            String isrog3 = prefs.getString(isphone_rog3_shared_preference_key," ");
            if(!isrog3.equals(" ")){
                if(isrog3.charAt(0) == '3'){
                    Log.i("AsusRogPhone2RGBNotificationService","Starting Rog 3 wakelock");
                    SystemWriter.rog_3_wakelock(context);
                }
            }

            if(!magisk_mode) {
                Intent notificationIntent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent =
                        PendingIntent.getActivity(context, 0, notificationIntent, 0);

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
                Notification notification = notificationBuilder.setOngoing(true)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Notification service running")
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationManager.IMPORTANCE_MIN)
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .build();

                startForeground(2, notification);
            }
        }else {
            if(!magisk_mode) {
                stopForeground(true);
            }
            stopSelf();
        }

    }
    @Override
    public void onNotificationPosted (StatusBarNotification sbn) {
        handle_notification(true, sbn.getPackageName());
    }
    @Override
    public void onNotificationRemoved (StatusBarNotification sbn) {
        handle_notification(false, sbn.getPackageName());
    }

    private void handle_notification(boolean added, String package_name){

        SharedPreferences prefs = context.getSharedPreferences(
                "terminal_heat_sink.asusrogphone2rgb", Context.MODE_PRIVATE);
        boolean test = prefs.getBoolean(notifications_on_shared_preference_key,false);

        Log. i ( "AsusRogPhone2RGBNotificationService" , "adding notification:"+added+" notifications_enabled:"+test+" package name:"+package_name);

        if(!test){
            if(!magisk_mode) {
                stopForeground(true);
            }
            stopSelf();
        }else{

            Set<String> apps_to_notify = prefs.getStringSet(apps_selected_for_notifications_shared_preference_key,null);
            if(apps_to_notify != null){
                if(apps_to_notify.contains(package_name)){
                    send_notification(package_name,added);
                }
            }

        }


    }

    private void send_notification(String name, boolean added){

        SharedPreferences prefs = context.getSharedPreferences(
                "terminal_heat_sink.asusrogphone2rgb", Context.MODE_PRIVATE);

        int mode = prefs.getInt(notifications_animation_on_shared_preference_key, 1);

        if(added && (!latest_notification.equals(name))){ //if different notification is added.
            latest_notification = name;
            boolean use_second_led_for_notification = prefs.getBoolean(notifications_second_led_on_shared_preference_key,false);
            boolean use_second_led_only = prefs.getBoolean(use_notifications_second_led_only_on_shared_preference_key,false);
            Log. i ( "AsusRogPhone2RGBNotificationService" , "stating to notify for "+latest_notification+" use second led:"+use_second_led_for_notification+" use second led only:"+use_second_led_only);


            if(mode >= 20){ //using custom animations
                mode = 8; // thunder rainbow
            }else{

            }

            //check if the notification uses custom animations
            int custom_mode = prefs.getInt(package_animation_preference_pretext+name,0);
            boolean use_colour = false;
            int red = 0, green = 0, blue = 0;

            if(custom_mode != 0){
                mode = custom_mode;
                int colour = prefs.getInt(package_color_preference_pretext+name,0);
                if(colour != 0){
                    use_colour = true;
                    red = Color.red(colour);
                    green = Color.green(colour);
                    blue = Color.blue(colour);
                }

            }

            prefs.edit().putBoolean(notification_animation_running_shared_preference_key,true).apply();
            SystemWriter.notification_start(mode,use_colour,red,green,blue,context,use_second_led_for_notification,use_second_led_only);

            boolean use_timeout = prefs.getBoolean(use_notifications_timeout_shared_preference_key,false);
            if(use_timeout){//create timeout
                int timeout_time = prefs.getInt(notifications_timeout_seconds_shared_preference_key,60*30);//default 30 mins
                time_to_finish_timer = System.currentTimeMillis() + timeout_time*1000;
                timerHandler.postDelayed(timerRunnable, 10*1000);//10 seconds because that is the lowest time.
                long temp = System.currentTimeMillis();
                Log.i( "AsusRogPhone2RGBNotificationService" , "Timeout started for "+latest_notification+ " time_to_finish_timer:"+temp+time_to_finish_timer+" time now:"+temp);
            }



        }else if( !added && latest_notification.equals(name)){ // restore to previous config

            stopNotificationAndRestore();
        }


    }



    private void stopNotificationAndRestore(){
        SharedPreferences prefs = context.getSharedPreferences(
                "terminal_heat_sink.asusrogphone2rgb", Context.MODE_PRIVATE);

        Log. i ( "AsusRogPhone2RGBNotificationService" , "stopping notifications because this is cleared "+latest_notification);


        boolean on = prefs.getBoolean(fab_on_shared_preference_key,false);
        int animation = prefs.getInt(current_selected_shared_preference_key,0);
        boolean use_second_led = prefs.getBoolean(use_second_led_on_shared_preference_key,false);

        int color = prefs.getInt(SAVED_PREFS_KEY_COLOR,-1031);


        prefs.edit().putBoolean(notification_animation_running_shared_preference_key,false).apply();
        SystemWriter.notification_stop(!on,animation,true,Color.red(color),Color.green(color),Color.blue(color),context,use_second_led);

        //Log.e("AsusRogPhone2RGBNotificationService", "stop notification: on:"+on+" animation:"+animation+" use_second_led:"+use_second_led);

        latest_notification = "";
    }

    //gets called when the notification access is removed from settings
    @Override
    public void onDestroy() {
        Log.i("AsusRogPhone2RGBNotificationService", "onDestroy() , service stopped...");
        String isRog3 = context.getSharedPreferences("terminal_heat_sink.asusrogphone2rgb", Context.MODE_PRIVATE).getString(isphone_rog3_shared_preference_key," ");
        if(!isRog3.equals(" ")) {
            if (isRog3.charAt(0) == '3') {
                Log.i("AsusRogPhone2RGBNotificationService", "Rog3 Releasing wakelock");
                SystemWriter.rog_3_wakeunlock(context);
            }
        }

        if(!magisk_mode) {
            stopForeground(true);
        }
        stopSelf();
        super.onDestroy();

    }


}