/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chen.deskclock.alarms;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.chen.deskclock.R;

import java.util.Calendar;

/**
 * DialogFragment that presents a keyboard-entry time picker. The time is typed using the device
 * hardware/number keys: focus advances automatically from hours to minutes (and back when
 * deleting), the backspace key deletes the last digit, the enter key confirms, and the menu key
 * toggles AM/PM. The bottom bar mirrors these actions for touch: AM/PM on the left, OK in the
 * center and delete on the right.
 */
public class TimePickerDialogFragment extends DialogFragment {

    /**
     * Tag for timer picker fragment in FragmentManager.
     */
    private static final String TAG = "TimePickerDialogFragment";

    private static final String ARG_HOUR = TAG + "_hour";
    private static final String ARG_MINUTE = TAG + "_minute";

    private static final int AM = 0;
    private static final int PM = 1;

    private OnTimeSetListener mListener;
    private boolean mIs24Hour;

    // Hour input state (0-2 typed characters).
    private String mHourString = "";
    // Minute input state (0-2 typed characters).
    private String mMinuteString = "";
    // {@code true} while typing affects the minute field, {@code false} for the hour field.
    private boolean mEditingMinute;
    // AM or PM for 12-hour mode.
    private int mAmPm = AM;

    private TextView mHoursView;
    private TextView mMinutesView;
    private TextView mAmPmView;
    private TextView mAmPmButton;

    @Override
    @SuppressWarnings("deprecation")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mListener = (OnTimeSetListener) getParentFragment();

        final Context context = getActivity();
        mIs24Hour = DateFormat.is24HourFormat(context);

        final Calendar now = Calendar.getInstance();
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        final int hour = args.getInt(ARG_HOUR, now.get(Calendar.HOUR_OF_DAY));
        mAmPm = hour >= 12 ? PM : AM;

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View content = inflater.inflate(R.layout.keyboard_time_picker, null);

        mHoursView = content.findViewById(R.id.kb_picker_hours);
        mMinutesView = content.findViewById(R.id.kb_picker_minutes);
        mAmPmView = content.findViewById(R.id.kb_picker_ampm);
        mAmPmButton = content.findViewById(R.id.kb_picker_ampm_button);
        final View okButton = content.findViewById(R.id.kb_picker_ok);
        final ImageButton deleteButton = content.findViewById(R.id.kb_picker_delete);

