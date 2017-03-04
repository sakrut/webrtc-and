package pl.speedydev.webrtc_test.webapi.dto;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
/**
 * Created by Krystian on 27.02.2017.
 */

public class CallToPeer {

    @SerializedName("PeerId")
    @Expose
     String peerId;
    @SerializedName("CallTo")
    @Expose
     String callTo ;

    @SerializedName("SDP")
    @Expose
     String SDP ;

    public CallToPeer(String SDP, String callTo, String peerId) {
        this.SDP = SDP;
        this.callTo = callTo;
        this.peerId = peerId;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getCallTo() {
        return callTo;
    }

    public void setCallTo(String callTo) {
        this.callTo = callTo;
    }

    public String getSDP() {
        return SDP;
    }

    public void setSDP(String SDP) {
        this.SDP = SDP;
    }
}
