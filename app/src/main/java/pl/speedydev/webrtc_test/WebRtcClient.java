package pl.speedydev.webrtc_test;

import android.content.Context;
import android.media.AudioManager;

import android.util.Log;


import org.webrtc.AudioSource;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoSource;

import java.util.HashMap;
import java.util.LinkedList;


import pl.speedydev.webrtc_test.webapi.ApiProvider;
import pl.speedydev.webrtc_test.webapi.dto.CallToPeer;

/**
 * Created by Krystian on 24.02.2017.
 */

public class WebRtcClient implements ApiProvider.IGetOffer{
    private final static String TAG = WebRtcClient.class.getCanonicalName();

    private final ApiProvider apiProvider;

    private PeerConnectionFactory factory;
    private HashMap<String, Peer> peers = new HashMap<>();
    private LinkedList<PeerConnection.IceServer> iceServers ;
    private PeerConnectionParameters pcParams;

    private MediaConstraints pcConstraints = new MediaConstraints();
    private MediaStream localMS;
    private VideoSource localVideoSource;
    private volatile RtcListener mListener;

    private boolean isSendLocalDescription;
    private AudioSource localAudioSource;
    private boolean isOnCamera = true;
    private boolean isMute = false;
    private AudioManager audioManager;
    private boolean isEnableFrontCamera = true;




