package com.courier.services.kohcw;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Signin extends AppCompatActivity {
    public EditText et_order;
    public ListView mList;
    public ArrayList<String> mOrdernos = new ArrayList<>();
    public ListArrayAdapter mAdapter;
    private ProgressDialog progressDialog;
    public TextView tx_title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        mList = findViewById(R.id.list);
        et_order = findViewById(R.id.et_orderno);
        tx_title = findViewById(R.id.tx_title);
        mAdapter = new ListArrayAdapter(Signin.this, mOrdernos);
        mList.setAdapter(mAdapter);
//        et_order.setOnEditorActionListener(
//                new EditText.OnEditorActionListener() {
//                    @Override
//                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                        if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null &&
//                                event.getAction() == KeyEvent.ACTION_DOWN &&
//                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
//                            if (event == null || !event.isShiftPressed()) {
//                                // the user is done typing.
//                                mOrdernos.add(String.valueOf(et_order.getText()));
//                                mAdapter.addData(String.valueOf(et_order.getText()));
//                                mAdapter.notifyDataSetChanged();
//                                return true; // consume.
//                            }
//                        }
//                        return false; // pass on to other listeners.                     }
//                    }
//                }
//        );
    }

    public void goHome(View v) {
        this.finish();
    }

    public void doLogout(View v) {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage("Do you really want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(Signin.this, Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        Signin.this.finish();
                    }
                })
                .setNegativeButton("No", null).show();
    }

    public void doScan(View v) {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivityForResult(intent, 999);
    }

    public void doSubmit(View v) {
        if (mAdapter.getData().size() == 0) {
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Signing Jobs...");
        progressDialog.show();

        this.mOrdernos.clear();
        mOrdernos.addAll(mAdapter.getData());
        String strJoblist = "<OrderNoListSignedIn>\n";

        for (int i = 0; i < this.mOrdernos.size(); i++) {
            strJoblist += "<OrderNoList>\n<OrderNo>" + this.mOrdernos.get(i) + "</OrderNo>\n</OrderNoList>\n";
        }
        strJoblist += "</OrderNoListSignedIn>\n";
        String strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <SignInJob xmlns=\"http://tempuri.org/\">\n" +
                "      <UserId>" + Global.strUserID + "</UserId>\n" +
                "      <Sid>" + Global.strSID + "</Sid>\n" + strJoblist +
                "    </SignInJob>\n" +
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

    ArrayList<String> mRejected = new ArrayList<>();

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

        String xml = response;
        XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
        JSONObject jsonObject = xmlToJson.toJson();
        try {

            JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
            JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
            JSONObject jsonResponse = (JSONObject) jsonBody.get("SignInJobResponse");
            JSONObject jsonResult = (JSONObject) jsonResponse.get("SignInJobResult");
            JSONObject jsonCONTWS = (JSONObject) jsonResult.get("CONTWSJOBUPD");
            Object intervention = jsonCONTWS.get("SignInJob");
            if (intervention instanceof JSONArray) {
                JSONArray jsonJobs = (JSONArray) jsonCONTWS.get("SignInJob");
                Log.d("Data", String.valueOf(jsonJobs));
                mRejected.clear();
                for (int i = 0; i < jsonJobs.length(); i++) {
                    String strStatus = jsonJobs.getJSONObject(i).getString("Staus");
                    String strRejcted = "";
                    strRejcted = jsonJobs.getJSONObject(i).getString("OrderNo");
                    if(strStatus.equals("Rejected")) {
                        mRejected.add(strRejcted);
                    }
                }
                Signin.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(Signin.this)
                                .setMessage("Signed In Order : " + (mOrdernos.size() - mRejected.size()) + '\n' + "Rejected Order : " + mRejected.size())
                                .setCancelable(false)
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                        if(mRejected.size() > 0)
                            tx_title.setText("Rejected Order No");
                        mOrdernos.clear();
                        mOrdernos.addAll(mRejected);
                        mAdapter.setData(mOrdernos);
                        mAdapter.notifyDataSetChanged();
                    }
                });
            } else {
                JSONObject jsonJobs = (JSONObject) jsonCONTWS.get("SignInJob");
                mRejected.clear();
                String strStatus = jsonJobs.getString("Staus");
                String strRejcted = "";
                strRejcted = jsonJobs.getString("OrderNo");
                if(strStatus.equals("Rejected")) {
                    mRejected.add(strRejcted);
                }
                Signin.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(Signin.this)
                                .setMessage("Signed In Order : " + (mOrdernos.size() - mRejected.size()) + '\n' + "Rejected Order : " + mRejected.size())
                                .setCancelable(false)
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                        if(mRejected.size() > 0)
                            tx_title.setText("Rejected Order No");
                        mOrdernos.clear();
                        mOrdernos.addAll(mRejected);
                        mAdapter.setData(mOrdernos);
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    public void doAdd(View v) {
        if(et_order.getText().toString().equals(""))
            return;
        if(mOrdernos.contains(et_order.getText().toString()))
            return;
        mOrdernos.add(String.valueOf(et_order.getText()));
        mAdapter.addData(String.valueOf(et_order.getText()));
        et_order.setText("");
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 999) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("result");
                et_order.setText(result);
//                mOrdernos.add(result);
//                mAdapter.addData(result);
//                mAdapter.notifyDataSetChanged();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }
}
