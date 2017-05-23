package media.apis.android.example.packagecom.wearrecordertest;


import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private boolean mPause = true;
    private Button record, save, cancel;
    private Chronometer mChronometer;
    private boolean mIsRecording = false;
    private long timeWhenStopped = 0;
    private GoogleApiClient mGoogleApiClient;
    private WavAudioRecorder mRecord;
    private String mRecordFilePath;
    public PendingResult<DataApi.DataItemResult> pendingResult;
    public PutDataRequest request;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        mRecordFilePath = Environment.getExternalStorageDirectory() + "/temp.wav";

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                record = (Button) findViewById(R.id.recordButton);
                save = (Button) findViewById(R.id.saveButton);
                cancel = (Button) findViewById(R.id.cancelButton);
                cancel.setVisibility(View.INVISIBLE);
                save.setVisibility(View.INVISIBLE);

                mChronometer = (Chronometer) findViewById(R.id.chronometer);

                mRecord = WavAudioRecorder.getInstance();

                record.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleAudioRecord();
                        cancel.setVisibility(View.VISIBLE);
                        save.setVisibility(View.VISIBLE);
                    }

                });

                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveRecording();
                        syncRecording();
                    }
                });

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelRecording();
                    }
                });
            }
        });
    }

    private void toggleAudioRecord() {
        if (!mIsRecording) {
            startRecording();
        } else {
            if (mPause) {
                resumeRecording();
            }
            else {
                pauseRecording();
            }
        }
    }

    private void startRecording() {
        mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        mChronometer.start();
        record.setText("Pause");
        mRecord.setOutputFile(mRecordFilePath);
        mRecord.prepare();
        mRecord.start();
        mIsRecording = true;
        mPause = false;
    }

    private void resumeRecording() {
        mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        mChronometer.start();
        record.setText("Pause");
        mPause = false;
        mRecord.resume();
    }

    private void pauseRecording() {
        timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
        mChronometer.stop();
        save.setText("Resume");
        mPause = true;
        mRecord.pause();
    }

    private void cancelRecording() {
        cancel.setVisibility(View.INVISIBLE);
        save.setVisibility(View.INVISIBLE);
        mChronometer.stop();
        mChronometer.setText("00:00");
        timeWhenStopped = 0;
        mIsRecording = false;
        mPause = true;
        mRecord.stop();
        mRecord.reset();
        deleteTempFile();
    }

    private void saveRecording() {
        cancel.setVisibility(View.INVISIBLE);
        save.setVisibility(View.INVISIBLE);
        mChronometer.stop();
        mChronometer.setText("00:00");
        timeWhenStopped = 0;
        mIsRecording = false;
        mPause = true;
        mRecord.stop();
        mRecord.reset();
    }

    private void syncRecording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (getNumberOfConnectedNodes() > 0) {
                    FileInputStream fileInputStream;
                    File file = new File(Environment.getExternalStorageDirectory().
                            getAbsolutePath() + "/temp.wav");
                    byte[] bFile = new byte[(int) file.length()];
                    //convert file into array of bytes
                    try {
                        fileInputStream = new FileInputStream(file);
                        fileInputStream.read(bFile);
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Asset asset = Asset.createFromBytes(bFile);
                    PutDataMapRequest dataMap = PutDataMapRequest.create(Environment.
                            getExternalStorageDirectory().getAbsolutePath() + "/recording.wav");
                    dataMap.getDataMap().putAsset("asset", asset);
                    dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());
                    request = dataMap.asPutDataRequest();
                    request.setUrgent();
                    pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            if (dataItemResult.getStatus().isSuccess()) {
                                //Log.d(TAG, "Data item set: " + dataItemResult.getDataItem().getUri());
                                Toast.makeText(getApplicationContext(), "Syncing data", Toast.LENGTH_LONG).show();
                            } else {
                                //Log.d(TAG, "ERROR: Data item lost");
                            }
                        }
                    });
                    deleteTempFile();
                }
                else {
                    // there are no connected nodes/devices
                }
            }
        }).start();
    }

    private int getNumberOfConnectedNodes() {
        List<Node> connectedNodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await().getNodes();
        return connectedNodes.size();
    }

    private void deleteTempFile() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp.wav");
        file.delete();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        if(mRecord!=null) mRecord.reset();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
