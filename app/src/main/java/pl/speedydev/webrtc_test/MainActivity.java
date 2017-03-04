package pl.speedydev.webrtc_test;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.List;

import pl.speedydev.webrtc_test.list.PeersAdapter;
import pl.speedydev.webrtc_test.webapi.ApiProvider;
import pl.speedydev.webrtc_test.webapi.dto.ApiPeer;

public class MainActivity extends AppCompatActivity implements WebRtcClient.RtcListener, ApiProvider.IOnGetPeers, PeersAdapter.IOnPeerConnectClick {

    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 72;
    private static final int LOCAL_Y_CONNECTING = 3;
    private static final int LOCAL_WIDTH_CONNECTING = 25;
    private static final int LOCAL_HEIGHT_CONNECTING = 25;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 3;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType .SCALE_ASPECT_FILL;
    //private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private String callerId;
    private static Context appContext;
    private boolean isRemoteOnScreen;
    private VideoRenderer.Callbacks remoteRenderPriview;
    private ImageButton disConnectImageButton;
    private Button switchCameraButton;
    private ImageButton stopCameraButton;
    private ImageButton stopMicrophonButton;

    private ProgressDialog progressDialog;
    private ImageButton disconnectButton;
    private ListView usersListView;
    private boolean rebootApplicationPostEnd = false;
    private ApiProvider apiProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPerrmision();
        apiProvider = new ApiProvider(this);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_main);

        prepareView();

        vsv.setVisibility(View.INVISIBLE);
        usersListView = (ListView)findViewById(R.id.clientsListView);
        disconnectButton = (ImageButton)findViewById(R.id.disconnect_button);
        switchCameraButton = (Button)findViewById(R.id.switchCameraButton);
        stopCameraButton = (ImageButton)findViewById(R.id.stop_camera);
        stopMicrophonButton = (ImageButton)findViewById(R.id.stop_micro);
        disConnectImageButton = (ImageButton)findViewById(R.id.disconnect_button);
        showDialogForName();
        createWebRtcClient();
    }

    private void createWebRtcClient() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        boolean usHWac = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP  && getWindow().getDecorView().isHardwareAccelerated();
        PeerConnectionParameters params = null;
         if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            params = new PeerConnectionParameters(
                    true,  640, 480, 30, VIDEO_CODEC_VP9, usHWac);
            else
        params = new PeerConnectionParameters(
                true,  displaySize.x, displaySize.y, 15, VIDEO_CODEC_VP9, usHWac);
        client = new WebRtcClient(this, params, apiProvider);
    }

    private void checkPerrmision() {

        if ( ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        101);
        }
    }

    private void showDialogForName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Podaj imie");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT );
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    apiProvider.singIn(input.getText().toString());
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }

            }
        });
        builder.setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);
            }
        });
        builder.show();
    }

    private void prepareView() {
        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setEGLContextClientVersion(2);
        vsv.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {

            }
        });
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
        remoteRenderPriview = VideoRendererGui.create(
                0, 0,
                0, 0, scalingType, false);
    }


    @Override
    public void onStatusChanged(final String newStatus, final boolean longTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, longTime ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onLocalStream(final MediaStream localStream) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
                VideoRendererGui.update(localRender,
                        LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                        LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                        scalingType, true);
            }
        });

    }

    @Override
    public void onAddRemoteStream(final MediaStream remoteStream) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
                remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRenderPriview));
                VideoRendererGui.update(remoteRender,
                        REMOTE_X, REMOTE_Y,
                        REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
                if (client.getIsOnCamera())
                    VideoRendererGui.update(localRender,
                            LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                            LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                            scalingType, true);
                isRemoteOnScreen = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        disConnectImageButton.setVisibility(View.VISIBLE);
                    }
                });
                if (progressDialog != null)
                    progressDialog.dismiss();

                AudioManager am = (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);
                am.setSpeakerphoneOn(true);
            }
        });

    }

    @Override
    public void onRemoveRemoteStream() {
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disConnectButtonClick(null);
            }
        });

    }

    @Override
    public void onMcuIsDisconect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        });

    }
    public Context  getContext(){
        return this;
    }

    @Override
    public void onStartCalling() {
        vsv.setVisibility(View.VISIBLE);
        if(client.isEnableFrontCamera()) {
            stopCameraButton.setVisibility(View.VISIBLE);
            switchCameraButton.setEnabled(true);
        }
        stopMicrophonButton.setVisibility(View.VISIBLE);
        usersListView.setVisibility(View.INVISIBLE);
        usersListView.setEnabled(false);
    }



    private void connectToPeer(ApiPeer peer) {
        client.connectToPeer(peer.getId());
        startCallingView();
    }

    private void startCallingView() {
        usersListView.setVisibility(View.INVISIBLE);
        vsv.setVisibility(View.VISIBLE);

        if(client.isEnableFrontCamera()) {
            stopCameraButton.setVisibility(View.VISIBLE);
            switchCameraButton.setEnabled(true);
        }
        stopMicrophonButton.setVisibility(View.VISIBLE);

        progressDialog = ProgressDialog.show(MainActivity.this, "Trwa łączenie", "Proszę czekać...", true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        disConnectButtonClick(disConnectImageButton);
                    }
                });
            }
        });
    }


    public void turnOffCamera(View Button) {
        if (client != null) {
            client.turnOffCamera();
            if (client.getIsOnCamera()) {
                ((ImageButton)Button).setImageResource(R.drawable.android_videocam);
                VideoRendererGui.update(localRender,
                        LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                        LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                        scalingType, true);
                switchCameraButton.setEnabled(true);
            } else {
                if(!isRemoteOnScreen)
                    changeCamera(Button);
                VideoRendererGui.update(localRender,
                        0, 0,
                        0, 0,
                        scalingType, false);
                VideoRendererGui.update(remoteRenderPriview,
                        0, 0,
                        0, 0,
                        scalingType, false);
                switchCameraButton.setEnabled(false);
                ((ImageButton)Button).setImageResource(R.drawable.android_videocam_off);
            }
        }
    }


    public void changeCamera(View view) {
        if (isRemoteOnScreen) {
            VideoRendererGui.update(remoteRender,
                    0, 0,
                    0, 0,
                    scalingType, false);
            VideoRendererGui.update(localRender,
                    REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, true);

            VideoRendererGui.update(remoteRenderPriview,
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                    LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                    scalingType, false);

            isRemoteOnScreen = false;

        } else {

            VideoRendererGui.update(remoteRender,
                    REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
            VideoRendererGui.update(localRender,
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                    LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                    scalingType, true);
            VideoRendererGui.update(remoteRenderPriview,
                    0, 0,
                    0, 0,
                    scalingType, false);


            isRemoteOnScreen = true;
        }
    }


    public void disConnectButtonClick(View view) {
        disconnect();
    }

    public void disconnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Trwa rozłączanie...", Toast.LENGTH_LONG).show();
            }
        });
        closeAplication();
    }

    public void closeAplication() {
        if (client != null)
            client.onDestroy();
        System.exit(0);
    }


    public void turnOffmicro(View Button) {
        if (client != null) {
            client.mutteMicrophone();
            if (client.getIsMute()) {
                ((ImageButton)Button).setImageResource(R.drawable.android_mic_off);
            } else {
                ((ImageButton)Button).setImageResource(R.drawable.android_mic);
            }
        }
    }


    @Override
    public void OnGetPeers(List<ApiPeer> peers) {
        ApiPeer[] array = new ApiPeer[peers.size()];
        peers.toArray(array);
        PeersAdapter adapter = new PeersAdapter(this,array,this);
        usersListView.setAdapter(adapter);
    }

    @Override
    public void OnPeerConnectClick(ApiPeer peer) {
        connectToPeer(peer);
    }
}
