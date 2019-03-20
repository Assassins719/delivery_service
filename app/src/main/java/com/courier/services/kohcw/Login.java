package com.courier.services.kohcw;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Login extends AppCompatActivity {
    EditText et_username, et_pwd;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_username = findViewById(R.id.edt_email);
        et_pwd = findViewById(R.id.edt_password);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
//            String channelId  = getString(R.string.default_notification_channel_id);
//            String channelName = getString(R.string.default_notification_channel_name);
//            NotificationManager notificationManager =
//                    getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
//                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d("tag", "Key: " + key + " Value: " + value);
            }
        }
        // [END handle_data_extras]

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String strSID = preferences.getString("SID", "");
        String strUserID = preferences.getString("USERID", "");
        if((!strSID.equalsIgnoreCase("")) && (!strUserID.equalsIgnoreCase("")))
        {
            Intent intent = new Intent(this, Home.class);
            Bundle b = new Bundle();
            Global.strSID = strSID;
            Global.strUserID = strUserID;
            b.putString("SID", strSID); //Your id
            b.putString("USERID", strUserID); //Your id
            intent.putExtras(b);
            startActivity(intent);
        }

    }

    public void doLogin(View view){

        if(et_username.getText().toString().equals("")){
            Toast.makeText(this, "Enter Username.",
                    Toast.LENGTH_SHORT).show();
            return;
        }else if(et_pwd.getText().toString().equals("")){
            Toast.makeText(this, "Enter Password.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.setMessage("Sign In...");
        progressDialog.show();
        String soap_string = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <IsValidAppUser xmlns=\"http://tempuri.org/\">\n" +
                "      <UserId>" + et_username.getText() + "</UserId>\n" +
                "      <Password>" + et_pwd.getText() + "</Password>\n" +
                "    </IsValidAppUser>\n" +
                "  </soap12:Body>\n" +
                "</soap12:Envelope>";
        final MediaType SOAP_MEDIA_TYPE = MediaType.parse("text/xml");
        final OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(SOAP_MEDIA_TYPE, soap_string);

        final Request request = new Request.Builder()
                .url("http://contwsjobupd.eastwestcourier.com.sg/CourierOrderNTWSJobUpd.asmx")
                .post(body)
                .addHeader("Content-Type", "text/xml")
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                String mMessage = e.getMessage().toString();
                Log.w("failure Response", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();

                //code = response.code();
                try {
                    getResponse(mMessage, response);
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

    }
    public void getResponse(String response, Response mainRes) throws IOException, SAXException {

        DocumentBuilder newDocumentBuilder =
                null;
        try {
            newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        final Document parse = newDocumentBuilder.parse(new
                ByteArrayInputStream(response.getBytes()));

        String dataString = parse.getElementsByTagName
                ("Result").item(0).getTextContent();
        if(dataString.contains("False"))
        {
            if (progressDialog.isShowing()) {
                progressDialog.cancel();
            }
            Login.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Login.this, "Login Failed.",                    Toast.LENGTH_SHORT).show();//
                }});
            Log.d("re","false");
        }else{
            Intent intent = new Intent(this, Home.class);
            Bundle b = new Bundle();
            Global.strSID = dataString;
            Global.strUserID = String.valueOf(et_username.getText());
            b.putString("SID", dataString); //Your id
            b.putString("USERID", String.valueOf(et_username.getText())); //Your id
            intent.putExtras(b);
            //Save user data
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("SID",dataString);
            editor.putString("USERID", String.valueOf(et_username.getText())); //Your id
            editor.apply();

            startActivity(intent);
            if (progressDialog.isShowing()) {
                progressDialog.cancel();
            }
            this.finish();
            Log.d("re",dataString);
        }
    }
}
