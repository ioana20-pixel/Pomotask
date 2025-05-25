package com.example.pomodoroapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {
    private List<Session> sessionList;

    public SessionAdapter(List<Session> sessions) {
        this.sessionList = sessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessionList.get(position);
        holder.tvType.setText("Pomodoro Set");
        holder.tvSessionNum.setText("Set " + session.sessionNum);

        // For grouped sets, session.startTime is already like "14:00 - 16:00"
        holder.tvTime.setText(session.startTime);

        // Show duration in minutes
        holder.tvDuration.setText((session.duration / 60) + "m");
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvSessionNum, tvTime, tvDuration;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvType);
            tvSessionNum = itemView.findViewById(R.id.tvSessionNum);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }
    }
}
