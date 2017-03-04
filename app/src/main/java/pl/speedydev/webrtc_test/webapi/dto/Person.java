package pl.speedydev.webrtc_test.webapi.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Krystian on 27.02.2017.
 */

public class Person {
    @SerializedName("Name")
    @Expose
     String name;

    public Person(String name) {
        this.name = name;
    }

    public Person() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
