package com.example.my_bluetooth10;


import android.widget.Button;

import android.content.Intent;

import android.os.Bundle;

import android.view.View;

import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class BluetoothActivity extends AppCompatActivity {
    Button back=null;
     ListView btList=null;
    Intent intent=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth1);
        btList=(ListView)findViewById(R.id.btList);
        back=(Button) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        intent = new Intent(BluetoothActivity.this, MainActivity.class);
        startActivity(intent);
    }
});
    }
}







