package com.donkingliang.imageselector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.Image;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by CM on 2021/7/4.
 */

public class PicPreviewSelectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private Context context;
    private List<Image> selectImages = new ArrayList<>();
    private int clicks_position = -1;
    private OnPicItemClickListener listener;
    public PicPreviewSelectAdapter(Context context,List<Image> selectImages){
        this.context = context;
        this.selectImages  = selectImages;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pic_preview, parent, false);
        return new PicPreviewViewHolder(view);

    }

    public void setOnPicClickListener(OnPicItemClickListener listener){
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        final PicPreviewViewHolder contentHolder = (PicPreviewViewHolder) holder;
        if(clicks_position == position){
            contentHolder.item_pic_stroke.setVisibility(View.VISIBLE);
        }else {
            contentHolder.item_pic_stroke.setVisibility(View.GONE);
        }
        Glide.with(context)
                .load(selectImages.get(position).getPath())
                .thumbnail(0.2f)
                .into(contentHolder.item_pic_img);

        contentHolder.item_pic_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != listener){
                    listener.OnPicItemClickListener(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectImages.size();
    }

    public void setClickPosition(int position_click){
        clicks_position = position_click;
        notifyDataSetChanged();
    }

    public class  PicPreviewViewHolder extends RecyclerView.ViewHolder {
        View headerView;
        ImageView item_pic_img;
        TextView item_pic_stroke;

        public PicPreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            headerView = itemView;
            item_pic_img = itemView.findViewById(R.id.item_pic_img);
            item_pic_stroke = itemView.findViewById(R.id.item_pic_stroke);
        }
    }

    public interface OnPicItemClickListener{
        void OnPicItemClickListener(int position);
    }
}
