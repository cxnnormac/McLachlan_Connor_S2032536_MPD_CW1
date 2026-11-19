package org.me.gcu.mclachlan_connor_s2032536;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.List;

public class SituationAdapter extends ArrayAdapter<TrafficSituation> {

    public SituationAdapter(@NonNull Context context, @NonNull List<TrafficSituation> situations) {
        super(context, 0, situations);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_situation, parent, false);
        }

        TrafficSituation item = getItem(position);
        if (item == null) return convertView;

        LinearLayout container = convertView.findViewById(R.id.rowContainer);
        ImageView icon = convertView.findViewById(R.id.imgSituationIcon);
        TextView tvType = convertView.findViewById(R.id.tvSituationType);
        TextView tvBadge = convertView.findViewById(R.id.tvDelayBadge);
        TextView tvComment = convertView.findViewById(R.id.tvSituationComment);

        tvType.setText(item.getSituationType() + " (" + item.getRoadName() + ")");
        tvComment.setText(item.getComment() != null ? item.getComment() : "No public comment available.");

        if (item.isRoadwork()) {
            icon.setImageResource(android.R.drawable.ic_menu_manage);
            if ("longDelays".equalsIgnoreCase(item.getDelaysType())) {
                container.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.delay_long_bg));
                tvBadge.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.delay_long_badge));
                tvBadge.setText("LONG DELAYS");
            } else {
                container.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.delay_short_bg));
                tvBadge.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.delay_short_badge));
                tvBadge.setText("SHORT DELAYS");
            }
        } else {
            if ("AbnormalTraffic".equalsIgnoreCase(item.getSituationType())) {
                icon.setImageResource(android.R.drawable.ic_dialog_info);
            } else {
                icon.setImageResource(android.R.drawable.ic_dialog_alert);
            }
            container.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.incident_bg));
            tvBadge.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.incident_badge));
            tvBadge.setText("INCIDENT");
        }

        return convertView;
    }
}