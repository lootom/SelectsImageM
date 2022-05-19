package com.donkingliang.imageselector;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

import com.donkingliang.imageselector.adapter.CenterLayoutManager;
import com.donkingliang.imageselector.adapter.ImagePagerAdapter;
import com.donkingliang.imageselector.adapter.PicPreviewSelectAdapter;
import com.donkingliang.imageselector.entry.Image;
import com.donkingliang.imageselector.entry.MessageBean.ActivityResultBean;
import com.donkingliang.imageselector.imaging.IMGEditActivity;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.donkingliang.imageselector.view.MyViewPager;
import com.shuyu.gsyvideoplayer.GSYVideoManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static android.animation.ObjectAnimator.ofFloat;
import static com.donkingliang.imageselector.ImageSelectorActivity.createCoverUri;

public class PreviewActivity extends AppCompatActivity {

    private RelativeLayout preview_horlist;
    private RecyclerView preview_horlist1;
    private MyViewPager vpImage;
    private TextView tvIndicator;
    private TextView tvConfirm;
    private FrameLayout btnConfirm;
    private TextView tvSelect,ap_clicps;
    private RelativeLayout rlTopBar;
    private RelativeLayout rlBottomBar;

    //tempImages和tempSelectImages用于图片列表数据的页面传输。
    //之所以不要Intent传输这两个图片列表，因为要保证两位页面操作的是同一个列表数据，同时可以避免数据量大时，
    // 用Intent传输发生的错误问题。
    private static ArrayList<Image> tempImages;
    private static ArrayList<Image> tempSelectImages;

    private ArrayList<Image> mImages;
    private ArrayList<Image> mSelectImages;
    private boolean isShowBar = true;
    private boolean isConfirm = false;
    private boolean isSingle;
    private int mMaxCount;

    private PicPreviewSelectAdapter adapter_hor;
    private CenterLayoutManager linearLayoutManager;

    private BitmapDrawable mSelectDrawable;
    private BitmapDrawable mUnSelectDrawable;
    private int currentPosition; // viewPager 当前位置
    private int isSelectList;

    public static void openActivity(Activity activity, ArrayList<Image> images,
                                    ArrayList<Image> selectImages, boolean isSingle,
                                    int maxSelectCount, int position,int isSelectList) {
        tempImages = images;
        tempSelectImages = selectImages;
        Intent intent = new Intent(activity, PreviewActivity.class);
        intent.putExtra(ImageSelector.MAX_SELECT_COUNT, maxSelectCount);
        intent.putExtra(ImageSelector.IS_SINGLE, isSingle);
        intent.putExtra("isSelectList",isSelectList);
        intent.putExtra(ImageSelector.POSITION, position);
        activity.startActivityForResult(intent, ImageSelector.RESULT_CODE);
    }

    private void initBars() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            Window window = getWindow();
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(Color.TRANSPARENT);
//            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            Window window = getWindow();
//            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,  WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#111111"));
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        setStatusBarVisible(true);
        mImages = tempImages;
        tempImages = null;
        mSelectImages = tempSelectImages;
        tempSelectImages = null;

