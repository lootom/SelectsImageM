package com.donkingliang.imageselector.adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.Image;
import com.donkingliang.imageselector.utils.DateUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Image> mImages;
    private LayoutInflater mInflater;

    //保存选中的图片
    private ArrayList<Image> mSelectImages = new ArrayList<>();
    private OnImageSelectListener mSelectListener;
    private OnItemClickListener mItemClickListener;
    private int mMaxCount;
    private boolean isSingle;
    private boolean isViewImage;

    private static final int TYPE_CAMERA = 1;
    private static final int TYPE_IMAGE = 2;

    private boolean useCamera;

    /**
     * @param maxCount    图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param isSingle    是否单选
     * @param isViewImage 是否点击放大图片查看
     */
    public ImageAdapter(Context context, int maxCount, boolean isSingle, boolean isViewImage) {
        mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
        mMaxCount = maxCount;
        this.isSingle = isSingle;
        this.isViewImage = isViewImage;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_IMAGE) {
            View view = mInflater.inflate(R.layout.adapter_images_item, parent, false);
            return new ViewHolder(view);
        } else {
            View view = mInflater.inflate(R.layout.adapter_camera, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_IMAGE) {
            final Image image = getImage(position);
            Glide.with(mContext).load(new File(image.getPath()))
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(holder.ivImage);

            setItemSelect(holder, mSelectImages.contains(image),image.getPath());

            holder.ivGif.setVisibility(image.isGif() ? View.VISIBLE : View.GONE);

            boolean video = image.isVideo();
            holder.videoLl.setVisibility(video ? View.VISIBLE : View.GONE);
            if (video) {
                //获取视频文件时长
                File file = new File(image.getPath());
                MediaPlayer meidaPlayer = new MediaPlayer();
                try {
                    meidaPlayer.setDataSource(file.getPath());
                    meidaPlayer.prepare();
                    long duration = meidaPlayer.getDuration();
                    holder.ivDuration.setText(DateUtils.getTimeToMinutesSeconds(duration));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //点击选中/取消选中图片
            holder.tvCheck2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkedImage(holder, image);
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isViewImage) {
                        if (mItemClickListener != null) {
                            int p = holder.getAdapterPosition();
                            mItemClickListener.OnItemClick(image, useCamera ? p - 1 : p);
                        }
                    } else {
                        checkedImage(holder, image);
                    }
                }
            });
        } else if (getItemViewType(position) == TYPE_CAMERA) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.OnCameraClick();
                    }
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (useCamera && position == 0) {
            return TYPE_CAMERA;
        } else {
            return TYPE_IMAGE;
        }
    }

    private void checkedImage(ViewHolder holder, Image image) {
        if (mSelectImages.contains(image)) {
            //如果图片已经选中，就取消选中
            unSelectImage(image);
            setItemSelect(holder, false, image.getPath());
        } else if (isSingle) {
            //如果是单选，就先清空已经选中的图片，再选中当前图片
            clearImageSelect();
            selectImage(image);
            setItemSelect(holder, true, image.getPath());
        } else if (mMaxCount <= 0 || mSelectImages.size() < mMaxCount) {
            //如果不限制图片的选中数量，或者图片的选中数量
            // 还没有达到最大限制，就直接选中当前图片。
            selectImage(image);
            setItemSelect(holder, true, image.getPath());
        }
    }

    /**
     * 选中图片
     *
     * @param image
     */
    private void selectImage(Image image) {
        mSelectImages.add(image);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(image, true, mSelectImages.size());
        }
    }

    /**
     * 取消选中图片
     *
     * @param image
     */
    private void unSelectImage(Image image) {
        mSelectImages.remove(image);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(image, false, mSelectImages.size());
        }
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return useCamera ? getImageCount() + 1 : getImageCount();
    }

    private int getImageCount() {
        return mImages == null ? 0 : mImages.size();
    }

    public ArrayList<Image> getData() {
        return mImages;
    }

    public void refresh(ArrayList<Image> data, boolean useCamera) {
        mImages = data;
        this.useCamera = useCamera;
        notifyDataSetChanged();
    }

    private Image getImage(int position) {
        return mImages.get(useCamera ? position - 1 : position);
    }

    public Image getFirstVisibleImage(int firstVisibleItem) {
        if (mImages != null && !mImages.isEmpty()) {
            if (useCamera) {
                return mImages.get(firstVisibleItem == 0 ? 0 : firstVisibleItem - 1);
            } else {
                return mImages.get(firstVisibleItem);
            }
        }
        return null;
    }

    /**
     * 设置图片选中和未选中的效果
     */
    private void setItemSelect(ViewHolder holder, boolean isSelect,String imgpath) {
        if (isSelect) {
//            holder.ivSelectIcon.setImageResource(R.drawable.icon_image_select);
            notifyCheckChanged(holder,imgpath, isSelect);
            holder.ivMasking.setAlpha(0.5f);
        } else {
            notifyCheckChanged(holder, imgpath,isSelect);
            holder.ivMasking.setAlpha(0.2f);
        }
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(ViewHolder viewHolder, String imgpath, boolean isSelect) {
        viewHolder.tvCheck.setText("");
        viewHolder.tvCheck.setBackgroundResource(R.drawable.rc_picture_check_normal);
        int size = mSelectImages.size();
        for (int i = 0; i < size; i++) {
            String media = mSelectImages.get(i).getPath();
            if (media.equals(imgpath)) {
                viewHolder.tvCheck.setText(String.valueOf(i+1));
                viewHolder.tvCheck.setBackgroundResource(R.drawable.rc_picture_check_selected_red);
            }
        }
    }

    private void clearImageSelect() {
        if (mImages != null && mSelectImages.size() == 1) {
            int index = mImages.indexOf(mSelectImages.get(0));
            mSelectImages.clear();
            if (index != -1) {
                notifyItemChanged(useCamera ? index + 1 : index);
            }
        }
    }

    public void setSelectedImages(ArrayList<String> selected) {
        if (mImages != null && selected != null) {
            for (String path : selected) {
                if (isFull()) {
                    return;
                }
                for (Image image : mImages) {
                    if (path.equals(image.getPath())) {
                        if (!mSelectImages.contains(image)) {
                            mSelectImages.add(image);
                        }
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }
    }


    private boolean isFull() {
        if (isSingle && mSelectImages.size() == 1) {
            return true;
        } else if (mMaxCount > 0 && mSelectImages.size() == mMaxCount) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<Image> getSelectImages() {
        return mSelectImages;
    }

    public void setOnImageSelectListener(OnImageSelectListener listener) {
        this.mSelectListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;
//        ImageView ivSelectIcon;
        ImageView ivMasking;
        ImageView ivGif;
        ImageView ivVideo;
        ImageView ivCamera;
        TextView ivDuration,tvCheck,tvCheck2;
        LinearLayout videoLl;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            ivMasking = itemView.findViewById(R.id.iv_masking);
            ivGif = itemView.findViewById(R.id.iv_gif);
            ivVideo = itemView.findViewById(R.id.iv_video);
            ivDuration = itemView.findViewById(R.id.iv_duration);
            tvCheck = itemView.findViewById(R.id.tvCheck);
            tvCheck2 = itemView.findViewById(R.id.tvCheck2);
            videoLl = itemView.findViewById(R.id.video_ll);

            ivCamera = itemView.findViewById(R.id.iv_camera);
        }
    }

    public interface OnImageSelectListener {
        void OnImageSelect(Image image, boolean isSelect, int selectCount);
    }

    public interface OnItemClickListener {
        void OnItemClick(Image image, int position);

        void OnCameraClick();
    }
}
