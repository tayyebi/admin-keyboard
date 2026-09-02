package com.admin.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.SparseArray;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws the keyboard as stacked translucent glass instead of the framework's flat key
 * drawables: a frosted sheet with colour blooming under it, and every key a pane with a
 * lit upper rim, a specular sheen and a soft seat beneath it.
 *
 * Painting here rather than through drawable resources is what lets per-key state --
 * pressed, latched modifier -- be expressed as light instead of as a marker glued onto
 * the label, and lets long labels shrink to fit a one-unit key.
 */
public class GlassKeyboardView extends KeyboardView {

    /** Multi-tap keys carry their digit and their letters on separate lines. */
    private static final int T9_FIRST = -309;
    private static final int T9_LAST = -301;
    private static final int CODE_SPACE = 32;

    private static final int TEXT_PRIMARY = 0xFFEEF3FC;
    private static final int TEXT_SECONDARY = 0xFFAFBCD2;
    private static final int TEXT_LIT = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF6E9BFF;

    private static final int PANE = 0;
    private static final int PANE_MUTED = 1;
    private static final int PANE_LATCHED = 2;
    private static final int PANE_PRESSED = 3;
    private static final int RIM = 4;
    private static final int SHEEN = 5;

    /** Top and bottom colour of each vertical gradient, indexed by the constants above. */
    private static final int[][] GRADIENTS = {
            {0x3DFFFFFF, 0x12FFFFFF},
            {0x1FFFFFFF, 0x0AFFFFFF},
            {0x9E7FAEFF, 0x663F6DDB},
            {0xB38FB6FF, 0x7A4A78E0},
            {0x73FFFFFF, 0x0FFFFFFF},
            {0x2EFFFFFF, 0x00FFFFFF},
    };
    private static final int MAX_SPAN = 8192;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics metrics = new Paint.FontMetrics();
    private final GlassBackdrop backdrop = new GlassBackdrop();
    private final SparseArray<Shader> gradients = new SparseArray<Shader>();
    private final RectF key = new RectF();
    private final RectF scratch = new RectF();
    private final Set<Integer> activeCodes = new HashSet<Integer>();

    private final float density;
    private final float gap;
    private final float pressInset;
    private final float maxCorner;
    private final float rimWidth;

    public GlassKeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlassKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        gap = 2.6f * density;
        pressInset = 1.4f * density;
        maxCorner = 14f * density;
        rimWidth = 1.1f * density;

        stroke.setStyle(Paint.Style.STROKE);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setTextAlign(Paint.Align.CENTER);

