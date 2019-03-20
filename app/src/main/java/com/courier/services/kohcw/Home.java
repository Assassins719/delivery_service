package com.courier.services.kohcw;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Home extends AppCompatActivity {
    String strSID = "";
    String strUSERID = "";
    String strAPI = "";
    private ProgressDialog progressDialog;
    ListView mList;
    ArrayList<JobItem> arrayJobs = new ArrayList<JobItem>();
    CustomArrayAdapter mAdapter;
    SwipeRefreshLayout pulltorefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Bundle b = getIntent().getExtras();
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        mList = findViewById(R.id.list);

        if (b != null) {
            strSID = b.getString("SID");
            strUSERID = b.getString("USERID");
            getJobs();
        }
        pulltorefresh = findViewById(R.id.pulltorefresh);
        pulltorefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getJobs();
                pulltorefresh.setRefreshing(false);
            }
        });
        sendTokenToServer();
    }

    public void readPermission() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                for (String p : info.requestedPermissions) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendTokenToServer() {
        Log.d("Refreshed token1:", " " + FirebaseInstanceId.getInstance().getToken());


        String strToken = FirebaseInstanceId.getInstance().getToken();
        strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <UpdateFCMRegToken xmlns=\"http://tempuri.org/\">\n" +
                "      <UserId>" + strUSERID + "</UserId>\n" +
                "      <DeviceRegistrationToken>" + strToken + "</DeviceRegistrationToken>\n" +
                "    </UpdateFCMRegToken>\n" +
                "  </soap12:Body>\n" +
                "</soap12:Envelope>";

        final MediaType SOAP_MEDIA_TYPE = MediaType.parse("text/xml");
        final OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(SOAP_MEDIA_TYPE, strAPI);

        final Request request = new Request.Builder()
                .url("http://contwsjobupd.eastwestcourier.com.sg/CourierOrderNTWSJobUpd.asmx")
                .post(body)
                .addHeader("Content-Type", "text/xml")
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("failure Response", mMessage);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();

                try {
                    getResponseToken(mMessage, response);
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getResponseToken(String response, Response mainRes) throws IOException, SAXException {
        String xml = response;
        XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
        JSONObject jsonObject = xmlToJson.toJson();
        try {
            JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
            JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
            JSONObject jsonResponse = (JSONObject) jsonBody.get("RetrieveJobResponse");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    public void doRefresh(View v) {
        getJobs();
    }

    public void getJobs() {
        progressDialog.setMessage("Retrieve Jobs...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <RetrieveJob xmlns=\"http://tempuri.org/\">\n" +
                "      <UserId>" + strUSERID + "</UserId>\n" +
                "      <Sid>" + strSID + "</Sid>\n" +
                "    </RetrieveJob>\n" +
                "  </soap12:Body>\n" +
                "</soap12:Envelope>";

        final MediaType SOAP_MEDIA_TYPE = MediaType.parse("text/xml");
        final OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(SOAP_MEDIA_TYPE, strAPI);

        final Request request = new Request.Builder()
                .url("http://contwsjobupd.eastwestcourier.com.sg/CourierOrderNTWSJobUpd.asmx")
                .post(body)
                .addHeader("Content-Type", "text/xml")
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("failure Response", mMessage);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();

                try {
                    getResponse(mMessage, response);
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void goSiginin(View v) {
        Intent intent = new Intent(this, Signin.class);
        startActivity(intent);
    }

    public void goRetrieve(View v) {
        Intent intent = new Intent(this, Retrieve.class);
        startActivity(intent);
    }

    public void goHome(View v) {
//        Intent intent = new Intent(this, Signin.class);
//        startActivity(intent);
//        Intent intent = new Intent(this, ScanActivity.class);
//        startActivityForResult(intent, 999);
    }

    public void doLogout(View v) {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage("Do you really want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {

                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Home.this);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("SID","");
                        editor.putString("USERID", ""); //Your id
                        editor.apply();

                        Intent intent = new Intent(Home.this, Login.class);
                        startActivity(intent);
                        Home.this.finish();
                    }
                })
                .setNegativeButton("No", null).show();


    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("resume", "resume");
        // put your code here...
        getJobs();
    }

    public void getResponse(String response, Response mainRes) throws IOException, SAXException {
        String xml = response;
        XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
        JSONObject jsonObject = xmlToJson.toJson();
        try {
            JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
            JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
            JSONObject jsonResponse = (JSONObject) jsonBody.get("RetrieveJobResponse");
            JSONObject jsonResult = (JSONObject) jsonResponse.get("RetrieveJobResult");
            JSONObject jsonCONTWS = (JSONObject) jsonResult.get("CONTWSJOBUPD");
            Object intervention = jsonCONTWS.get("RetrieveJob");
            arrayJobs.clear();
            if (intervention instanceof JSONArray) {
                JSONArray jsonJobs = (JSONArray) jsonCONTWS.get("RetrieveJob");
                Log.d("Data", String.valueOf(jsonJobs));
                for (int i = 0; i < jsonJobs.length(); i++) {
                    JobItem jobTemp = new JobItem();
                    if (jsonJobs.getJSONObject(i).has("OrderNo")) {
                        jobTemp.OrderNo = jsonJobs.getJSONObject(i).getString("OrderNo");
                    } else {
                        jobTemp.OrderNo = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("Status")) {
                        jobTemp.Status = jsonJobs.getJSONObject(i).getString("Status");
                    } else {
                        jobTemp.Status = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("OrderNo")) {
                        jobTemp.JobDorC = jsonJobs.getJSONObject(i).getString("JobDorC");
                    } else {
                        jobTemp.JobDorC = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("JobTypeDesp")) {
                        jobTemp.JobTypeDesp = jsonJobs.getJSONObject(i).getString("JobTypeDesp");
                    } else {
                        jobTemp.JobTypeDesp = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("FromName")) {
                        jobTemp.FromName = jsonJobs.getJSONObject(i).getString("FromName");
                    } else {
                        jobTemp.FromName = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("FromCTCPerson")) {
                        jobTemp.FromCTCPerson = jsonJobs.getJSONObject(i).getString("FromCTCPerson");
                    } else {
                        jobTemp.FromCTCPerson = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("FromAddress")) {
                        jobTemp.FromAddress = jsonJobs.getJSONObject(i).getString("FromAddress");
                    } else {
                        jobTemp.FromAddress = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("FromTel")) {
                        jobTemp.FromTel = jsonJobs.getJSONObject(i).getString("FromTel");
                    } else {
                        jobTemp.FromTel = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("FromHPNo")) {
                        jobTemp.FromHPNo = jsonJobs.getJSONObject(i).getString("FromHPNo");
                    } else {
                        jobTemp.FromHPNo = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("ToName")) {
                        jobTemp.ToName = jsonJobs.getJSONObject(i).getString("ToName");
                    } else {
                        jobTemp.ToName = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("ToCTCPerson")) {
                        jobTemp.ToCTCPerson = jsonJobs.getJSONObject(i).getString("ToCTCPerson");
                    } else {
                        jobTemp.ToCTCPerson = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("ToAddress")) {
                        jobTemp.ToAddress = jsonJobs.getJSONObject(i).getString("ToAddress");
                    } else {
                        jobTemp.ToAddress = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("ToTel")) {
                        jobTemp.ToTel = jsonJobs.getJSONObject(i).getString("ToTel");
                    } else {
                        jobTemp.ToTel = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("ToHPNo")) {
                        jobTemp.ToHPNo = jsonJobs.getJSONObject(i).getString("ToHPNo");
                    } else {
                        jobTemp.ToHPNo = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("JobDate")) {
                        jobTemp.JobDate = jsonJobs.getJSONObject(i).getString("JobDate");
                    } else {
                        jobTemp.JobDate = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("UpdateInfo")) {
                        jobTemp.UpdateInfo = jsonJobs.getJSONObject(i).getString("UpdateInfo");
                    } else {
                        jobTemp.UpdateInfo = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("SpecialRemark")) {
                        jobTemp.SpecialRemark = jsonJobs.getJSONObject(i).getString("SpecialRemark");
                    } else {
                        jobTemp.SpecialRemark = "";
                    }
                    if (jsonJobs.getJSONObject(i).has("CustRefs")) {
                        jobTemp.CustRefs = jsonJobs.getJSONObject(i).getString("CustRefs");
                    } else {
                        jobTemp.CustRefs = "";
                    }

                    jobTemp.isExpand = false;
                    if (jobTemp.Status != "Completed") {
                        arrayJobs.add(jobTemp);
                    }
                }
            } else {
                JSONObject jsonJobs = (JSONObject) jsonCONTWS.get("RetrieveJob");
                JobItem jobTemp = new JobItem();
                if (jsonJobs.has("OrderNo")) {
                    jobTemp.OrderNo = jsonJobs.getString("OrderNo");
                } else {
                    jobTemp.OrderNo = "";
                }
                if (jsonJobs.has("Status")) {
                    jobTemp.Status = jsonJobs.getString("Status");
                } else {
                    jobTemp.Status = "";
                }
                if (jsonJobs.has("OrderNo")) {
                    jobTemp.JobDorC = jsonJobs.getString("JobDorC");
                } else {
                    jobTemp.JobDorC = "";
                }
                if (jsonJobs.has("JobTypeDesp")) {
                    jobTemp.JobTypeDesp = jsonJobs.getString("JobTypeDesp");
                } else {
                    jobTemp.JobTypeDesp = "";
                }
                if (jsonJobs.has("FromName")) {
                    jobTemp.FromName = jsonJobs.getString("FromName");
                } else {
                    jobTemp.FromName = "";
                }
                if (jsonJobs.has("FromCTCPerson")) {
                    jobTemp.FromCTCPerson = jsonJobs.getString("FromCTCPerson");
                } else {
                    jobTemp.FromCTCPerson = "";
                }
                if (jsonJobs.has("FromAddress")) {
                    jobTemp.FromAddress = jsonJobs.getString("FromAddress");
                } else {
                    jobTemp.FromAddress = "";
                }
                if (jsonJobs.has("FromTel")) {
                    jobTemp.FromTel = jsonJobs.getString("FromTel");
                } else {
                    jobTemp.FromTel = "";
                }
                if (jsonJobs.has("FromHPNo")) {
                    jobTemp.FromHPNo = jsonJobs.getString("FromHPNo");
                } else {
                    jobTemp.FromHPNo = "";
                }
                if (jsonJobs.has("ToName")) {
                    jobTemp.ToName = jsonJobs.getString("ToName");
                } else {
                    jobTemp.ToName = "";
                }
                if (jsonJobs.has("ToCTCPerson")) {
                    jobTemp.ToCTCPerson = jsonJobs.getString("ToCTCPerson");
                } else {
                    jobTemp.ToCTCPerson = "";
                }
                if (jsonJobs.has("ToAddress")) {
                    jobTemp.ToAddress = jsonJobs.getString("ToAddress");
                } else {
                    jobTemp.ToAddress = "";
                }
                if (jsonJobs.has("ToTel")) {
                    jobTemp.ToTel = jsonJobs.getString("ToTel");
                } else {
                    jobTemp.ToTel = "";
                }
                if (jsonJobs.has("ToHPNo")) {
                    jobTemp.ToHPNo = jsonJobs.getString("ToHPNo");
                } else {
                    jobTemp.ToHPNo = "";
                }

                if (jsonJobs.has("JobDate")) {
                    jobTemp.JobDate = jsonJobs.getString("JobDate");
                } else {
                    jobTemp.JobDate = "";
                }
                if (jsonJobs.has("UpdateInfo")) {
                    jobTemp.UpdateInfo = jsonJobs.getString("UpdateInfo");
                } else {
                    jobTemp.UpdateInfo = "";
                }
                if (jsonJobs.has("SpecialRemark")) {
                    jobTemp.SpecialRemark = jsonJobs.getString("SpecialRemark");
                } else {
                    jobTemp.SpecialRemark = "";
                }
                if (jsonJobs.has("CustRefs")) {
                    jobTemp.CustRefs = jsonJobs.getString("CustRefs");
                } else {
                    jobTemp.CustRefs = "";
                }

                jobTemp.isExpand = false;
                if (jobTemp.Status != "Completed") {
                    arrayJobs.add(jobTemp);
                }
            }

            Home.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter = new CustomArrayAdapter(Home.this, arrayJobs);
                    mList.setAdapter(mAdapter);

                    mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Log.d("Click", String.valueOf(position));
                            int index = -1;
                            for (int i = 0; i < arrayJobs.size(); i++) {
                                if (arrayJobs.get(i).isExpand) {
                                    index = i;
                                } else {
                                    arrayJobs.get(i).isExpand = false;
                                }
                            }
                            if (index == position) {
                                arrayJobs.get(position).isExpand = !arrayJobs.get(position).isExpand;
                            } else {
                                if (index != -1)
                                    arrayJobs.get(index).isExpand = false;
                                arrayJobs.get(position).isExpand = true;
                            }

                            mAdapter.setData(arrayJobs, strSID, strUSERID);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });

        } catch (JSONException e) {
            arrayJobs.clear();
            Home.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter = new CustomArrayAdapter(Home.this, arrayJobs);
                    mList.setAdapter(mAdapter);
                }
            });
            e.printStackTrace();
        }
        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 999) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("result");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Scan Result");
                builder.setMessage(result);
                AlertDialog alert1 = builder.create();
                alert1.show();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }
}
