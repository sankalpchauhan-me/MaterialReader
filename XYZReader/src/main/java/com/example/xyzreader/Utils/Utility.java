package com.example.xyzreader.Utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class Utility {

    public static int calculateNoOfColumns(Context context) {
        /**
         * Dynamically Calculates the number of columns that can fit in the display view
         *
         * ATTRIBUTION calculateNoOfColumns(Context context)
         * https://stackoverflow.com/questions/33575731/gridlayoutmanager-how-to-auto-fit-columns/38472370#38472370
         */
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int scalingFactor = 180;
        return (int) (dpWidth / scalingFactor);
    }
}