        backdrop.setCorners(22f * density, 0f);
        backdrop.setRimWidth(1.2f * density);
    }

    /**
     * Latched modifiers, by key code. They are drawn lit rather than relabelled, so a key
     * keeps reading as "Ctrl" whether or not it is held.
     */
    public void setActiveCodes(int... codes) {
        activeCodes.clear();
        for (int code : codes) activeCodes.add(code);
    }

    // Public rather than protected: KeyboardView widens this one, and an override
    // cannot narrow it back.
    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        backdrop.resize(w, h);
        gradients.clear();
    }

    /**
     * The framework repaints only the pressed key's own rectangle, which would clip the
     * glow that spills past its edges. Repainting the whole sheet keeps the lighting
     * continuous, and at this size it costs a handful of gradients.
     */
    @Override
    public void invalidateKey(int keyIndex) {
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        backdrop.draw(canvas);

        Keyboard keyboard = getKeyboard();
        if (keyboard == null) return;

        drawGrabber(canvas);

        float offsetX = getPaddingLeft();
        float offsetY = getPaddingTop();
        for (Keyboard.Key k : keyboard.getKeys()) {
            drawKey(canvas, k, offsetX, offsetY);
        }
    }

    private void drawGrabber(Canvas canvas) {
        float centerY = getPaddingTop() * 0.42f;
        if (centerY < 2f * density) return;
        float halfWidth = 17f * density;
        float thickness = 3.5f * density;
        scratch.set(getWidth() / 2f - halfWidth, centerY - thickness / 2f,
                getWidth() / 2f + halfWidth, centerY + thickness / 2f);
        fill.setShader(null);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0x40FFFFFF);
        canvas.drawRoundRect(scratch, thickness, thickness, fill);
    }

    private void drawKey(Canvas canvas, Keyboard.Key k, float offsetX, float offsetY) {
        key.set(k.x + offsetX, k.y + offsetY,
                k.x + offsetX + k.width, k.y + offsetY + k.height);
        key.inset(gap, gap);
        if (key.width() <= 0f || key.height() <= 0f) return;

        int code = k.codes != null && k.codes.length > 0 ? k.codes[0] : 0;
        boolean pressed = k.pressed;
        boolean latched = activeCodes.contains(code);
        boolean muted = isMuted(code);

        if (pressed) key.inset(pressInset, pressInset);
        float corner = Math.min(maxCorner, Math.min(key.width(), key.height()) * 0.34f);

        drawSeat(canvas, corner);
        if (pressed || latched) drawHalo(canvas, corner, pressed ? 0x59 : 0x3D);

        fill.setStyle(Paint.Style.FILL);
        fill.setShader(gradient(pressed ? PANE_PRESSED
                : latched ? PANE_LATCHED
                : muted ? PANE_MUTED : PANE, key));
        canvas.drawRoundRect(key, corner, corner, fill);

        drawSheen(canvas, corner);
        drawRim(canvas, corner, pressed || latched);
        drawLabel(canvas, k, code, pressed, latched, muted);
    }

    /** A few widening strokes stand in for a blur, which the GPU canvas will not do. */
    private void drawSeat(Canvas canvas, float corner) {
        stroke.setShader(null);
        stroke.setStrokeWidth(density);
        for (int ring = 0; ring < 3; ring++) {
            float grow = (ring + 0.5f) * density;
            scratch.set(key);
            scratch.inset(-grow, -grow);
            scratch.offset(0f, 0.7f * density);
            stroke.setColor(Color.argb(40 - ring * 13, 0, 0, 0));
            canvas.drawRoundRect(scratch, corner + grow, corner + grow, stroke);
        }
    }

    private void drawHalo(Canvas canvas, float corner, int peakAlpha) {
        stroke.setShader(null);
        stroke.setStrokeWidth(1.4f * density);
        for (int ring = 0; ring < 3; ring++) {
            float grow = (ring + 0.6f) * 1.4f * density;
            scratch.set(key);
            scratch.inset(-grow, -grow);
            stroke.setColor(((peakAlpha - ring * 22) << 24) | (ACCENT & 0xFFFFFF));
            canvas.drawRoundRect(scratch, corner + grow, corner + grow, stroke);
        }
    }

    /** The wet highlight across the top of the pane; it fades out before the bottom. */
    private void drawSheen(Canvas canvas, float corner) {
        scratch.set(key);
        scratch.inset(rimWidth, rimWidth);
        scratch.bottom = scratch.top + key.height() * 0.58f;
        fill.setShader(gradient(SHEEN, scratch));
        canvas.drawRoundRect(scratch, corner, corner, fill);
    }

    private void drawRim(Canvas canvas, float corner, boolean lit) {
        scratch.set(key);
        scratch.inset(rimWidth / 2f, rimWidth / 2f);
        stroke.setStrokeWidth(rimWidth);
        if (lit) {
            stroke.setShader(null);
            stroke.setColor(0xB3FFFFFF);
        } else {
            stroke.setShader(gradient(RIM, scratch));
        }
        canvas.drawRoundRect(scratch, corner, corner, stroke);
        stroke.setShader(null);
    }

    private void drawLabel(Canvas canvas, Keyboard.Key k, int code,
                           boolean pressed, boolean latched, boolean muted) {
        if (code == CODE_SPACE && isBlank(k.label)) {
            drawSpaceMark(canvas);
            return;
        }
        if (isBlank(k.label)) return;

        text.setColor(pressed || latched ? TEXT_LIT : muted ? TEXT_SECONDARY : TEXT_PRIMARY);
        String label = k.label.toString();

        if (code >= T9_FIRST && code <= T9_LAST && label.length() > 1) {
            drawMultiTapLabel(canvas, label);
            return;
        }

        text.setTextSize(fitText(label, preferredTextSize(label)));
        text.getFontMetrics(metrics);
        canvas.drawText(label, key.centerX(),
                key.centerY() - (metrics.ascent + metrics.descent) / 2f, text);
    }

    /** Digit large, letter group small underneath, the way a phone keypad reads. */
    private void drawMultiTapLabel(Canvas canvas, String label) {
        String letters = label.substring(0, label.length() - 1);
        String digit = label.substring(label.length() - 1);

        float digitSize = fitText(digit, Math.min(24f * density, key.height() * 0.36f));
        float letterSize = fitText(letters, Math.min(13f * density, key.height() * 0.2f));

        text.setTextSize(digitSize);
        text.getFontMetrics(metrics);
        float digitHeight = -metrics.ascent + metrics.descent;
        float top = key.centerY() - (digitHeight + letterSize * 1.35f) / 2f;
        canvas.drawText(digit, key.centerX(), top - metrics.ascent, text);

        int color = text.getColor();
        text.setColor(dim(color, 0.72f));
        text.setTextSize(letterSize);
        text.getFontMetrics(metrics);
        canvas.drawText(letters, key.centerX(),
                top + digitHeight + letterSize * 0.95f - metrics.descent, text);
        text.setColor(color);
    }

    private void drawSpaceMark(Canvas canvas) {
        float halfWidth = Math.min(key.width() * 0.22f, 26f * density);
        float thickness = 2.4f * density;
        scratch.set(key.centerX() - halfWidth, key.centerY() - thickness / 2f,
                key.centerX() + halfWidth, key.centerY() + thickness / 2f);
        fill.setShader(null);
        fill.setColor(0x66FFFFFF);
        canvas.drawRoundRect(scratch, thickness, thickness, fill);
    }

    private float preferredTextSize(String label) {
        float base = Math.min(key.height() * 0.44f, 19f * density);
        int length = label.length();
        if (length == 1) return base;
        if (length == 2) return base * 0.82f;
        if (length <= 4) return base * 0.66f;
        return base * 0.54f;
    }

    /** Shrinks a label until it clears the pane, so "Pause" fits a one-unit key. */
    private float fitText(String label, float preferred) {
        text.setTextSize(preferred);
        float available = key.width() - 6f * density;
        float measured = text.measureText(label);
        if (measured <= available || measured <= 0f) return preferred;
        return Math.max(7f * density, preferred * available / measured);
    }

    /**
     * A vertical gradient spanning {@code bounds}, cached by the span it covers. Keys
     * share a row's geometry, so a whole keyboard needs only a couple of dozen of these,
     * and none is ever mutated after it is built -- a shader whose local matrix is
     * rewritten between draws can leak that change into calls already recorded.
     */
    private Shader gradient(int style, RectF bounds) {
        int top = Math.round(bounds.top);
        int height = Math.max(1, Math.round(bounds.height()));
        if (top < 0 || top >= MAX_SPAN || height >= MAX_SPAN) {
            return newGradient(style, top, height);
        }
        int cacheKey = (style * MAX_SPAN + height) * MAX_SPAN + top;
        Shader shader = gradients.get(cacheKey);
        if (shader == null) {
            shader = newGradient(style, top, height);
            gradients.put(cacheKey, shader);
        }
        return shader;
    }

    private static Shader newGradient(int style, int top, int height) {
        return new LinearGradient(0f, top, 0f, top + height,
                GRADIENTS[style][0], GRADIENTS[style][1], Shader.TileMode.CLAMP);
    }

    /** Everything that is not a character sits back a step so the letters lead. */
    private static boolean isMuted(int code) {
        return code < 0 && !(code >= T9_FIRST && code <= T9_LAST);
    }

    private static boolean isBlank(CharSequence label) {
        return label == null || label.length() == 0;
    }

    private static int dim(int color, float factor) {
        return (Math.round(Color.alpha(color) * factor) << 24) | (color & 0xFFFFFF);
    }
}