    public WebRtcClient(RtcListener listener, PeerConnectionParameters params, ApiProvider api) {
        mListener = listener;
        pcParams = params;

        apiProvider = api;
        apiProvider.setGetOfferListener(this);

        iceServers= GlobalInfo.IceServers;

        PeerConnectionFactory.initializeAndroidGlobals(listener, true, true,
                params.videoCodecHwAcceleration);
        factory = new PeerConnectionFactory();
        //MediaConstraints pcConstraints = appRtcClient.pcConstraints();
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("RtpDataChannels", "true"));
    }


    public void turnOffCamera() {
        if(localVideoSource != null) {
            if(isOnCamera)
            {
                localVideoSource.stop();
                isOnCamera = false;
            }else{
                localVideoSource.restart();
                isOnCamera = true;
            }
        }
    }

    public boolean getIsOnCamera() {
        return isOnCamera;
    }

    public boolean getIsMute() {
        return isMute;
    }

    public void mutteMicrophone() {
        if(audioManager == null)
            audioManager = (AudioManager)mListener.getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMicrophoneMute(!isMute);
        isMute = !isMute;
    }

    public boolean isEnableFrontCamera() {
        return isEnableFrontCamera;
    }
    boolean iGetOffer = false;
    @Override
    public boolean GetOffer(CallToPeer offer) {
        if(iGetOffer)
            return true;
        if(peers != null && peers.containsKey(offer.getPeerId()) && peers.get(offer.getPeerId()).isOwner())
        {
            Log.d("sakrut", "GetOffer - setRemoteSdpAnswer");
            peers.get(offer.getPeerId()).setRemoteSdpAnswer(offer.getSDP());
        }else
        {
            Log.d("sakrut", "GetOffer - answerToPeer");
            answerToPeer(offer.getPeerId(), offer);
            mListener.onStartCalling();
        }
        iGetOffer = true;
        return true;
    }


    private class Peer implements SdpObserver, PeerConnection.Observer {
        private final boolean owner;
        private PeerConnection pc;
        private String id;
        private volatile SessionDescription localSdp;
        private volatile SessionDescription remoteSdp;
        public volatile LinkedList<IceCandidate> queuedLocalCandidates = new LinkedList<>();
        public volatile LinkedList<IceCandidate> queuedRemoteCandidates = new LinkedList<>();
        private String mtuCallID;

        public Peer(String id, boolean owner) {
            this.owner = owner;
            Log.d(TAG, "new Peer: " + id );
            this.pc = factory.createPeerConnection(iceServers, pcConstraints, this);
            this.id = id;
            mListener.onStatusChanged("CONNECTING",false);
        }

        public boolean isOwner() {
            return owner;
        }

        private void sendOfferToApi() {
            Log.d("sakrut", "Send LOCAL DESCRIPTION");
            isSendLocalDescription = true;
                SessionDescription desc = pc.getLocalDescription();
                String descriptionString = desc.description;
                Log.d("SakSDP",descriptionString);
                apiProvider.SendOffer(id, descriptionString);
        }


        public void setRemoteSdpAnswer(String sdp) {
            remoteSdp = new SessionDescription(SessionDescription.Type.ANSWER,sdp);

            pc.setRemoteDescription(Peer.this, remoteSdp);
        }


        @Override
        public void onCreateSuccess(SessionDescription sdp) {
            // change sdp Bandwidth
            /*int audioBandwidth = .getAudioBandwidth();
            int videoBandwidth = .getVideoBandwidth();
            String desc = sdp.description.replace("/a=mid:audio\r\n/g", "a=mid:audio\r\nb=AS:" + audioBandwidth + "\r\n");
            desc = desc.replace("/a=mid:video\r\n/g", "a=mid:video\r\nb=AS:" + videoBandwidth + "\r\n");
            sdp = new SessionDescription(sdp.type,desc); */
            Log.d(TAG, "onCreateSuccess: Is local desc");
            localSdp = sdp;
            pc.setLocalDescription(Peer.this, sdp);
            if (!this.isOwner()) {
                sendOfferToApi();
            }
        }


        @Override
        public void onSetSuccess() {
            if (pc.getRemoteDescription() != null) {
                drainRemoteCandidates();
            }
        }


        @Override
        public void onCreateFailure(String s) {
            Log.d(TAG, "onCreateFailure: "+s);mListener.onStatusChanged(s,true);
        }


        @Override
        public void onSetFailure(String s) {
            Log.d(TAG, "onSetFailure: "+s);mListener.onStatusChanged(s,true);
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            mListener.onStatusChanged(signalingState.toString(),true);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            mListener.onStatusChanged("ICE -"+iceConnectionState.toString(),true);
            Log.d("onIceConnectionChange", iceConnectionState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
        }


        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
                sendOfferToApi();
            }
            Log.d("onIceGatheringChange", iceGatheringState.toString());
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            queuedLocalCandidates.add(candidate);
            Log.d("onIceCandidate", candidate.toString());
        }


        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "onAddStream " + mediaStream.label());
            mListener.onAddRemoteStream(mediaStream);
        }


        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            if(mListener != null)
                mListener.onRemoveRemoteStream();
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            try {
                Log.d(TAG, "onDataChannel: " + dataChannel.state().name() );
            }catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        @Override
        public void onRenegotiationNeeded() {

        }


        public void drainRemoteCandidates() {
            if (queuedRemoteCandidates != null && queuedRemoteCandidates.size() > 0) {
                for (IceCandidate candidate : queuedRemoteCandidates) {
                    pc.addIceCandidate(candidate);
                }
                queuedRemoteCandidates = null;
            }
        }

        public void disconnect() {

        }
    }




    private Peer addPeer(String id,boolean owner) {
        Peer peer = new Peer(id,owner);
        peers.put(id, peer);
        return peer;
    }


    public void connectToPeer(String id) {
        Peer peer = addPeer(id,true);
        setLocalMS();
        peer.pc.addStream(localMS);
        peer.pc.createOffer(peer, pcConstraints);
    }
    public void answerToPeer(String id, CallToPeer offer) {
        Peer peer = addPeer(id,false);
        setLocalMS();
        peer.pc.addStream(localMS);
        peer.pc.setRemoteDescription(peer,new SessionDescription(SessionDescription.Type.OFFER,offer.getSDP()));
        peer.pc.createAnswer(peer, pcConstraints);
       
    }


    public void onPause() {
        if (localVideoSource != null) localVideoSource.stop();
    }


    public void onResume() {
        if (localVideoSource != null) localVideoSource.restart();
    }


    public void onDestroy() {
        if(isMute)
        {
            mutteMicrophone();
        }
        for (Peer peer : peers.values()) {
            try{
                Log.d("sakrut", "disconect post go");
                peer.disconnect();

            }catch (Exception ex){ ex.printStackTrace();}
            try {
                peer.pc.dispose();
            } catch (Exception ex) {
            }
        }
        apiProvider.disconect();
        peers.clear();
        if (localVideoSource != null) {
            localVideoSource.dispose();
            localVideoSource = null;
        }
        if (factory != null) {
            factory.dispose();
            factory = null;
        }

    }


    public void setLocalMS() {
        localMS = factory.createLocalMediaStream("ARDAMS");
        if (pcParams.videoCallEnabled) {
            MediaConstraints videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", Integer.toString(GlobalInfo.VidewoHeight)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", Integer.toString(GlobalInfo.VidewoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(pcParams.videoHeight)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(pcParams.videoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(60)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(10)));

            VideoCapturer cam = getVideoCapturer();
            if (cam != null) {
                localVideoSource = factory.createVideoSource(cam, videoConstraints);
                localMS.addTrack(factory.createVideoTrack("ARDAMSv0", localVideoSource));
            }else
            {
                isOnCamera = false;
                isEnableFrontCamera = false;
            }

        }

        localAudioSource = factory.createAudioSource(new MediaConstraints());
        localMS.addTrack(factory.createAudioTrack("ARDAMSa0", localAudioSource));

        mListener.onLocalStream(localMS);

    }


    private VideoCapturer getVideoCapturer() {
        String frontCameraDeviceName = CameraEnumerationAndroid.getNameOfFrontFacingDevice();
        if (frontCameraDeviceName == null)
            return null;
        return VideoCapturerAndroid.create(frontCameraDeviceName);
    }




    public interface RtcListener {


        void onStatusChanged(String newStatus, boolean longTime);

        void onLocalStream(MediaStream localStream);

        void onAddRemoteStream(MediaStream remoteStream);

        void onRemoveRemoteStream();

        void onMcuIsDisconect();
        void onStartCalling();

        Context getContext();

    }
}
