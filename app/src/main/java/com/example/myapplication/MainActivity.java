package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private SoundMeter sm;
    private Thread thread;
    private static final int SAMPLE_DELAY = 160;
    private boolean micRecording = false;
    private MediaPlayer player;

    private void checkRecordPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkRecordPermission();
        player = MediaPlayer.create(this,
                Settings.System.DEFAULT_RINGTONE_URI);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int samp_cnt = 0, miss_cnt = 0;
    private static final double AMP_MIN = 1500.0, FREQ_MIN = 1200.0;

    private int getUserRequestedCount() {
        EditText av = findViewById(R.id.numOfWhistles);
        if(av == null) {
            return -1;
        }
        return Integer.parseInt(av.getText().toString());
    }

    private void incrementWhistleCount() {
        TextView av = findViewById(R.id.whistleCount);
        if(av == null) {
            return;
        }
        int whistleCount = Integer.parseInt(av.getText().toString());
        whistleCount++;
        av.setText(String.valueOf(whistleCount));
        if(whistleCount >= getUserRequestedCount()) {
            if(!player.isPlaying()) {
                player.start();
            }
        }
    }

    private void handleSample(double amp, double freq) {
        if(amp > AMP_MIN && freq > FREQ_MIN) {
            samp_cnt++;
            if(samp_cnt > 5) {
                miss_cnt = 0;
            }
        } else {
            miss_cnt++;
            if(miss_cnt > 3) {
                if(samp_cnt > 10) {
                    incrementWhistleCount();
                }
                samp_cnt = 0;
            }
        }
    }

    private void startRecording() {
        sm = new SoundMeter();
        sm.start();
        thread = new Thread(new Runnable() {
            public void run() {
                while (thread != null && !thread.isInterrupted()) {
                    try {
                        Thread.sleep(SAMPLE_DELAY);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView av = findViewById(R.id.ampBox);
                            if(sm != null) {
                                double amp = sm.getAmplitude();
                                handleSample(amp, sm.freq);
                                av.setText("amp=" + String.valueOf(amp) +
                                        " freq=" + String.valueOf(sm.freq) +
                                        " samp=" + String.valueOf(samp_cnt) +
                                        " miss=" + String.valueOf(miss_cnt));
                            }
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void stopRecording() {
        try {
            if(sm != null) {
                sm.stop();
                sm = null;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        thread.interrupt();
        thread = null;
    }

    public void onButton(View view) {
        Button bt = findViewById(R.id.reset);
        if(micRecording == false) {
            startRecording();
            bt.setText("Stop");
        } else {
            stopRecording();
            bt.setText("Start");
            samp_cnt = 0;
            miss_cnt = 0;
            if(player.isPlaying()) {
                player.pause();
            }
        }
        micRecording = !micRecording;
    }
}
