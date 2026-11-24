package com.lifeapp.organizer;

import android.app.*;
import android.os.*;
import android.webkit.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.content.pm.PackageManager;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> picker;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(16,21,29));
        getWindow().setNavigationBarColor(Color.rgb(16,21,29));
        ReportReceiver.channel(this);
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(16,21,29));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccessFromFileURLs(false);
        web.getSettings().setAllowUniversalAccessFromFileURLs(false);
        web.addJavascriptInterface(new Bridge(), "LifeAndroid");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {return true;}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams params){
                if(picker!=null)picker.onReceiveValue(null);
                picker=cb;
                try {startActivityForResult(params.createIntent(),44);return true;}
                catch(ActivityNotFoundException ex){picker.onReceiveValue(null);picker=null;return false;}
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},45);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==44 && picker!=null){picker.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));picker=null;}
        if(request==46 && result==RESULT_OK && data!=null && data.getData()!=null && backup!=null){
            try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out!=null)out.write(backup.getBytes(java.nio.charset.StandardCharsets.UTF_8)); android.widget.Toast.makeText(this,"Копия сохранена",android.widget.Toast.LENGTH_SHORT).show();}
            catch(Exception ex){android.widget.Toast.makeText(this,"Не удалось сохранить копию",android.widget.Toast.LENGTH_LONG).show();}
            backup=null;
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if(web != null) web.evaluateJavascript("if(window.refreshLife) window.refreshLife()",null);
    }
    private String backup;
    public class Bridge {
        @JavascriptInterface public void reports(String json) {
            getSharedPreferences("reports",0).edit().putString("queue",json).apply();
            runOnUiThread(() -> ReportReceiver.schedule(MainActivity.this));
        }
        @JavascriptInterface public void share(String json) {
            runOnUiThread(() -> {
                backup=json;
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"life-app.json");
                startActivityForResult(i,46);
            });
        }
    }
}
