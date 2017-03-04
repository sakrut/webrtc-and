package pl.speedydev.webrtc_test;

import org.webrtc.PeerConnection;

import java.util.LinkedList;

/**
 * Created by Krystian on 24.02.2017.
 */

public class GlobalInfo {
    public static final String ProviderUrl = "http://webrtctestapi.speedydev.pl/api/";
    public static final LinkedList<PeerConnection.IceServer> IceServers = new LinkedList<>();
    public static final int VidewoHeight =320;
    public static final int VidewoWidth = 320;

    static {
    //    IceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        IceServers.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
        //IceServers.add(new PeerConnection.IceServer("stun:my.stunsrv.com","username","password"));
        // TURN
        //IceServers.add(new PeerConnection.IceServer("turn:my.turn.com","username","password"));
    }
}
