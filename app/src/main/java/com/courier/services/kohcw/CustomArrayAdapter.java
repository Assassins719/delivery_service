package com.courier.services.kohcw;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.xml.sax.SAXException;

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

public class CustomArrayAdapter extends ArrayAdapter<JobItem> {
    private final Context context;
    private ArrayList<JobItem> values = new ArrayList<>();
    public String strSID, strUSERID;

    public CustomArrayAdapter(Context context, ArrayList<JobItem> values) {
        super(context, R.layout.listitem, values);
        this.context = context;
        this.values.addAll(values);
    }

    public void setData(ArrayList<JobItem> values, String strSID, String strUSERID) {
        this.values.clear();
        this.values.addAll(values);
        this.strSID = strSID;
        this.strUSERID = strUSERID;
    }

    @Override
    public int getCount()
    {
        return values.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listitem, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.populateForm(values.get(position), position);
        if (position % 2 == 1) {
            // Set a background color for ListView regular row/item
            convertView.setBackgroundColor(Color.parseColor("#ffffff"));
        } else {
            // Set the background color for alternate row/item
            convertView.setBackgroundColor(Color.parseColor("#EdF3db"));
        }
        if (values.get(position).isExpand) {
            convertView.setBackgroundColor(Color.parseColor("#b2bfff"));
        }
        return convertView;
    }

    class ViewHolder {
        public TextView tx_no, tx_type, tx_from, tx_fromctc, tx_fromname, tx_fromtel, tx_to, tx_toctc, tx_toname, tx_totel;
        public Button btn_accept;
        public LinearLayout lyt;

        ViewHolder(View row) {
            tx_no = row.findViewById(R.id.tx_no);
            tx_type = row.findViewById(R.id.tx_type);
            tx_from = row.findViewById(R.id.tx_from);
            tx_fromctc = row.findViewById(R.id.tx_fromctc);
            tx_fromname = row.findViewById(R.id.tx_fromname);
            tx_fromtel = row.findViewById(R.id.tx_fromtel);
            tx_to = row.findViewById(R.id.tx_to);
            tx_toctc = row.findViewById(R.id.tx_toctc);
            tx_toname = row.findViewById(R.id.tx_toname);
            tx_totel = row.findViewById(R.id.tx_totel);
            btn_accept = row.findViewById(R.id.btn_accept);
            lyt = row.findViewById(R.id.lyt);

        }

        void populateForm(final JobItem jobItem, final int nIndex) {
            tx_no.setText(jobItem.OrderNo);
            tx_type.setText(jobItem.JobDorC + " | " + jobItem.JobTypeDesp);
            tx_fromctc.setText(jobItem.FromCTCPerson);
            tx_fromname.setText(jobItem.FromName + " | " + jobItem.FromAddress);
            tx_fromtel.setText(jobItem.FromTel + " | " + jobItem.FromHPNo);
            tx_toctc.setText(jobItem.ToCTCPerson);
            tx_toname.setText(jobItem.ToName + " | " + jobItem.ToAddress);
            tx_totel.setText(jobItem.ToTel + " | " + jobItem.ToHPNo);
            if (!jobItem.isExpand) {
                tx_from.setVisibility(View.GONE);
                tx_fromctc.setVisibility(View.GONE);
                tx_fromtel.setVisibility(View.GONE);
                tx_toctc.setVisibility(View.GONE);
                tx_totel.setVisibility(View.GONE);
                lyt.setBackgroundColor(Color.parseColor("#FFFFFF"));
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
            if (jobItem.Status.equals("NEW")) {
                btn_accept.setText("Accept");
            } else if(jobItem.Status.equals("IN PROCESS")){
                btn_accept.setText("Update");
            }else{
                btn_accept.setText("Update");
            }
            btn_accept.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(btn_accept.getText().equals("Accept")) {
//                        values.get(nIndex).Status = "IN PROCESS";
//                        notifyDataSetChanged();
                        acceptjob(jobItem, nIndex);
                    }else{
                        Intent intent = new Intent(context, Retrieve.class);
                        Global.retrieveJob = jobItem;
                        context.startActivity(intent);
                    }

                }
            });
        }
        public void acceptjob(JobItem jobItem, final int nIndex){
            Log.d("data", String.valueOf(jobItem.OrderNo));
            String strAPI = "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                    "  <soap12:Body>\n" +
                    "    <AcceptJob xmlns=\"http://tempuri.org/\">\n" +
                    "      <UserId>" + Global.strUserID + "</UserId>\n" +
                    "      <Sid>" + Global.strSID + "</Sid>\n" +
                    "      <OrderNo>" + jobItem.OrderNo + "</OrderNo>\n" +
                    "    </AcceptJob>\n" +
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
                        getResponse(mMessage, response, nIndex);
                    } catch (SAXException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        public void getResponse(String response, Response mainRes, final int nIndex) throws IOException, SAXException {

            DocumentBuilder newDocumentBuilder =
                    null;
            try {
                newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }

            String xml = response;
            XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
            JSONObject jsonObject = xmlToJson.toJson();
            try {
                JSONObject jsonEnvelope = (JSONObject) jsonObject.get("soap:Envelope");
                JSONObject jsonBody = (JSONObject) jsonEnvelope.get("soap:Body");
                JSONObject jsonResponse = (JSONObject) jsonBody.get("AcceptJobResponse");
                JSONObject jsonResult = (JSONObject) jsonResponse.get("AcceptJobResult");
                JSONObject jsonCONTWS = (JSONObject) jsonResult.get("CONTWSJOBUPD");
                JSONObject jsonJobs = (JSONObject) jsonCONTWS.get("AcceptJob");

                final String strResult = (String) jsonJobs.get("Result");

                Log.d("Data", strResult );

                ((Home)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(strResult.equals("Accepted")){
                            values.get(nIndex).Status = "IN PROCESS";
                            notifyDataSetChanged();
                        }
                        Toast.makeText(context, strResult,
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
            }
        }
    }
}