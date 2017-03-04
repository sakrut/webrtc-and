package pl.speedydev.webrtc_test.webapi;

        import java.util.List;

        import android.os.Handler;
        import android.util.Log;

        import okhttp3.ResponseBody;
        import pl.speedydev.webrtc_test.GlobalInfo;
        import pl.speedydev.webrtc_test.WebRtcClient;
        import pl.speedydev.webrtc_test.webapi.dto.ApiPeer;
        import pl.speedydev.webrtc_test.webapi.dto.CallToPeer;
        import pl.speedydev.webrtc_test.webapi.dto.Person;
        import retrofit2.Call;
        import retrofit2.Callback;
        import retrofit2.Response;
        import retrofit2.Retrofit;
        import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Krystian on 24.02.2017.
 */

public class ApiProvider {
    private IOnGetPeers listener;
    WebRtcTestApiInterface webRtcTestApiInterface;
    Retrofit restAdapter = null;
    private ApiPeer currentUser;
    private Handler hendler;
    private IGetOffer getOfferListener;
    private boolean stopTick;


    public ApiProvider(IOnGetPeers listener) {
        this.listener = listener;
        restAdapter = new Retrofit
                .Builder()
                .baseUrl(GlobalInfo.ProviderUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        webRtcTestApiInterface = restAdapter.create(WebRtcTestApiInterface.class);
    }

    public void singIn(String name)
    {
        webRtcTestApiInterface.singIn(new Person(name)).enqueue(new Callback<ApiPeer>() {
            @Override
            public void onResponse(Call<ApiPeer> call, Response<ApiPeer> response) {
                if(response.isSuccessful())
                {
                    currentUser = response.body();
                    getPeers();
                }
            }

            @Override
            public void onFailure(Call<ApiPeer> call, Throwable t) {
                t.printStackTrace();
            }
        });
        startCheckApi();
    }

    private void getPeers() {
        webRtcTestApiInterface.getPeers().enqueue(new Callback<List<ApiPeer>>() {
            @Override
            public void onResponse(Call<List<ApiPeer>> call, Response<List<ApiPeer>> response) {
                if(response.isSuccessful())
                {
                    if(listener !=null)
                        listener.OnGetPeers(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ApiPeer>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void disconect() {
        webRtcTestApiInterface.singOut(currentUser.getId());
    }

    public void SendOffer(String callTo, String descriptionString) {

                webRtcTestApiInterface.sendOffer(new CallToPeer(descriptionString,callTo,currentUser.getId())).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful())
                    Log.d("sakrut", "onResponse: "+response.message());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void setGetOfferListener(IGetOffer getOfferListener) {
        this.getOfferListener = getOfferListener;
    }

    public IGetOffer getGetOfferListener() {
        return getOfferListener;
    }


    public interface IGetOffer {
        boolean GetOffer(CallToPeer offer);
    }


    public interface IOnGetPeers{
        void OnGetPeers(List<ApiPeer> peers);
    }

    private void startCheckApi() {
        hendler = new Handler();
        hendler.postDelayed(tickR,1000);
    }

    private Runnable tickR = new Runnable() {
        @Override
        public void run() {
            if(!stopTick)
            hendler.postDelayed(tickR,2000);
            tick();
        }
    };
    private void tick(){
        if(currentUser != null) {
            getPeers();
            checkOfferForMe();
        }
    }

    private void checkOfferForMe() {
        webRtcTestApiInterface.getMy(currentUser.getId()).enqueue(new Callback<ApiPeer>() {
            @Override
            public void onResponse(Call<ApiPeer> call, Response<ApiPeer> response) {
                if(response.isSuccessful())
                {
                    ApiPeer body = response.body();
                    CallToPeer offer = body.getOffer();
                    if(offer != null) {

                        Log.d("sakrut", "on get offer");
                        stopTick = getOfferListener.GetOffer(offer);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiPeer> call, Throwable t) {

            }
        });
    }
}
