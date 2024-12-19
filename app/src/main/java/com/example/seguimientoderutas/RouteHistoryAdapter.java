package com.example.seguimientoderutas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpsmapapp.R;

import java.util.List;

public class RouteHistoryAdapter extends RecyclerView.Adapter<RouteHistoryAdapter.RouteViewHolder> {

    private List<String> routeHistory;

    // Constructor del adaptador
    public RouteHistoryAdapter(List<String> routeHistory) {
        this.routeHistory = routeHistory;
    }

    @Override
    public RouteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflar el layout de cada item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_history, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RouteViewHolder holder, int position) {
        // Asignar los datos al TextView correspondiente
        String route = routeHistory.get(position);
        holder.routeTextView.setText(route);
    }

    @Override
    public int getItemCount() {
        return routeHistory.size();
    }

    // ViewHolder para cada item en la lista
    public static class RouteViewHolder extends RecyclerView.ViewHolder {
        public TextView routeTextView;

        public RouteViewHolder(View itemView) {
            super(itemView);
            routeTextView = itemView.findViewById(R.id.routeInfoTextView);
        }
    }
}
