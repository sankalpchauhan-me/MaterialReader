package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.Utils.Utility;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, ClickCallback {

    private static final String TAG = ArticleListActivity.class.toString();
    Adapter adapter;
    private CollapsingToolbarLayout mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private int mMutedColor = 0xFF333333;
    private int mDarkColor = 0xFF333333;
    private FloatingActionButton transForm;
    private AnimatedVectorDrawableCompat gridToVertical, verticalToGrid;
    private boolean isGrid;
    private StaggeredGridLayoutManager sglm;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private CoordinatorLayout parentLayout;
    private boolean mIsRefreshing = false;
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (CollapsingToolbarLayout) findViewById(R.id.toolbar);
        Toolbar activityToolbar = (Toolbar) findViewById(R.id.activity_toolbar);
        final View toolbarContainerView = findViewById(R.id.toolbar_container);
        transForm = (FloatingActionButton) findViewById(R.id.transform_fab);
        init();
        setSupportActionBar(activityToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        parentLayout = (CoordinatorLayout) findViewById(R.id.parent_coordinator_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
        transForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsRefreshing) {
                    if (isGrid) {
                        showVertical();
                        sglm.setSpanCount(1);
                        adapter.notifyDataSetChanged();
                    } else {
                        showGrid();
                        sglm.setSpanCount(Utility.calculateNoOfColumns(ArticleListActivity.this));
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    Snackbar.make(parentLayout, getResources().getString(R.string.loading_in_progress), Snackbar.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void init() {
        isGrid = true;
        gridToVertical = AnimatedVectorDrawableCompat.create(this, R.drawable.main_layout_switch);
        verticalToGrid = AnimatedVectorDrawableCompat.create(this, R.drawable.main_layout_switch_two);
        transForm.setImageDrawable(gridToVertical);
    }

    public void showGrid() {
        if (!isGrid) {
            morph();
        }
    }

    public void showVertical() {
        if (isGrid) {
            morph();
        }
    }

    private void morph() {
        AnimatedVectorDrawableCompat drawable;
        if (isGrid) {
            drawable = gridToVertical;
        } else {
            drawable = verticalToGrid;
        }
        transForm.setImageDrawable(drawable);
        drawable.start();
        isGrid = !isGrid;

    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.e(TAG, "I am here 3");
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.e(TAG, "I am here 2");
        boolean isStillLoading = false;
        adapter = new Adapter(cursor, ArticleListActivity.this);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = Utility.calculateNoOfColumns(this);
        sglm = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onItemClick(int position, String transitionName, View sharedView) {
        if (!mIsRefreshing) {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    ItemsContract.Items.buildItemUri(adapter.getItemId(position)));
            i.putExtra("IMAGE_TRANSITION_NAME", ViewCompat.getTransitionName(sharedView));
            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(ArticleListActivity.this, sharedView, transitionName);
            startActivity(i, optionsCompat.toBundle());
        } else {
            //Toast.makeText(ArticleListActivity.this, "Loading", Toast.LENGTH_LONG).show();
            Snackbar.make(parentLayout, getResources().getString(R.string.loading_in_progress), Snackbar.LENGTH_SHORT).show();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public LinearLayout linearLayout;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            linearLayout = (LinearLayout) view.findViewById(R.id.ll_list_item);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final ClickCallback mCallback;
        private Cursor mCursor;


        public Adapter(Cursor cursor, ClickCallback mCallback) {
            mCursor = cursor;
            this.mCallback = mCallback;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            return vh;
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

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader().get(mCursor.getString(ArticleLoader.Query.THUMB_URL), new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                    Bitmap bitmap = imageContainer.getBitmap();
                    if (bitmap != null) {
                        Palette p = Palette.generate(bitmap, 12);
                        mMutedColor = p.getLightMutedColor(0xFF333333);
                        holder.thumbnailView.setBackgroundColor(mMutedColor);
                        holder.linearLayout.setBackgroundColor(mMutedColor);
                        mDarkColor = p.getDarkVibrantColor(0xFF333333);
                        holder.titleView.setTextColor(mDarkColor);
                        float width = bitmap.getWidth();
                        float height = bitmap.getHeight();
                        Log.d("Calculated Aspect ratio", "+" + calculateAspectRatio(width, height));
                        holder.thumbnailView.setAspectRatio(calculateAspectRatio(width, height));

                    }
                }

                @Override
                public void onErrorResponse(VolleyError volleyError) {

                }
            });
            //holder.thumbnailView.setAspectRatio(1);
            //Log.d(TAG, "Aspect Ratio: "+mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallback.onItemClick(holder.getAdapterPosition(), holder.titleView.getText().toString(), holder.thumbnailView);
                }
            });
        }

        /**
         * Calculates Aspect Ratio Of Image
         *
         * @param width
         * @param height
         * @return
         */
        public float calculateAspectRatio(float width, float height) {
            if (width > height) {
                return width / height;
            }
            return height / width;
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }
}
