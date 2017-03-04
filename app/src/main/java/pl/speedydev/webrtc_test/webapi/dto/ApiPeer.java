package pl.speedydev.webrtc_test.webapi.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Krystian on 27.02.2017.
 */

public class ApiPeer {
    @SerializedName("Id")
    @Expose
     String id;
    @SerializedName("Person")
    @Expose
     Person person;

    @SerializedName("InCall")
    @Expose
     boolean InCall;
    @SerializedName("Offer")
    @Expose
     CallToPeer Offer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public boolean isInCall() {
        return InCall;
    }

    public void setInCall(boolean inCall) {
        InCall = inCall;
    }

    public CallToPeer getOffer() {
        return Offer;
    }

    public void setOffer(CallToPeer offer) {
        Offer = offer;
    }
}
