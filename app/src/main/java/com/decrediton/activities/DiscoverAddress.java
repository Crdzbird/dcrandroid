package com.decrediton.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.decrediton.R;
import com.decrediton.util.MyCustomTextView;
import com.decrediton.util.PreferenceUtil;
import com.decrediton.util.Utils;

import java.util.Locale;

import dcrwallet.Dcrwallet;

/**
 * Created by collins on 2/1/18.
 */

public class DiscoverAddress extends AppCompatActivity implements Animation.AnimationListener {
    Animation animRotate;
    ImageView imgAnim;
    MyCustomTextView tvLoading;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_page);
        imgAnim = findViewById(R.id.splashscreen_icon);
        animRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_rotate);
        animRotate.setAnimationListener(this);
        tvLoading = findViewById(R.id.loading_status);
        showInputPassPhraseDialog();
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        imgAnim.startAnimation(animRotate);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    public void showInputPassPhraseDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.input_passphrase_box, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final EditText passphrase = dialogView.findViewById(R.id.passphrase_input);

        dialogBuilder.setMessage("Enter your passphrase");
        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String pass = passphrase.getText().toString();
                if(pass.length() > 0){
                    discover(pass);
                }else{
                    Toast.makeText(DiscoverAddress.this, "Invalid Passphrase", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialogBuilder.setCancelable(true);
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(b.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }


    public void discover(final String pass){
        imgAnim.startAnimation(animRotate);
        new Thread(){
            public void run(){
                try {
                    PreferenceUtil util = new PreferenceUtil(DiscoverAddress.this);
                    setText("Discovering Addresses");
                    Dcrwallet.discoverAddresses(pass);
                    util.set("discover_address", "true");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DiscoverAddress.this, "Done", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(DiscoverAddress.this, "ERROR: "+e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }.start();
    }

    private void setText(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLoading.setText(str);
            }
        });
    }

}
