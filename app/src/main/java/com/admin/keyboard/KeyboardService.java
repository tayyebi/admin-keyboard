package com.admin.keyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.IBinder;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard currentKeyboard;
    private Keyboard qwertyKeyboard;
    private boolean isShifted = false;
    private boolean isCtrl = false;
    private boolean isAlt = false;
    private boolean isFn = false;
    private String languageLabel = "\uD83C\uDF10";

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
        qwertyKeyboard = new Keyboard(this, R.xml.keyboard_tkl);
        keyboardView.setKeyboard(qwertyKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(false);
        updateLanguageLabel();
        return keyboardView;
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
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
                switchNextInputLanguage();
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
                }
                break;
        }
    }

    private void releaseShift() {
        isShifted = false;
        if (keyboardView != null) {
            keyboardView.setShifted(false);
        }
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
        if (keyboardView == null || qwertyKeyboard == null) return;
        keyboardView.setShifted(isShifted);
        // Update key labels for modifiers and language key
        List<Keyboard.Key> keys = qwertyKeyboard.getKeys();
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

    private void switchNextInputLanguage() {
        boolean switched = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switched = switchToNextInputMethod(false);
        } else if (keyboardView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            IBinder token = keyboardView.getApplicationWindowToken();
            if (imm != null && token != null) {
                switched = imm.switchToNextInputMethod(token, false);
            }
        }
        if (!switched) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
        }
        clearModifiers();
    }

    private void updateLanguageLabel() {
        String label = "\uD83C\uDF10";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                InputMethodSubtype subtype = imm != null ? imm.getCurrentInputMethodSubtype() : null;
                if (subtype != null) {
                    Locale locale = null;
                    String tag = subtype.getLanguageTag();
                    if (tag != null && !tag.isEmpty()) {
                        locale = Locale.forLanguageTag(tag);
                    }
                    if (locale == null) {
                        String loc = subtype.getLocale();
                        if (loc != null && !loc.isEmpty()) {
                            locale = new Locale(loc);
                        }
                    }
                    if (locale != null && !locale.getLanguage().isEmpty()) {
                        label = locale.getLanguage().toUpperCase(locale);
                    }
                }
            } catch (Exception e) {
                // keep fallback label
            }
        }
        languageLabel = label;
        updateModifierStates();
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
