package com.courier.services.kohcw;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Retrieve extends AppCompatActivity {
    EditText et_order;
    int TAKE_PHOTO_CODE = 0;
    int SELECT_PHOTO_CODE = 1;
    ImageView img_photo, img_sign;
    JobItem orderDetail;
    ProgressDialog progressDialog;
    ArrayList<String> arrayReasons = new ArrayList<>();
    ArrayList<String> arraySms = new ArrayList<>();
    int nReasonIndex = -1;
    public TextView tx_no, tx_type, tx_from, tx_fromctc, tx_fromname, tx_fromtel, tx_to, tx_toctc, tx_toname, tx_totel, tx_date, tx_updateinfo, tx_remark, tx_custrefs;

    public LinearLayout lyt, lyt_updateinfo, lyt_remark, lyt_custrefs;
    Button btn_photo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retrieve);
        btn_photo = findViewById(R.id.btn_photo);
        btn_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPhoto();
            }
        });
        img_photo = findViewById(R.id.img_photo);
        img_sign = findViewById(R.id.img_sign);

        et_order = findViewById(R.id.et_orderno);
        tx_no = findViewById(R.id.tx_no);
        tx_type = findViewById(R.id.tx_type);
        tx_from = findViewById(R.id.tx_from);
        tx_fromctc = findViewById(R.id.tx_fromctc);
        tx_fromname = findViewById(R.id.tx_fromname);
        tx_fromtel = findViewById(R.id.tx_fromtel);
        tx_to = findViewById(R.id.tx_to);
        tx_toctc = findViewById(R.id.tx_toctc);
        tx_toname = findViewById(R.id.tx_toname);
        tx_totel = findViewById(R.id.tx_totel);
        tx_date = findViewById(R.id.tx_datefield);

        lyt = findViewById(R.id.lyt);
        lyt.setVisibility(View.GONE);

        lyt_updateinfo = findViewById(R.id.lyt_updateinfo);
        lyt_remark = findViewById(R.id.lyt_remark);
        lyt_custrefs = findViewById(R.id.lyt_custrefs);

        tx_updateinfo = findViewById(R.id.tx_updateinfo);
        tx_remark = findViewById(R.id.tx_remark);
        tx_custrefs = findViewById(R.id.tx_custrefs);

        if (Global.retrieveJob != null) {
            lyt.setVisibility(View.VISIBLE);
            orderDetail = Global.retrieveJob;
            Global.retrieveJob = null;
            et_order.setText(orderDetail.OrderNo);
            fillDetail();
        }
        getResons();
        getSmss();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        _checkPermission();
    }
    private void _checkPermission() {
        ArrayList<String> arrStr = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "WRITE_EXTERNAL_STORAGE Permission Request");
            arrStr.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "READ_EXTERNAL_STORAGE Permission Request");
            arrStr.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "WRITE_EXTERNAL_STORAGE Permission Request");
            arrStr.add(android.Manifest.permission.CAMERA);
        }

        int len = arrStr.size();
        if (len > 0) {
            String[] arrString = new String[len];
            for (int i = 0; i < len; i++) {
                arrString[i] = arrStr.get(i);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrString, 45);
            }
        }
    }
    private String pictureImagePath = "";

    public void doPhoto() {
        String imageFileName = "Sample.jpg";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;
        File file = new File(pictureImagePath);
        Uri outputFileUri = Uri.fromFile(file);
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
    }

    public void doGallery(View v) {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, SELECT_PHOTO_CODE);
    }

    public void doSign(View v) {
        Intent sign = new Intent(Retrieve.this, DrawSign.class);
        startActivityForResult(sign, 888);
    }

    //
    public void doCall(View v) {
        if (orderDetail != null) {
            if (orderDetail.JobDorC.equals("Collection")) {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                final ArrayList<String> strNumbers = new ArrayList<>();
                strNumbers.add(orderDetail.FromTel);
                strNumbers.add(orderDetail.FromHPNo);
                String items[] = strNumbers.toArray(new String[0]);//new String[arrayReasons.size()];
                adb.setSingleChoiceItems(items, nReasonIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int n) {
                        Log.d("reason", "" + n);
                        d.dismiss();
                        String phone = strNumbers.get(n);
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                        startActivity(intent);
                    }
                });
                adb.setNegativeButton("Cancel", null);
                adb.setTitle("Select Phone Number.");
                adb.show();
            } else {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                final ArrayList<String> strNumbers = new ArrayList<>();
                strNumbers.add(orderDetail.ToTel);
                strNumbers.add(orderDetail.ToHPNo);
                String items[] = strNumbers.toArray(new String[0]);//new String[arrayReasons.size()];
                adb.setSingleChoiceItems(items, nReasonIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int n) {
                        Log.d("reason", "" + n);
                        d.dismiss();
                        String phone = strNumbers.get(n);
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                        startActivity(intent);
                    }
                });
                adb.setNegativeButton("Cancel", null);
                adb.setTitle("Select Phone Number.");
                adb.show();
            }
        }
    }

    public void doSMS(View v) {
        if (orderDetail != null) {
            if (orderDetail.JobDorC.equals("Collection")) {
                try {
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    String items[] = arraySms.toArray(new String[0]);//new String[arrayReasons.size()];
                    adb.setSingleChoiceItems(items, nReasonIndex, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int n) {
                            Log.d("reason", "" + n);
                            d.dismiss();
                            String strSms = "";
                            if (n != 0) {
                                strSms = arraySms.get(n);
                            }
                            String uri = "smsto:" + orderDetail.FromHPNo;
                            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
                            intent.putExtra("sms_body", strSms);
                            intent.putExtra("compose_mode", true);
                            startActivity(intent);
                        }
                    });
                    adb.setNegativeButton("Cancel", null);
                    adb.setTitle("Select Message.");
                    adb.show();


                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), ex.getMessage().toString(),
                            Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                }
            } else {
                try {
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    String items[] = arraySms.toArray(new String[0]);//new String[arrayReasons.size()];
                    adb.setSingleChoiceItems(items, nReasonIndex, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int n) {
                            Log.d("reason", "" + n);
                            d.dismiss();
                            String strSms = "";
                            if (n != 0) {
                                strSms = arraySms.get(n);
                            }
                            String uri = "smsto:" + orderDetail.ToHPNo;
                            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
                            intent.putExtra("sms_body", strSms);
                            intent.putExtra("compose_mode", true);
                            startActivity(intent);
                        }
                    });
                    adb.setNegativeButton("Cancel", null);
                    adb.setTitle("Select Message.");
                    adb.show();
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), ex.getMessage().toString(),
                            Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                }
            }
        }
    }

    public void doIncomplete(View v) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        String items[] = arrayReasons.toArray(new String[0]);//new String[arrayReasons.size()];
        adb.setSingleChoiceItems(items, nReasonIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int n) {
                Log.d("reason", "" + n);
                nReasonIndex = n;
                d.dismiss();
                inCompleteJob();
            }
        });
        adb.setNegativeButton("Cancel", null);
        adb.setTitle("Select Reason.");
        adb.show();
    }

    public void inCompleteJob() {
        if (orderDetail == null) {
            Toast.makeText(this, "Get Order Details Correctly.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
        String strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <SubmitInCompletedJob xmlns=\"http://tempuri.org/\">\n" +
                "       <UserId>" + Global.strUserID + "</UserId>\n" +
                "       <Sid>" + Global.strSID + "</Sid>\n" +
                "       <OrderNo>" + orderDetail.OrderNo + "</OrderNo>\n" +
                "       <Reason>" + arrayReasons.get(nReasonIndex) + "</Reason>\n" +
                "       <ProofOfDeliveryImage></ProofOfDeliveryImage>\n" +
                "       <SignatureImage></SignatureImage>\n" +
                "    </SubmitInCompletedJob>\n" +
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
                img_photo.setImageResource(R.color.zxing_transparent);
                img_sign.setImageResource(R.color.zxing_transparent);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();

                try {
                    getInCompleteResponse(mMessage, response);
                } catch (SAXException e) {
                    e.printStackTrace();
                    img_photo.setImageResource(R.color.zxing_transparent);
                    img_sign.setImageResource(R.color.zxing_transparent);
                }
            }
        });
    }

    public void getInCompleteResponse(String response, Response mainRes) throws IOException, SAXException {
        String xml = response;
        XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
        JSONObject jsonObject = xmlToJson.toJson();
        try {
            JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
            JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
            JSONObject jsonResponse = (JSONObject) jsonBody.get("SubmitInCompletedJobResponse");
            JSONObject jsonResult = (JSONObject) jsonResponse.get("SubmitInCompletedJobResult");
            JSONObject jsonCONTWS = (JSONObject) jsonResult.get("CONTWSJOBUPD");
            JSONObject jsonJob = (JSONObject) jsonCONTWS.get("SubmitInCompletedJob");
            final String strResult = (String) jsonJob.get("Result");
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    img_photo.setImageResource(R.color.zxing_transparent);
                    img_sign.setImageResource(R.color.zxing_transparent);
                    Toast.makeText(Retrieve.this, strResult,
                            Toast.LENGTH_LONG).show();
                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
//            img_photo.setImageResource(R.color.zxing_transparent);
//            img_sign.setImageResource(R.color.zxing_transparent);
        }
        Retrieve.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }
            }
        });
    }

    public void doComplete(View v) {

        if (orderDetail == null) {
            Toast.makeText(this, "Get Order Details Correctly.", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
        String strSignencoded = "";
        String strPhotoencoded = "";
        if (bmp_sign != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bmp_sign.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            strSignencoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        }
        if (bmp_photo != null) {
            final float densityMultiplier = this.getResources().getDisplayMetrics().density;

            int h = (int) (300 * densityMultiplier);
            int w = (int) (h * bmp_photo.getWidth() / ((double) bmp_photo.getHeight()));

            bmp_photo = Bitmap.createScaledBitmap(bmp_photo, w, h, true);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bmp_photo.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            strPhotoencoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        }

        String strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <SubmitCompletedJob xmlns=\"http://tempuri.org/\">\n" +
                "      <UserId>" + Global.strUserID + "</UserId>\n" +
                "      <Sid>" + Global.strSID + "</Sid>\n" +
                "      <OrderNo>" + orderDetail.OrderNo + "</OrderNo>\n" +
                "      <ProofOfDeliveryImage>" + strPhotoencoded + "</ProofOfDeliveryImage>\n" +
                "      <SignatureImage>" + strSignencoded + "</SignatureImage>\n" +
                "    </SubmitCompletedJob>\n" +
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
                img_photo.setImageResource(R.color.zxing_transparent);
                img_sign.setImageResource(R.color.zxing_transparent);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();

                try {
                    getCompleteResponse(mMessage, response);
                } catch (SAXException e) {
                    e.printStackTrace();
                    img_photo.setImageResource(R.color.zxing_transparent);
                    img_sign.setImageResource(R.color.zxing_transparent);
                }
            }
        });
    }

    public void getCompleteResponse(String response, Response mainRes) throws IOException, SAXException {
        String xml = response;
        XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
        JSONObject jsonObject = xmlToJson.toJson();
        try {
            JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
            JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
            JSONObject jsonResponse = (JSONObject) jsonBody.get("SubmitCompletedJobResponse");
            JSONObject jsonResult = (JSONObject) jsonResponse.get("SubmitCompletedJobResult");
            JSONObject jsonCONTWS = (JSONObject) jsonResult.get("CONTWSJOBUPD");
            JSONObject jsonJob = (JSONObject) jsonCONTWS.get("SubmitCompletedJob");
            final String strResult = (String) jsonJob.get("Result");


            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    img_photo.setImageResource(R.color.zxing_transparent);
                    img_sign.setImageResource(R.color.zxing_transparent);
                    Toast.makeText(Retrieve.this, strResult,
                            Toast.LENGTH_LONG).show();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            img_photo.setImageResource(R.color.zxing_transparent);
            img_sign.setImageResource(R.color.zxing_transparent);
        }
        Retrieve.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }
            }
        });
    }

    public void expandDetail(View v) {
        if (orderDetail != null) {
            orderDetail.isExpand = !orderDetail.isExpand;
            fillDetail();
        }
    }

    public void getSmss() {
        String strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <GetSMSStdMsg xmlns=\"http://tempuri.org/\">\n" +
                "      <UserId>" + Global.strUserID + "</UserId>\n" +
                "      <Sid>" + Global.strSID + "</Sid>\n" +
                "    </GetSMSStdMsg>\n" +
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
                    getResponseSms(mMessage, response);
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getResponseSms(String response, Response mainRes) throws IOException, SAXException {
        String xml = response;
        XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
        JSONObject jsonObject = xmlToJson.toJson();
        try {
            JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
            JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
            JSONObject jsonResponse = (JSONObject) jsonBody.get("GetSMSStdMsgResponse");
            JSONObject jsonResult = (JSONObject) jsonResponse.get("GetSMSStdMsgResult");
            JSONObject jsonCONTWS = (JSONObject) jsonResult.get("CONTWSJOBUPD");
            JSONArray jsonReasons = (JSONArray) jsonCONTWS.get("GetSMSStdMsg");
            Log.d("Data", String.valueOf(jsonReasons));
            arraySms.clear();
            arraySms.add("New Blank Message");
            for (int i = 0; i < jsonReasons.length(); i++) {
                String strReason = jsonReasons.getJSONObject(i).getString("Message");
                arraySms.add(strReason);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Retrieve.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }
            }
        });
    }

    public void getResons() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
        String strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <GetInCompletedJobReason xmlns=\"http://tempuri.org/\">\n" +
                "      <UserId>" + Global.strUserID + "</UserId>\n" +
                "      <Sid>" + Global.strSID + "</Sid>\n" +
                "    </GetInCompletedJobReason>\n" +
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
                    getResponseReason(mMessage, response);
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getDetail(View v) {
        if (et_order.getText().equals("")) {
            Toast.makeText(Retrieve.this, "Please Enter OrderNo",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Getting Order Detail...");
        progressDialog.show();

        img_photo.setImageResource(R.color.zxing_transparent);
        img_sign.setImageResource(R.color.zxing_transparent);

        String strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <soap12:Body>\n" +
                "    <GetOrderDetail xmlns=\"http://tempuri.org/\">\n" +
                "      <UserId>" + Global.strUserID + "</UserId>\n" +
                "      <Sid>" + Global.strSID + "</Sid>\n" +
                "      <OrderNo>" + et_order.getText() + "</OrderNo>\n" +
                "    </GetOrderDetail>\n" +
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

    public void getResponse(String response, Response mainRes) throws IOException, SAXException {
        String xml = response;
        XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
        JSONObject jsonObject = xmlToJson.toJson();
        try {
            JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
            JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
            JSONObject jsonResponse = (JSONObject) jsonBody.get("GetOrderDetailResponse");
            JSONObject jsonResult = (JSONObject) jsonResponse.get("GetOrderDetailResult");
            JSONObject jsonCONTWS = (JSONObject) jsonResult.get("CONTWSJOBUPD");
            JSONObject jsonJobs = (JSONObject) jsonCONTWS.get("RetrieveOrder");
            Log.d("Data", String.valueOf(jsonJobs));
            if (jsonJobs.has("Result")) {
                final String strResult = jsonJobs.getString("Result");
                Retrieve.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Retrieve.this, strResult, Toast.LENGTH_LONG).show();
                        orderDetail = null;
                        fillDetail();
                        lyt.setVisibility(View.GONE);
                    }
                });
            } else {
                orderDetail = new JobItem();
                if (jsonJobs.has("OrderNo")) {
                    orderDetail.OrderNo = jsonJobs.getString("OrderNo");
                } else {
                    orderDetail.OrderNo = "";
                }
                if (jsonJobs.has("Status")) {
                    orderDetail.Status = jsonJobs.getString("Status");
                } else {
                    orderDetail.Status = "";
                }
                if (jsonJobs.has("OrderNo")) {
                    orderDetail.JobDorC = jsonJobs.getString("JobDorC");
                } else {
                    orderDetail.JobDorC = "";
                }
                if (jsonJobs.has("JobTypeDesp")) {
                    orderDetail.JobTypeDesp = jsonJobs.getString("JobTypeDesp");
                } else {
                    orderDetail.JobTypeDesp = "";
                }
                if (jsonJobs.has("FromName")) {
                    orderDetail.FromName = jsonJobs.getString("FromName");
                } else {
                    orderDetail.FromName = "";
                }
                if (jsonJobs.has("FromCTCPerson")) {
                    orderDetail.FromCTCPerson = jsonJobs.getString("FromCTCPerson");
                } else {
                    orderDetail.FromCTCPerson = "";
                }
                if (jsonJobs.has("FromAddress")) {
                    orderDetail.FromAddress = jsonJobs.getString("FromAddress");
                } else {
                    orderDetail.FromAddress = "";
                }
                if (jsonJobs.has("FromTel")) {
                    orderDetail.FromTel = jsonJobs.getString("FromTel");
                } else {
                    orderDetail.FromTel = "";
                }
                if (jsonJobs.has("FromHPNo")) {
                    orderDetail.FromHPNo = jsonJobs.getString("FromHPNo");
                } else {
                    orderDetail.FromHPNo = "";
                }
                if (jsonJobs.has("ToName")) {
                    orderDetail.ToName = jsonJobs.getString("ToName");
                } else {
                    orderDetail.ToName = "";
                }
                if (jsonJobs.has("ToCTCPerson")) {
                    orderDetail.ToCTCPerson = jsonJobs.getString("ToCTCPerson");
                } else {
                    orderDetail.ToCTCPerson = "";
                }
                if (jsonJobs.has("ToAddress")) {
                    orderDetail.ToAddress = jsonJobs.getString("ToAddress");
                } else {
                    orderDetail.ToAddress = "";
                }
                if (jsonJobs.has("ToTel")) {
                    orderDetail.ToTel = jsonJobs.getString("ToTel");
                } else {
                    orderDetail.ToTel = "";
                }
                if (jsonJobs.has("ToHPNo")) {
                    orderDetail.ToHPNo = jsonJobs.getString("ToHPNo");
                } else {
                    orderDetail.ToHPNo = "";
                }
                if (jsonJobs.has("JobDate")) {
                    orderDetail.JobDate = jsonJobs.getString("JobDate");
                } else {
                    orderDetail.JobDate = "";
                }
                if (jsonJobs.has("UpdateInfo")) {
                    orderDetail.UpdateInfo = jsonJobs.getString("UpdateInfo");
                } else {
                    orderDetail.UpdateInfo = "";
                }
                if (jsonJobs.has("SpecialRemark")) {
                    orderDetail.SpecialRemark = jsonJobs.getString("SpecialRemark");
                } else {
                    orderDetail.SpecialRemark = "";
                }
                if (jsonJobs.has("CustRefs")) {
                    orderDetail.CustRefs = jsonJobs.getString("CustRefs");
                } else {
                    orderDetail.CustRefs = "";
                }

                this.orderDetail.isExpand = false;
                Retrieve.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fillDetail();
                        lyt.setVisibility(View.VISIBLE);
                    }
                });

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Retrieve.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }
            }
        });
    }

    public void getResponseReason(String response, Response mainRes) throws IOException, SAXException {
        String xml = response;
        XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
        JSONObject jsonObject = xmlToJson.toJson();
        try {
            JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
            JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
            JSONObject jsonResponse = (JSONObject) jsonBody.get("GetInCompletedJobReasonResponse");
            JSONObject jsonResult = (JSONObject) jsonResponse.get("GetInCompletedJobReasonResult");
            JSONObject jsonCONTWS = (JSONObject) jsonResult.get("CONTWSJOBUPD");
            JSONArray jsonReasons = (JSONArray) jsonCONTWS.get("GetInCompletedJobReason");
            Log.d("Data", String.valueOf(jsonReasons));
            arrayReasons.clear();
            for (int i = 0; i < jsonReasons.length(); i++) {
                String strReason = jsonReasons.getJSONObject(i).getString("Reason");
                arrayReasons.add(strReason);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Retrieve.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }
            }
        });
    }

    public void fillDetail() {
        if (orderDetail != null) {
            tx_no.setText(orderDetail.OrderNo);
            tx_type.setText(orderDetail.JobDorC + " | " + orderDetail.JobTypeDesp);
            tx_fromctc.setText(orderDetail.FromCTCPerson);
            tx_fromname.setText(orderDetail.FromName + " | " + orderDetail.FromAddress);
            tx_fromtel.setText(orderDetail.FromTel + " | " + orderDetail.FromHPNo);
            tx_toctc.setText(orderDetail.ToCTCPerson);
            tx_toname.setText(orderDetail.ToName + " | " + orderDetail.ToAddress);
            tx_totel.setText(orderDetail.ToTel + " | " + orderDetail.ToHPNo);
            tx_date.setText(orderDetail.JobDate);
            tx_updateinfo.setText(orderDetail.UpdateInfo);
            tx_remark.setText(orderDetail.SpecialRemark);
            tx_custrefs.setText(orderDetail.CustRefs);

            if (!orderDetail.isExpand) {
                tx_from.setVisibility(View.GONE);
                tx_fromctc.setVisibility(View.GONE);
                tx_fromtel.setVisibility(View.GONE);
                tx_toctc.setVisibility(View.GONE);
                tx_totel.setVisibility(View.GONE);
                lyt_custrefs.setVisibility(View.GONE);
                lyt.setBackgroundColor(Color.parseColor("#EDF3DB"));

                tx_no.setMaxLines(1);
                tx_type.setMaxLines(1);
                tx_fromctc.setMaxLines(1);
                tx_fromname.setMaxLines(1);
                tx_fromtel.setMaxLines(1);
                tx_toctc.setMaxLines(1);
                tx_toname.setMaxLines(1);
                tx_totel.setMaxLines(1);
            } else {
                tx_from.setVisibility(View.VISIBLE);
                tx_fromctc.setVisibility(View.VISIBLE);
                tx_fromtel.setVisibility(View.VISIBLE);
                tx_toctc.setVisibility(View.VISIBLE);
                tx_totel.setVisibility(View.VISIBLE);
                lyt_custrefs.setVisibility(View.VISIBLE);
                lyt.setBackgroundColor(Color.parseColor("#b2bfff"));

                tx_no.setMaxLines(3);
                tx_type.setMaxLines(3);
                tx_fromctc.setMaxLines(3);
                tx_fromname.setMaxLines(3);
                tx_fromtel.setMaxLines(3);
                tx_toctc.setMaxLines(3);
                tx_toname.setMaxLines(3);
                tx_totel.setMaxLines(3);
            }
        } else {
            tx_no.setText("");
            tx_type.setText("");
            tx_fromctc.setText("");
            tx_fromname.setText("");
            tx_fromtel.setText("");
            tx_toctc.setText("");
            tx_toname.setText("");
            tx_totel.setText("");
        }
        if (orderDetail.UpdateInfo.equals("")) {
            lyt_updateinfo.setVisibility(View.GONE);
        }
        if (orderDetail.SpecialRemark.equals("")) {
            lyt_remark.setVisibility(View.GONE);
        }
        if (orderDetail.CustRefs.equals("")) {
            lyt_custrefs.setVisibility(View.GONE);
        }
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
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Retrieve.this);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("SID", "");
                        editor.putString("USERID", ""); //Your id
                        editor.apply();

                        Intent intent = new Intent(Retrieve.this, Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        Retrieve.this.finish();
                    }
                })
                .setNegativeButton("No", null).show();
    }

    public void doScan(View v) {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivityForResult(intent, 999);
    }

    public void doAdd(View v) {

    }

    Bitmap bmp_photo = null, bmp_sign = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 999) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("result");
                et_order.setText(result);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
        if (requestCode == SELECT_PHOTO_CODE ) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                try {
                    bmp_photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                img_photo.setImageURI(selectedImage);
            }
        }

        if (requestCode == TAKE_PHOTO_CODE) {
            if (resultCode == RESULT_OK) {
                File imgFile = new File(pictureImagePath);
                if (imgFile.exists()) {
                    bmp_photo = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    img_photo.setImageURI(Uri.parse(pictureImagePath));
                }
            }
        }
        if (requestCode == 888) {
            bmp_sign = (Bitmap) data.getExtras().get("result");
            img_sign.setImageBitmap(bmp_sign);
        }
    }
}
