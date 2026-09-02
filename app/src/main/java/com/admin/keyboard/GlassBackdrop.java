package com.admin.keyboard;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * The frosted sheet every other layer sits on: a dark translucent slab with soft colour
 * pooling underneath it, so the glass drawn on top has something to refract. Shared by
 * the keyboard surface and the settings screen.
 *
 * Everything is a plain framework shader -- no image assets and no libraries -- so the
 * whole surface is a handful of gradients the GPU rasterises directly.
 */
final class GlassBackdrop {

    private static final int SHEET_TOP = 0xD9151D30;
    private static final int SHEET_BOTTOM = 0xF0070A11;

    /** Centre x, centre y and radius of each colour bloom, as fractions of the sheet. */
    private static final float[][] BLOOM = {
            {0.16f, 0.02f, 0.78f},
            {0.88f, 0.26f, 0.70f},
            {0.46f, 1.06f, 0.85f},
    };
    private static final int[] BLOOM_COLOR = {0xFF5B8CFF, 0xFFB265FF, 0xFF2ED2C6};
    private static final int[] BLOOM_ALPHA = {104, 84, 62};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline = new Path();
    private final Path rimPath = new Path();
    private final RectF rect = new RectF();
    private final Shader[] blooms = new Shader[BLOOM.length];
    private final float[] corners = new float[8];

    private Shader sheet;
    private Shader rim;
    private float topRadius;
    private float bottomRadius;
    private float rimWidth;
    private int width;
    private int height;

    void setCorners(float top, float bottom) {
        topRadius = top;
        bottomRadius = bottom;
        buildPaths();
    }

    void setRimWidth(float rimWidth) {
        this.rimWidth = rimWidth;
        buildPaths();
    }

    void resize(int width, int height) {
        if (width == this.width && height == this.height) return;
        this.width = width;
        this.height = height;
        if (width <= 0 || height <= 0) return;

        sheet = new LinearGradient(0f, 0f, 0f, height, SHEET_TOP, SHEET_BOTTOM,
                Shader.TileMode.CLAMP);
        for (int i = 0; i < BLOOM.length; i++) {
            int hue = BLOOM_COLOR[i] & 0xFFFFFF;
            blooms[i] = new RadialGradient(BLOOM[i][0] * width, BLOOM[i][1] * height,
                    Math.max(1f, BLOOM[i][2] * width),
                    (BLOOM_ALPHA[i] << 24) | hue, hue, Shader.TileMode.CLAMP);
        }
        // The lit rim fades out well before the bottom edge, the way light catches only
        // the upper lip of a real pane.
        rim = new LinearGradient(0f, 0f, 0f, Math.max(1f, height * 0.5f),
                0x70FFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP);
        buildPaths();
    }

    void draw(Canvas canvas) {
        if (width <= 0 || height <= 0 || sheet == null) return;

        int save = canvas.save();
        canvas.clipPath(outline);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(sheet);
        canvas.drawRect(0f, 0f, width, height, paint);
        for (Shader bloom : blooms) {
            paint.setShader(bloom);
            canvas.drawRect(0f, 0f, width, height, paint);
        }
        canvas.restoreToCount(save);

        if (rimWidth > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(rimWidth);
            paint.setShader(rim);
            canvas.drawPath(rimPath, paint);
        }
        paint.setShader(null);
    }

    private void buildPaths() {
        if (width <= 0 || height <= 0) return;

        outline.reset();
        rect.set(0f, 0f, width, height);
        setCornerRadii(topRadius, bottomRadius);
        outline.addRoundRect(rect, corners, Path.Direction.CW);

        rimPath.reset();
        float inset = rimWidth / 2f;
        rect.set(inset, inset, width - inset, height - inset);
        setCornerRadii(Math.max(0f, topRadius - inset), Math.max(0f, bottomRadius - inset));
        rimPath.addRoundRect(rect, corners, Path.Direction.CW);
    }

    private void setCornerRadii(float top, float bottom) {
        corners[0] = corners[1] = corners[2] = corners[3] = top;
        corners[4] = corners[5] = corners[6] = corners[7] = bottom;
    }
}
