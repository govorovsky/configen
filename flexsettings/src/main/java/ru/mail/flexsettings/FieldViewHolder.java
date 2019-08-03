package ru.mail.flexsettings;

import android.graphics.Color;
import android.view.View;
import androidx.annotation.ColorInt;

class FieldViewHolder {
    private final View mView;
    private final View.OnClickListener mListener;
    @ColorInt
    private final int mColor;

    public FieldViewHolder(View view, int color, View.OnClickListener listener) {
        mView = view;
        mListener = listener;
        mColor = color;
    }

    public FieldViewHolder(View view, View.OnClickListener listener) {
        mView = view;
        mListener = listener;
        mColor = Color.WHITE;
    }

    public View getView() {
        return mView;
    }

    public View.OnClickListener getListener() {
        return mListener;
    }

    @ColorInt
    public int getColor() {
        return mColor;
    }
}
