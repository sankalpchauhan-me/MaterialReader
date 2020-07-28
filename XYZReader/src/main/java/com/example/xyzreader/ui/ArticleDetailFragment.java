package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.BodyAdapter;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "ArticleDetailFragment";
    private static final float PARALLAX_FACTOR = 1.25f;
    AnimatedVectorDrawableCompat shareToBack, backToShare;
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private NestedScrollView mScrollView;
    private ColorDrawable mStatusBarColorDrawable;
    private int mTopInset;
    private View mPhotoContainerView;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;
    private RecyclerView bodyView;
    private TextView bodyText;
    private BodyAdapter bodyAdapter;
    private Context context;
    private List<String> stringList = new ArrayList<>();
    private AppBarLayout appBarLayout;
    private boolean showingShare = false;
    private ImageButton fab;
    private Parcelable layoutManagerSavedState;
    private Toolbar mToolbar;


    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        bodyAdapter = new BodyAdapter();

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        init();
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        CoordinatorLayout mDrawInsetsFrameLayout = (CoordinatorLayout)
                mRootView.findViewById(R.id.draw_insets_frame_layout);
//        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
//            @Override
//            public void onInsetsChanged(Rect insets) {
//                mTopInset = insets.top;
//            }
//        });

//        mScrollView = (NestedScrollView) mRootView.findViewById(R.id.scrollview);
//        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
//            @Override
//            public void onScrollChanged() {
//                mScrollY = mScrollView.getScrollY();
//                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
//                mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
//                updateStatusBar();
//            }
//        });
        if (savedInstanceState != null) {
            layoutManagerSavedState = savedInstanceState.getParcelable("SAVED_STATE");
        }

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mToolbar = (Toolbar) mRootView.findViewById(R.id.activity_toolbar);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
//        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);
        mPhotoView.setTransitionName(((ArticleDetailActivity) getActivity()).getTransitionName());
//        setSharedElementEnterTransition(TransitionInflater.from(context).inflateTransition(android.R.transition.move));
//        setSharedElementReturnTransition(null);

        mStatusBarColorDrawable = new ColorDrawable(0);

        fab = (ImageButton) mRootView.findViewById(R.id.share_fab);
        fab.setImageDrawable(backToShare);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });
        //bindViews();
        updateStatusBar();
        return mRootView;
    }

    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
//        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        bodyView = (RecyclerView) mRootView.findViewById(R.id.content_holder_rv);
        bodyText = (TextView) mRootView.findViewById(R.id.article_body);
        appBarLayout = (AppBarLayout) mRootView.findViewById(R.id.app_bar);


        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    isShow = true;
                    //transhelp.zoomOut(200,0,findViewById(R.id.fab));
                    //fab.setVisibility(View.INVISIBLE);
                    showBack();
                    if (getActivity() instanceof ArticleDetailActivity) {
                        ((ArticleDetailActivity) getActivity()).hideUpButton();
                    }


                } else if (isShow) {
                    isShow = false;
                    //showingShare = true;
                    //transhelp.zoomIn(200, 0, findViewById(R.id.fab));
                    //fab.setVisibility(View.VISIBLE);
                    //showingShare=true;
                    showShare();
                    if (getActivity() instanceof ArticleDetailActivity) {
                        ((ArticleDetailActivity) getActivity()).showUpButton();
                    }
                }
            }
        });
        if (mCursor != null) {
            String original = (Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")).toString());
//            splitToNChar(original, 15000);
//            totalSections(original, 15000);
            stringList.addAll(splitToNChar(original, 15000));
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }


            /**
             * If the text is very large (say larger than 10000 characters),
             * I broke it into sections of 15000 characters & loaded it in recyclerview
             * (This improved the performance greatly, thereby improving User Experience)
             */
//            Spanned s = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />"));
//            String original = s.toString();
//            List<String> =
            //Log.d(TAG,"Sections Required:"+ maxSectionsRequired(original.length(), 10000));
            if (original.length() > 10000) {
                bodyText.setVisibility(View.GONE);
                bodyView.setVisibility(View.VISIBLE);
                //Log.d(TAG, "Correction Check: "+stringList.get(0));
                bodyView.setLayoutManager(new LinearLayoutManager(context));
                bodyView.setHasFixedSize(true);
                bodyView.setAdapter(bodyAdapter);
                bodyView.setItemAnimator(new DefaultItemAnimator());
                bodyView.setNestedScrollingEnabled(true);
                bodyAdapter.setViewData(stringList);
                Log.d(TAG, "Adapter Item Count" + bodyAdapter.getItemCount());
                bodyAdapter.notifyDataSetChanged();
                restoreLayoutManagerPosition();

            } else {
                bodyText.setText(original);
            }

            Log.d(TAG, "Actual Length of Text" + original.length());
            /**
             * Performance is greatly impacted by loading large texts in TextView
             * TODO: Convert TextView to RecyclerView (DONE)
             */
            //bodyView.setText(s.toString().substring(0, 15000));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                mRootView.findViewById(R.id.meta_bar)
                                        .setBackgroundColor(mMutedColor);
                                mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                    @Override
                                    public boolean onPreDraw() {
                                        mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                                        getActivity().startPostponedEnterTransition();
                                        return true;
                                    }
                                });
                                updateStatusBar();
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });

            //bodyAdapter.setViewData(stringList);
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyText.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        //Log.d(TAG, "Bool Test" + mCursor.moveToFirst());
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        //bindViews();
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }

    private List<String> splitToNChar(String text, int size) {
        List<String> tokens = Lists.newArrayList(Splitter.fixedLength(size).split(text));
        Log.d(TAG, "Size Of Split" + tokens.size());
        return tokens;
    }

//    private int totalSections(String s, int length){
//        int i = s.length()/length;
//        Log.d(TAG, "Sections I am calculating " + i+1);
//        return i+1;
//    }

    public void init() {
        showingShare = true;
        shareToBack = AnimatedVectorDrawableCompat.create(context, R.drawable.avd_anim);
        backToShare = AnimatedVectorDrawableCompat.create(context, R.drawable.avd_anim2);
        //fab.setImageDrawable(backToShare);
    }

    public void showShare() {
        if (!showingShare) {
            morph();
        }
    }

    public void showBack() {
        if (showingShare) {
            morph();
        }
    }

    private void morph() {
        AnimatedVectorDrawableCompat drawable;
        if (showingShare) {
            drawable = shareToBack;
        } else {
            drawable = backToShare;
        }
        fab.setImageDrawable(drawable);
        if (drawable != null) {
            drawable.start();
        }
        showingShare = !showingShare;

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (showingShare) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                            .setType("text/plain")
                            .setText("Some sample text")
                            .getIntent(), getString(R.string.action_share)));
                } else {
                    ((ArticleDetailActivity) getActivity()).onBackPressed();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bodyView != null) {
            outState.putParcelable("SAVED_STATE", bodyView.getLayoutManager().onSaveInstanceState());
        }
    }

    private void restoreLayoutManagerPosition() {
        if (layoutManagerSavedState != null) {
            bodyView.getLayoutManager().onRestoreInstanceState(layoutManagerSavedState);
        }
    }
}
