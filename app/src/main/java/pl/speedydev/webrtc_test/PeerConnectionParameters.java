package pl.speedydev.webrtc_test;

/**
 * Created by Krystian on 24.02.2017.
 */

public class PeerConnectionParameters {
    public final boolean videoCallEnabled;

    public final int videoWidth;
    public final int videoHeight;
    public final int videoFps;
    public final String videoCodec;
    public final boolean videoCodecHwAcceleration;



    public PeerConnectionParameters(
            boolean videoCallEnabled,
            int videoWidth, int videoHeight, int videoFps,
            String videoCodec, boolean videoCodecHwAcceleration) {
        this.videoCallEnabled = videoCallEnabled;

        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoFps = videoFps;
        this.videoCodec = videoCodec;
        this.videoCodecHwAcceleration = videoCodecHwAcceleration;

    }
}