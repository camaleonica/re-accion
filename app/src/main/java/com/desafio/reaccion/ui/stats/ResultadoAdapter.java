package com.desafio.reaccion.ui.stats;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.desafio.reaccion.R;
import com.desafio.reaccion.data.model.ResultadoPartida;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultadoAdapter extends RecyclerView.Adapter<ResultadoAdapter.ViewHolder> {

    private final List<ResultadoPartida> items;
    private final String highlightPlayer;

    public ResultadoAdapter(List<ResultadoPartida> items, String highlightPlayer) {
        this.items           = items;
        this.highlightPlayer = highlightPlayer;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resultado, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResultadoPartida r = items.get(position);

        holder.tvRank.setText(String.format(Locale.getDefault(), "#%d", position + 1));
        holder.tvJugador.setText(r.jugador);
        holder.tvPuntos.setText(String.format(Locale.getDefault(), "%d pts", r.puntos));
        holder.tvModo.setText(r.modo + " \u2014 Nivel " + r.nivelAlcanzado + "/3");
        holder.tvTiempo.setText(String.format(Locale.getDefault(), "Promedio: %d ms", r.tiempoPromedio));
        holder.tvFecha.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(r.fecha)));

        boolean isHighlighted = highlightPlayer != null
                && highlightPlayer.equalsIgnoreCase(r.jugador);
        MaterialCardView card = (MaterialCardView) holder.itemView;
        card.setCardBackgroundColor(isHighlighted
                ? ContextCompat.getColor(card.getContext(), R.color.primary_container)
                : ContextCompat.getColor(card.getContext(), R.color.surface));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvRank, tvJugador, tvPuntos, tvModo, tvTiempo, tvFecha;

        ViewHolder(View v) {
            super(v);
            tvRank    = v.findViewById(R.id.tv_rank);
            tvJugador = v.findViewById(R.id.tv_jugador);
            tvPuntos  = v.findViewById(R.id.tv_puntos);
            tvModo    = v.findViewById(R.id.tv_modo);
            tvTiempo  = v.findViewById(R.id.tv_tiempo);
            tvFecha   = v.findViewById(R.id.tv_fecha);
        }
    }
}
