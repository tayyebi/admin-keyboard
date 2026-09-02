package com.admin.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/** The settings screen's backdrop: the same frosted sheet the keyboard is painted on. */
public class GlassBackdropView extends View {

    private final GlassBackdrop backdrop = new GlassBackdrop();

    public GlassBackdropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        backdrop.setCorners(0f, 0f);
        backdrop.setRimWidth(0f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        backdrop.resize(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        backdrop.draw(canvas);
    }
}
