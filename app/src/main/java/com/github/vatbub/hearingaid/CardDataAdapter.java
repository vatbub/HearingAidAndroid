package com.github.vatbub.hearingaid;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by frede on 13.01.2018.
 */

public class CardDataAdapter extends RecyclerView.Adapter<CardDataAdapter.ViewHolder> {
    private ArrayList<String> countries;

    public CardDataAdapter(ArrayList<String> countries) {
        this.countries = countries;
    }

    @Override
    public CardDataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CardDataAdapter.ViewHolder viewHolder, int i) {

        viewHolder.textView.setText(countries.get(i));
    }

    @Override
    public int getItemCount() {
        return countries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView textView;
        public ViewHolder(View view) {
            super(view);

            textView = view.findViewById(R.id.tv_country);
        }
    }
}
