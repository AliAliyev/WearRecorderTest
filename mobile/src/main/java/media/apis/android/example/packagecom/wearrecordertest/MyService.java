package media.apis.android.example.packagecom.wearrecordertest;

/**
 * Created by Ali on 21/05/2017.
 */

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyService extends Service implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = "Recorder";

    public MyService() {

    }

    @Override
    public void onCreate() {
        // The service is being created
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        String savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        savePath += "/WearRecorder";
        new File(savePath).mkdirs();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped
        //Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Toast.makeText(getApplicationContext(), "Receiving data", Toast.LENGTH_LONG).show();
        //Log.d(TAG, "data changed spotted");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(Environment.getExternalStorageDirectory().
                            getAbsolutePath() + "/recording.wav")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset myAsset = dataMapItem.getDataMap().getAsset("asset");
                Wearable.DataApi.getFdForAsset(mGoogleApiClient, myAsset).setResultCallback(
                        new ResultCallback<DataApi.GetFdForAssetResult>() {
                            @Override
                            public void onResult(@NonNull DataApi.GetFdForAssetResult getFdForAssetResult) {
                                InputStream assetInputStream = getFdForAssetResult.getInputStream();
                                // give each recording a unique name
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.ENGLISH);
                                final String current = sdf.format(new Date());
                                File file = new File(Environment.getExternalStorageDirectory().
                                        getAbsolutePath()+ "/WearRecorder/" + "Recording " + current + ".wav");
                                try {
                                    FileOutputStream fOut = new FileOutputStream(file);
                                    int nRead;
                                    byte[] data = new byte[16384];
                                    while ((nRead = assetInputStream.read(data, 0, data.length)) != -1) {
                                        fOut.write(data, 0, nRead);
                                    }
                                    fOut.flush();
                                    fOut.close();
                                } catch (IOException e) {
                                    //System.out.println("ERROR File write failed: " + e.toString());
                                }
                            }
                        }
                );
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG).show();
    }
}