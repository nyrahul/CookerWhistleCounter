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
import android.util.Log;


public class MainActivity extends AppCompatActivity {
    private SoundMeter sm;
    private Thread thread;
    private static final int SAMPLE_DELAY = 160;
    private boolean micRecording = false;
    private MediaPlayer player;
    private int whistleCount = 0;

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
        // commenting it out as the app's theme is providing an action bar,
        // but the code is also trying to create one. This conflict causes java.lang.IllegalStateException error.

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

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
        whistleCount++;
        TextView av = findViewById(R.id.whistleCount);
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
                                av.setText("amp=" + amp +
                                        " freq=" + sm.freq +
                                        " samp=" + samp_cnt +
                                        " miss=" + miss_cnt);
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
        if (sm != null) {
            sm.stop();
            sm = null;
        }

        // Check thread state before interrupting
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            thread = null;
        } else {
            // Log a message if thread is already stopped or not initialized
            Log.w("MainActivity", "Thread already stopped or not initialized");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    public void onStartStopButton(View view) {
        Button startStopButton = findViewById(R.id.reset);
        if (!micRecording) {
            startRecording();
            startStopButton.setText("Pause");
        } else {
            stopRecording();
            startStopButton.setText("Resume");
        }
        micRecording = !micRecording;
    }

    public void onResetButton(View view) {
        // Reset state for a new recording
        TextView av = findViewById(R.id.whistleCount);
        TextView amp = findViewById(R.id.ampBox);

        stopRecording();  // Stop any ongoing recording
        samp_cnt = 0;
        miss_cnt = 0;
        whistleCount = 0;
        if (player.isPlaying()) {
            player.pause();
        }
        micRecording = false;  // Ensure consistent state

        // Update UI elements for a fresh start
        Button startStopButton = findViewById(R.id.reset);
        startStopButton.setText("Start");
        av.setText(String.valueOf(whistleCount));
        amp.setText("amp=" + 0 +
                " freq=" + 0 +
                " samp=" + 0 +
                " miss=" + 0);

    }

}
