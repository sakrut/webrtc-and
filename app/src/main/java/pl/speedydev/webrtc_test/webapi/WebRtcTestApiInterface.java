package pl.speedydev.webrtc_test.webapi;

import java.util.List;

import okhttp3.ResponseBody;
import pl.speedydev.webrtc_test.webapi.dto.ApiPeer;
import pl.speedydev.webrtc_test.webapi.dto.CallToPeer;
import pl.speedydev.webrtc_test.webapi.dto.Person;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * Created by Krystian on 24.02.2017.
 */

public interface WebRtcTestApiInterface {
    @GET("Call")
    Call<List<ApiPeer>> getPeers();

    @GET("Call")
    Call<ApiPeer> getMy(@Query("ID") String id);

    @PUT("Call")
    Call<ApiPeer> singIn(@Body Person my);

    @POST("Call")
    Call<ResponseBody> sendOffer(@Body CallToPeer my);

    @DELETE("Call")
    Call<ResponseBody> singOut(@Query("ID") String id);
}

