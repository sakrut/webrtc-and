package pl.speedydev.webrtc_test.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import pl.speedydev.webrtc_test.R;
import pl.speedydev.webrtc_test.webapi.dto.ApiPeer;

/**
 * Created by Krystian on 01.03.2017.
 */

public class PeersAdapter extends ArrayAdapter<ApiPeer> {
    private IOnPeerConnectClick listener;
    private  Context context;

    public PeersAdapter(Context context, ApiPeer[] objects, IOnPeerConnectClick listener) {
        super(context, -1, objects);
        this.context = context;
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.user_list_row, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.user_row_text);
        ImageButton imageButton = (ImageButton) rowView.findViewById(R.id.user_row_connect_button);
        final ApiPeer item = getItem(position);
        if (item != null) {
            textView.setText(item.getPerson().getName());
        }else
        {
            textView.setText("x_"+position);
        }
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.OnPeerConnectClick(item);
            }
        });

        return rowView;
    }

    public interface IOnPeerConnectClick {
        void OnPeerConnectClick(ApiPeer peer);
    }
}
