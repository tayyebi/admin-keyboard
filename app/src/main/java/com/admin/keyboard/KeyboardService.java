package com.admin.keyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    public static final String PREFS_NAME = "keyboard_preferences";
    public static final String PREF_LAYOUT = "keyboard_layout";
    public static final String LAYOUT_TKL = "tkl";
    public static final String LAYOUT_T9 = "t9";

    private KeyboardView keyboardView;
    private Keyboard currentKeyboard;
    private Keyboard englishKeyboard;
    private Keyboard persianKeyboard;
    private Keyboard englishT9Keyboard;
    private Keyboard persianT9Keyboard;
    private boolean isPersian = false;
    private boolean isT9 = false;
    private boolean isShifted = false;
    private boolean isCtrl = false;
    private boolean isAlt = false;
    private boolean isFn = false;
    private String languageLabel = "EN";
    private int lastT9Code = 0;
    private int lastT9Index = 0;
    private long lastT9PressTime = 0;

    private static final long T9_REPEAT_TIMEOUT_MS = 800;
    private static final String[] T9_ENGLISH = {
            ".?!", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };
    private static final String[] T9_PERSIAN = {
            "،؟.", "ابپت", "ثجچح", "خدذر", "زژسش", "صضطظ", "عغفق", "کگلم", "نوهی"
    };

    private static final Map<Integer, String> SHIFTED_SYMBOLS = new HashMap<Integer, String>();

    static {
        SHIFTED_SYMBOLS.put((int) '`', "~");
        SHIFTED_SYMBOLS.put((int) '1', "!");
        SHIFTED_SYMBOLS.put((int) '2', "@");
        SHIFTED_SYMBOLS.put((int) '3', "#");
        SHIFTED_SYMBOLS.put((int) '4', "$");
        SHIFTED_SYMBOLS.put((int) '5', "%");
        SHIFTED_SYMBOLS.put((int) '6', "^");
        SHIFTED_SYMBOLS.put((int) '7', "&");
        SHIFTED_SYMBOLS.put((int) '8', "*");
        SHIFTED_SYMBOLS.put((int) '9', "(");
        SHIFTED_SYMBOLS.put((int) '0', ")");
        SHIFTED_SYMBOLS.put((int) '-', "_");
        SHIFTED_SYMBOLS.put((int) '=', "+");
        SHIFTED_SYMBOLS.put((int) '[', "{");
        SHIFTED_SYMBOLS.put((int) ']', "}");
        SHIFTED_SYMBOLS.put((int) '\\', "|");
        SHIFTED_SYMBOLS.put((int) ';', ":");
        SHIFTED_SYMBOLS.put((int) '\'', "\"");
        SHIFTED_SYMBOLS.put((int) ',', "<");
        SHIFTED_SYMBOLS.put((int) '.', ">");
        SHIFTED_SYMBOLS.put((int) '/', "?");
    }

    private static final int KEY_DELETE = -5;
    private static final int KEY_SPACE = 32;
    private static final int KEY_ENTER = -4;
    private static final int KEY_SHIFT = -1;
    private static final int KEY_CTRL = -200;
    private static final int KEY_ALT = -201;
    private static final int KEY_FN = -202;
    private static final int KEY_ESC = -203;
    private static final int KEY_TAB = -204;
    private static final int KEY_LANG = -205;
    private static final int KEY_F1 = -100;
    private static final int KEY_F12 = -111;
    private static final int KEY_CAPS = -50;
    private static final int KEY_LEFT = -51;
    private static final int KEY_RIGHT = -52;
    private static final int KEY_UP = -53;
    private static final int KEY_DOWN = -54;
    private static final int KEY_HOME = -55;
    private static final int KEY_END = -56;
    private static final int KEY_PAGEUP = -57;
    private static final int KEY_PAGEDOWN = -58;
    private static final int KEY_INSERT = -59;
    private static final int KEY_SYSREQ = -60;
    private static final int KEY_SCROLLLOCK = -61;
    private static final int KEY_PAUSE = -62;

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        englishKeyboard = new Keyboard(this, R.xml.keyboard_tkl);
        persianKeyboard = new Keyboard(this, R.xml.keyboard_tkl_fa);
        englishT9Keyboard = new Keyboard(this, R.xml.keyboard_t9);
        persianT9Keyboard = new Keyboard(this, R.xml.keyboard_t9_fa);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isT9 = LAYOUT_T9.equals(preferences.getString(PREF_LAYOUT, LAYOUT_TKL));
        currentKeyboard = getSelectedKeyboard();
        keyboardView.setKeyboard(currentKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(false);
        updateModifierStates();
        return keyboardView;
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        isT9 = LAYOUT_T9.equals(getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_LAYOUT, LAYOUT_TKL));
        if (keyboardView != null) {
            currentKeyboard = getSelectedKeyboard();
            keyboardView.setKeyboard(currentKeyboard);
        }
        isShifted = false;
        isCtrl = false;
        isAlt = false;
        isFn = false;
        updateModifierStates();
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        boolean isT9Character = isT9 && primaryCode <= -301 && primaryCode >= -309;
        if (!isT9Character) lastT9Code = 0;

        switch (primaryCode) {
            case KEY_SHIFT:
                isShifted = !isShifted;
                updateModifierStates();
                break;
            case KEY_CTRL:
                isCtrl = !isCtrl;
                updateModifierStates();
                break;
            case KEY_ALT:
                isAlt = !isAlt;
                updateModifierStates();
                break;
            case KEY_FN:
                isFn = !isFn;
                updateModifierStates();
                break;
            case KEY_CAPS:
                isShifted = !isShifted;
                updateModifierStates();
                break;
            case KEY_DELETE:
                if (isCtrl) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                    sendCtrlKey(KeyEvent.KEYCODE_DEL);
                } else if (isShifted) {
                    ic.deleteSurroundingText(0, 1);
                    releaseShift();
                    clearModifiers();
                } else {
                    ic.deleteSurroundingText(1, 0);
                }
                break;
            case KEY_ENTER:
                if (isCtrl) {
                    sendCtrlKey(KeyEvent.KEYCODE_ENTER);
                } else {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                }
                clearModifiers();
                break;
            case KEY_SPACE:
                ic.commitText(" ", 1);
                clearModifiers();
                break;
            case KEY_ESC:
                sendKey(KeyEvent.KEYCODE_ESCAPE);
                clearModifiers();
                break;
            case KEY_TAB:
                sendKey(KeyEvent.KEYCODE_TAB);
                clearModifiers();
                break;
            case KEY_LEFT:
                sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
                clearModifiers();
                break;
            case KEY_RIGHT:
                sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
                clearModifiers();
                break;
            case KEY_UP:
                sendKey(KeyEvent.KEYCODE_DPAD_UP);
                clearModifiers();
                break;
            case KEY_DOWN:
                sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
                clearModifiers();
                break;
            case KEY_HOME:
                sendKey(KeyEvent.KEYCODE_MOVE_HOME);
                clearModifiers();
                break;
            case KEY_END:
                sendKey(KeyEvent.KEYCODE_MOVE_END);
                clearModifiers();
                break;
            case KEY_PAGEUP:
                sendKey(KeyEvent.KEYCODE_PAGE_UP);
                clearModifiers();
                break;
            case KEY_PAGEDOWN:
                sendKey(KeyEvent.KEYCODE_PAGE_DOWN);
                clearModifiers();
                break;
            case KEY_INSERT:
                sendKey(KeyEvent.KEYCODE_INSERT);
                clearModifiers();
                break;
            case KEY_SYSREQ:
                sendKey(KeyEvent.KEYCODE_SYSRQ);
                clearModifiers();
                break;
            case KEY_SCROLLLOCK:
                sendKey(KeyEvent.KEYCODE_SCROLL_LOCK);
                clearModifiers();
                break;
            case KEY_PAUSE:
                sendKey(KeyEvent.KEYCODE_BREAK);
                clearModifiers();
                break;
            case KEY_LANG:
                switchKeyboardLanguage();
                break;
            default:
                if (primaryCode >= KEY_F12 && primaryCode <= KEY_F1) {
                    sendKey(KeyEvent.KEYCODE_F1 + (primaryCode - KEY_F1));
                    clearModifiers();
                } else if (primaryCode > 0) {
                    String text;
                    if (isShifted) {
                        String shiftedSymbol = SHIFTED_SYMBOLS.get(primaryCode);
                        text = shiftedSymbol != null
                                ? shiftedSymbol
                                : String.valueOf(Character.toUpperCase((char) primaryCode));
                    } else {
                        text = String.valueOf((char) primaryCode);
                    }
                    if (isCtrl) {
                        sendCtrlKey(primaryCode);
                    } else {
                        ic.commitText(text, 1);
                    }
                    releaseShift();
                    clearModifiers();
                } else if (isT9Character) {
                    handleT9Key(primaryCode, ic);
                }
                break;
        }
    }

    private void releaseShift() {
        isShifted = false;
        updateModifierStates();
    }

    private void sendKey(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        int metaState = 0;
        if (isCtrl) metaState |= KeyEvent.META_CTRL_ON;
        if (isAlt) metaState |= KeyEvent.META_ALT_ON;
        if (isShifted) metaState |= KeyEvent.META_SHIFT_ON;

        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, metaState));
        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, metaState));
    }

    private void sendCtrlKey(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_ON));
        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, KeyEvent.META_CTRL_ON));
    }

    private void clearModifiers() {
        isCtrl = false;
        isAlt = false;
        isFn = false;
        updateModifierStates();
    }

    private void updateModifierStates() {
        if (keyboardView == null || currentKeyboard == null) return;
        // Update key labels for modifiers and language key
        List<Keyboard.Key> keys = currentKeyboard.getKeys();
        for (Keyboard.Key key : keys) {
            if (key.codes == null || key.codes.length == 0) continue;
            int code = key.codes[0];
            if (code == KEY_CTRL) {
                key.label = isCtrl ? "Ctrl*" : "Ctrl";
            } else if (code == KEY_ALT) {
                key.label = isAlt ? "Alt*" : "Alt";
            } else if (code == KEY_FN) {
                key.label = isFn ? "Fn*" : "Fn";
            } else if (code == KEY_LANG) {
                key.label = languageLabel;
            }
        }
        keyboardView.invalidateAllKeys();
    }

    private void switchKeyboardLanguage() {
        isPersian = !isPersian;
        currentKeyboard = getSelectedKeyboard();
        languageLabel = isPersian ? "FA" : "EN";
        keyboardView.setKeyboard(currentKeyboard);
        clearModifiers();
        updateModifierStates();
    }

    private Keyboard getSelectedKeyboard() {
        if (isT9) return isPersian ? persianT9Keyboard : englishT9Keyboard;
        return isPersian ? persianKeyboard : englishKeyboard;
    }

    private void handleT9Key(int primaryCode, InputConnection ic) {
        String[] groups = isPersian ? T9_PERSIAN : T9_ENGLISH;
        int groupIndex = -301 - primaryCode;
        String group = groups[groupIndex];
        long now = SystemClock.uptimeMillis();
        if (primaryCode == lastT9Code && now - lastT9PressTime < T9_REPEAT_TIMEOUT_MS) {
            ic.deleteSurroundingText(1, 0);
            lastT9Index = (lastT9Index + 1) % group.length();
        } else {
            lastT9Index = 0;
        }
        char character = group.charAt(lastT9Index);
        String text = String.valueOf(isShifted && !isPersian
                ? Character.toUpperCase(character) : character);
        ic.commitText(text, 1);
        lastT9Code = primaryCode;
        lastT9PressTime = now;
    }

    @Override
    public void onPress(int primaryCode) {
        if (keyboardView != null) {
            keyboardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }
}