        Intent intent = getIntent();
        mMaxCount = intent.getIntExtra(ImageSelector.MAX_SELECT_COUNT, 0);
        isSingle = intent.getBooleanExtra(ImageSelector.IS_SINGLE, false);
        isSelectList = intent.getIntExtra("isSelectList", 0);
        Resources resources = getResources();
        Bitmap selectBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_image_select);
        mSelectDrawable = new BitmapDrawable(resources, selectBitmap);
        mSelectDrawable.setBounds(0, 0, selectBitmap.getWidth(), selectBitmap.getHeight());

        Bitmap unSelectBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_image_un_select);
        mUnSelectDrawable = new BitmapDrawable(resources, unSelectBitmap);
        mUnSelectDrawable.setBounds(0, 0, unSelectBitmap.getWidth(), unSelectBitmap.getHeight());

        setStatusBarColor();
        initView();
        initListener();
        initViewPager();

        tvIndicator.setText(1 + "/" + mImages.size());
        changeSelect(mImages.get(0));
        posiionts = intent.getIntExtra(ImageSelector.POSITION, 0);
        vpImage.setCurrentItem(intent.getIntExtra(ImageSelector.POSITION, 0));
        EventBus.getDefault().register(this);
        initBars();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void getClips(ActivityResultBean bean){
        if(bean.getRequestCode() == 1 && bean.getResultCode() == 1){
            Image mm ;
            mm = new Image(bean.getPhotoPath(),0,"","");
            Log.v("--->OkHttp", "2s:"+mm.getPath());
            mImages.set(lastPostion,mm);
            mSelectImages.set(lastPostion,mm);
            adapter.notifyDataSetChanged();
            adapter_hor.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    protected int lastPostion;
    private Uri coverUri;
    private int posiionts;
    private void initView() {
        vpImage = (MyViewPager) findViewById(R.id.vp_image);
        tvIndicator = (TextView) findViewById(R.id.tv_indicator);
        tvConfirm = (TextView) findViewById(R.id.tv_confirm);
        btnConfirm = (FrameLayout) findViewById(R.id.btn_confirm);
        tvSelect = (TextView) findViewById(R.id.tv_select);
        ap_clicps = (TextView) findViewById(R.id.ap_clicps);
        rlTopBar = (RelativeLayout) findViewById(R.id.rl_top_bar);
        rlBottomBar = (RelativeLayout) findViewById(R.id.rl_bottom_bar);
        preview_horlist = (RelativeLayout) findViewById(R.id.preview_horlist);
        preview_horlist1 = (RecyclerView) findViewById(R.id.preview_horlist1);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTopBar.getLayoutParams();
        lp.topMargin = getStatusBarHeight(this);
        rlTopBar.setLayoutParams(lp);

        if(isSelectList == 1){
            ap_clicps.setVisibility(View.VISIBLE);
            tvSelect.setVisibility(View.GONE);
            preview_horlist.setVisibility(View.VISIBLE);
            linearLayoutManager = new CenterLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

            preview_horlist1.setLayoutManager(linearLayoutManager);
            adapter_hor = new PicPreviewSelectAdapter(this,mImages);
            preview_horlist1.setAdapter(adapter_hor);

            adapter_hor.setClickPosition(0);
            adapter_hor.setOnPicClickListener(new PicPreviewSelectAdapter.OnPicItemClickListener() {
                @Override
                public void OnPicItemClickListener(int position) {
                    vpImage.setCurrentItem(position, true);
                    if (lastPostion != position) {
                        linearLayoutManager.smoothScrollToPosition(preview_horlist1, new RecyclerView.State(),
                                lastPostion, position);
                        lastPostion = position;
                    }
                }
            });
            linearLayoutManager.smoothScrollToPosition(preview_horlist1, new RecyclerView.State(),
                    0, posiionts);
            lastPostion = posiionts;


            ap_clicps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != coverUri) {
                        coverUri = null;
                    }
                    coverUri = createCoverUri(PreviewActivity.this, ""+System.currentTimeMillis());
                    Intent intent_p = new Intent(PreviewActivity.this, IMGEditActivity.class);
                    try {
                        Uri uri;
//                if (SdkVersionUtils.checkedAndroid_Q()) {
////                       uri =   Uri.parse(images.get(lastPostion).getPath());
//                    uri = Uri.fromFile(new File(PictureFileUtils.getPath(PictureSelectorActivity.this, Uri.parse(images.get(0).getPath()))));
//                } else {
                        uri = Uri.fromFile(new File(mSelectImages.get(lastPostion).getPath()));
//                }
                        intent_p.putExtra(IMGEditActivity.EXTRA_IMAGE_URI, uri);
                        intent_p.putExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH, coverUri.getPath());
                        startActivity(intent_p);
                    } catch (Exception e) {
                        Log.v("===oks",""+e);
                        e.printStackTrace();
                    }
                }
            });

        }else {
            ap_clicps.setVisibility(View.GONE);
            tvSelect.setVisibility(View.VISIBLE);
            preview_horlist.setVisibility(View.GONE);
        }

    }

    private void initListener() {
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isConfirm = true;
                finish();
            }
        });
        tvSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickSelect();
            }
        });
    }

    private  ImagePagerAdapter adapter;
    /**
     * 初始化ViewPager
     */
    private void initViewPager() {
        adapter = new ImagePagerAdapter(this, mImages);
        vpImage.setAdapter(adapter);
        vpImage.setOffscreenPageLimit(0);//关闭预加载
        adapter.setOnItemClickListener(new ImagePagerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, Image image) {
                if (isShowBar){
                    hideBar();
                } else {
                    showBar();
                }
            }
        });
        vpImage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.e("onPageScrolled", "position: "+position+"|| positionOffset"+positionOffset+"|| positionOffsetPixels"+positionOffsetPixels );
            }
            @Override
            public void onPageSelected(int position) {
                Log.e("onPageSelected", "position: "+position );
                tvIndicator.setText(position + 1 + "/" + mImages.size());
                changeSelect(mImages.get(position));
                //记录当前位置
                if (currentPosition!=position){
                    adapter.releaseVideo();
                }
                currentPosition = position;

                if(isSelectList == 1){
                    adapter_hor.setClickPosition(position);
                    if (lastPostion != position) {
                        linearLayoutManager.smoothScrollToPosition(preview_horlist1, new RecyclerView.State(),
                                lastPostion, position);
                        lastPostion = position;
                    }
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                Log.e("PageScrollStateChanged", "state: "+state);
            }
        });
    }

    /**
     * 修改状态栏颜色
     */
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#373c3d"));
        }
    }

    /**
     * 获取状态栏高度
     *
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 显示和隐藏状态栏
     *
     * @param show
     */
    private void setStatusBarVisible(boolean show) {
        if (show) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    /**
     * 显示头部和尾部栏
     */
    private void showBar() {
        isShowBar = true;
        setStatusBarVisible(true);
        //添加延时，保证StatusBar完全显示后再进行动画。
        rlTopBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (rlTopBar != null) {
                    ObjectAnimator animator = ofFloat(rlTopBar, "translationY",
                            rlTopBar.getTranslationY(), 0).setDuration(300);
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            if (rlTopBar != null) {
                                rlTopBar.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    animator.start();
                    ofFloat(rlBottomBar, "translationY", rlBottomBar.getTranslationY(), 0)
                            .setDuration(300).start();
                }
            }
        }, 100);
    }

    /**
     * 隐藏头部和尾部栏
     */
    private void  hideBar() {
        isShowBar = false;
        ObjectAnimator animator = ObjectAnimator.ofFloat(rlTopBar, "translationY",
                0, -rlTopBar.getHeight()).setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (rlTopBar != null) {
                    rlTopBar.setVisibility(View.GONE);
                    //添加延时，保证rlTopBar完全隐藏后再隐藏StatusBar。
                    rlTopBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setStatusBarVisible(false);
                        }
                    }, 5);
                }
            }
        });
        animator.start();
        ofFloat(rlBottomBar, "translationY", 0, rlBottomBar.getHeight())
                .setDuration(300).start();
    }

    private void clickSelect() {
        int position = vpImage.getCurrentItem();
        if (mImages != null && mImages.size() > position) {
            Image image = mImages.get(position);
            if (mSelectImages.contains(image)) {
                mSelectImages.remove(image);
            } else if (isSingle) {
                mSelectImages.clear();
                mSelectImages.add(image);
            } else if (mMaxCount <= 0 || mSelectImages.size() < mMaxCount) {
                mSelectImages.add(image);
            }
            changeSelect(image);
        }
    }

    private void changeSelect(Image image) {

        tvSelect.setText("");
//        tvSelect.setCompoundDrawables(mSelectImages.contains(image) ?
//                mSelectDrawable : mUnSelectDrawable, null, null, null);
        tvSelect.setBackgroundResource(R.drawable.rc_picture_check_normal);
        for (int i = 0; i < mSelectImages.size(); i++) {
            String media = mSelectImages.get(i).getPath();
            if (media.equals(image.getPath())) {
                tvSelect.setText(String.valueOf(i+1));
                tvSelect.setBackgroundResource(R.drawable.rc_picture_check_selected_red);
            }
        }
        setSelectImageCount(mSelectImages.size());
    }

    private void setSelectImageCount(int count) {
        if (count == 0) {
            btnConfirm.setEnabled(false);
            tvConfirm.setText("确定");
        } else {
            btnConfirm.setEnabled(true);
            if (isSingle) {
                tvConfirm.setText("确定");
            } else if (mMaxCount > 0) {
                tvConfirm.setText("确定(" + count + "/" + mMaxCount + ")");
            } else {
                tvConfirm.setText("确定(" + count + ")");
            }
        }
    }
    @Override
    public void finish() {
        //Activity关闭时，通过Intent把用户的操作(确定/返回)传给ImageSelectActivity。
        GSYVideoManager.instance().releaseMediaPlayer();
        Intent intent = new Intent();
        intent.putExtra(ImageSelector.IS_CONFIRM, isConfirm);
        setResult(ImageSelector.RESULT_CODE, intent);
        super.finish();
    }
}