        if (mIs24Hour) {
            mAmPmView.setVisibility(View.GONE);
            mAmPmButton.setVisibility(View.GONE);
        }

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(content)
                .create();

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm(dialog);
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteLastDigit();
            }
        });
        mAmPmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAmPm();
            }
        });

        dialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    // Consume the matching UP event for keys we handle so they don't propagate.
                    return isHandledKey(keyCode);
                }
                return handleKeyDown(keyCode, dialog);
            }
        });

        updateDisplay();
        return dialog;
    }

    private boolean isHandledKey(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            return true;
        }
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_FORWARD_DEL:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_MENU:
                return true;
            default:
                return false;
        }
    }

    private boolean handleKeyDown(int keyCode, Dialog dialog) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            addDigit(keyCode - KeyEvent.KEYCODE_0);
            return true;
        }
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            addDigit(keyCode - KeyEvent.KEYCODE_NUMPAD_0);
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_FORWARD_DEL:
                deleteLastDigit();
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                confirm(dialog);
                return true;
            case KeyEvent.KEYCODE_MENU:
                toggleAmPm();
                return true;
            default:
                return false;
        }
    }

    /**
     * Adds a typed digit, advancing automatically between the hour and minute fields and ignoring
     * inputs that would produce an invalid time.
     */
    private void addDigit(int digit) {
        if (!mEditingMinute) {
            if (mHourString.isEmpty()) {
                if (mIs24Hour) {
                    // 0,1,2 may begin a two-digit hour; 3-9 are complete single-digit hours.
                    mHourString = String.valueOf(digit);
                    if (digit >= 3) {
                        mEditingMinute = true;
                    }
                } else {
                    // 12-hour: hours are 1-12. Ignore a leading 0.
                    if (digit == 0) {
                        return;
                    }
                    mHourString = String.valueOf(digit);
                    if (digit >= 2) {
                        // 2-9 cannot be the tens digit of a 12-hour value.
                        mEditingMinute = true;
                    }
                }
            } else {
                final int first = Integer.parseInt(mHourString);
                if (mIs24Hour) {
                    if (first == 1 || (first == 2 && digit <= 3) || first == 0) {
                        mHourString = "" + first + digit;
                        mEditingMinute = true;
                    } else {
                        // The first digit was the whole hour; this digit starts the minutes.
                        startMinuteWithDigit(digit);
                    }
                } else {
                    if (first == 1 && digit <= 2) {
                        // 10, 11 or 12.
                        mHourString = "1" + digit;
                        mEditingMinute = true;
                    } else {
                        startMinuteWithDigit(digit);
                    }
                }
            }
        } else {
            if (mMinuteString.length() < 2) {
                if (mMinuteString.isEmpty()) {
                    // First minute digit is the tens place: must be 0-5.
                    if (digit <= 5) {
                        mMinuteString = String.valueOf(digit);
                    }
                } else {
                    mMinuteString = mMinuteString + digit;
                }
            }
        }
        updateDisplay();
    }

    private void startMinuteWithDigit(int digit) {
        mEditingMinute = true;
        if (digit <= 5) {
            mMinuteString = String.valueOf(digit);
        }
    }

    private void deleteLastDigit() {
        if (mEditingMinute) {
            if (!mMinuteString.isEmpty()) {
                mMinuteString = mMinuteString.substring(0, mMinuteString.length() - 1);
            } else {
                // Move focus back to the hour field and delete its last digit.
                mEditingMinute = false;
                if (!mHourString.isEmpty()) {
                    mHourString = mHourString.substring(0, mHourString.length() - 1);
                }
            }
        } else if (!mHourString.isEmpty()) {
            mHourString = mHourString.substring(0, mHourString.length() - 1);
        }
        updateDisplay();
    }

    private void toggleAmPm() {
        if (mIs24Hour) {
            return;
        }
        mAmPm = mAmPm == AM ? PM : AM;
        updateDisplay();
    }

    private void updateDisplay() {
        mHoursView.setText(mHourString.isEmpty() ? "--" : padDisplay(mHourString));
        mMinutesView.setText(mMinuteString.isEmpty() ? "--" : padDisplay(mMinuteString));
        if (!mIs24Hour) {
            mAmPmView.setText(mAmPm == AM ? "AM" : "PM");
            // The button toggles to the other meridiem.
            mAmPmButton.setText(mAmPm == AM ? "PM" : "AM");
        }
    }

    private static String padDisplay(String value) {
        return value.length() == 1 ? "0" + value : value;
    }

    private void confirm(Dialog dialog) {
        if (mHourString.isEmpty()) {
            return;
        }
        int hour = Integer.parseInt(mHourString);
        final int minute = mMinuteString.isEmpty() ? 0 : Integer.parseInt(mMinuteString);

        if (!mIs24Hour) {
            // Convert the 12-hour value to a 24-hour value.
            if (hour == 12) {
                hour = 0;
            }
            if (mAmPm == PM) {
                hour += 12;
            }
        }
        if (hour > 23 || minute > 59) {
            return;
        }
        if (mListener != null) {
            mListener.onTimeSet(this, hour, minute);
        }
        dialog.dismiss();
    }

    public static void show(Fragment fragment) {
        show(fragment, -1 /* hour */, -1 /* minute */);
    }

    public static void show(Fragment parentFragment, int hourOfDay, int minute) {
        if (!(parentFragment instanceof OnTimeSetListener)) {
            throw new IllegalArgumentException("Fragment must implement OnTimeSetListener");
        }

        final FragmentManager manager = parentFragment.getChildFragmentManager();
        if (manager == null || manager.isDestroyed()) {
            return;
        }

        // Make sure the dialog isn't already added.
        removeTimeEditDialog(manager);

        final TimePickerDialogFragment fragment = new TimePickerDialogFragment();

        final Bundle args = new Bundle();
        if (hourOfDay >= 0 && hourOfDay < 24) {
            args.putInt(ARG_HOUR, hourOfDay);
        }
        if (minute >= 0 && minute < 60) {
            args.putInt(ARG_MINUTE, minute);
        }

        fragment.setArguments(args);
        fragment.show(manager, TAG);
    }

    public static void removeTimeEditDialog(FragmentManager manager) {
        if (manager != null) {
            final Fragment prev = manager.findFragmentByTag(TAG);
            if (prev != null) {
                manager.beginTransaction().remove(prev).commit();
            }
        }
    }

    /**
     * The callback interface used to indicate the user is done filling in the time (e.g. they
     * clicked on the 'OK' button).
     */
    public interface OnTimeSetListener {
        /**
         * Called when the user is done setting a new time and the dialog has closed.
         *
         * @param fragment  the fragment associated with this listener
         * @param hourOfDay the hour that was set
         * @param minute    the minute that was set
         */
        void onTimeSet(TimePickerDialogFragment fragment, int hourOfDay, int minute);
    }
}
