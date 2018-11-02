package com.courier.services.kohcw;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.github.gcacace.signaturepad.views.SignaturePad;

public class DrawSign extends AppCompatActivity {
    SignaturePad mSignaturePad;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_sign);
        mSignaturePad = (SignaturePad) findViewById(R.id.signature_pad);
        mSignaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {

            @Override
            public void onStartSigning() {
                //Event triggered when the pad is touched
            }

            @Override
            public void onSigned() {
                //Event triggered when the pad is signed
            }

            @Override
            public void onClear() {
                //Event triggered when the pad is cleared
            }
        });

    }
    public void doClear(View v){
        mSignaturePad.clear();
    }
    public void doSave(View v){
        Bitmap photo = mSignaturePad.getSignatureBitmap();
        final float densityMultiplier = this.getResources().getDisplayMetrics().density;

        int h= (int) (100*densityMultiplier);
        int w= (int) (h * photo.getWidth()/((double) photo.getHeight()));

        photo=Bitmap.createScaledBitmap(photo, w, h, true);


        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", photo);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
    @Override
    public void onBackPressed()
    {
        // code here to show dialogneeds
        Bitmap photo = mSignaturePad.getSignatureBitmap();
        final float densityMultiplier = this.getResources().getDisplayMetrics().density;

        int h= (int) (100*densityMultiplier);
        int w= (int) (h * photo.getWidth()/((double) photo.getHeight()));

        photo=Bitmap.createScaledBitmap(photo, w, h, true);


        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", photo);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}
