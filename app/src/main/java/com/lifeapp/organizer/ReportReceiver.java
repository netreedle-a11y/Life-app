package com.lifeapp.organizer;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;

public class ReportReceiver extends BroadcastReceiver {
    static final String ACTION="com.lifeapp.organizer.REPORT";
    static void channel(Context c) {
        NotificationManager n=c.getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel("reports","Отчёты Life App",NotificationManager.IMPORTANCE_DEFAULT));
    }
    static PendingIntent alarm(Context c) {
        return PendingIntent.getBroadcast(c,23,new Intent(c,ReportReceiver.class).setAction(ACTION),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static JSONArray queue(Context c) {
        try {return new JSONArray(c.getSharedPreferences("reports",0).getString("queue","[]"));}
        catch(Exception e){return new JSONArray();}
    }
    static void schedule(Context c) {
        AlarmManager a=c.getSystemService(AlarmManager.class);
        a.cancel(alarm(c));
        long next=Long.MAX_VALUE, now=System.currentTimeMillis();
        JSONArray arr=queue(c);
        for(int j=0;j<arr.length();j++){
            JSONObject o=arr.optJSONObject(j);if(o==null)continue;
            long t=o.optLong("time");
            if(t>now && t<next) next=t;
        }
        if(next!=Long.MAX_VALUE) a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,alarm(c));
    }
    @Override public void onReceive(Context c,Intent i) {
        if(ACTION.equals(i.getAction())) {
            channel(c);
            long now=System.currentTimeMillis();
            JSONArray arr=queue(c);
            JSONObject newest=null;
            for(int j=0;j<arr.length();j++){
                JSONObject o=arr.optJSONObject(j);if(o==null)continue;
                long t=o.optLong("time");
                if(t<=now && now-t<3*60*60*1000L && (newest==null || t>newest.optLong("time")))newest=o;
            }
            if(newest!=null){
                PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                String title=newest.optString("title","План дня"), body=newest.optString("body","");
                Notification n=new Notification.Builder(c,"reports").setSmallIcon(android.R.drawable.ic_menu_today).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(open).setAutoCancel(true).build();
                c.getSystemService(NotificationManager.class).notify(24,n);
            }
        }
        schedule(c);
    }
}
