/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chen.deskclock;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.ColorInt;

/**
 * Resolves Material You (Material 3) wallpaper-based dynamic colors at runtime.
 *
 * <p>The system dynamic color palette ({@code @android:color/system_*}) only exists on
 * Android 12 (API 31) and above and is not part of the SDK this project compiles against.
 * To remain build-safe with the existing toolchain, the colors are resolved by name at
 * runtime via {@link android.content.res.Resources#getIdentifier} and guarded by an SDK
 * check. On older devices the caller's static fallback color is returned, so the app keeps
 * its baseline Material 3 palette defined in {@code res/values} and {@code res/values-night}.
 */
public final class DynamicColors {

    private static final int VERSION_CODES_S = 31;

    private DynamicColors() {
    }

    /** @return {@code true} when wallpaper-based dynamic color is available on this device. */
    public static boolean isAvailable() {
        return Build.VERSION.SDK_INT >= VERSION_CODES_S;
    }

    private static boolean isNightMode(Context context) {
        final int mode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Resolves a platform dynamic color resource (e.g. {@code system_accent1_600}) by name.
     *
     * @param fallback color returned when dynamic color is unavailable or cannot be resolved
     */
    @ColorInt
    public static int getSystemColor(Context context, String name, @ColorInt int fallback) {
        if (!isAvailable()) {
            return fallback;
        }
        try {
            final int id = context.getResources().getIdentifier(name, "color", "android");
            if (id != 0) {
                return context.getColor(id);
            }
        } catch (Exception ignored) {
            // Fall through to the supplied fallback.
        }
        return fallback;
    }

    /** @return the dynamic accent color, adapting to light/dark mode. */
    @ColorInt
    public static int getAccentColor(Context context) {
        final String name = isNightMode(context) ? "system_accent1_200" : "system_accent1_600";
        return getSystemColor(context, name, context.getResources().getColor(R.color.md_accent));
    }

    /** @return the dynamic window/background color, adapting to light/dark mode. */
    @ColorInt
    public static int getBackgroundColor(Context context) {
        final String name = isNightMode(context) ? "system_neutral1_900" : "system_neutral1_10";
        return getSystemColor(context, name,
                context.getResources().getColor(R.color.md_background));
    }

    /** @return the dynamic surface color, adapting to light/dark mode. */
    @ColorInt
    public static int getSurfaceColor(Context context) {
        final String name = isNightMode(context) ? "system_neutral1_800" : "system_neutral1_50";
        return getSystemColor(context, name, context.getResources().getColor(R.color.md_surface));
    }

    /** @return a soft, low-emphasis tint used for focus/selection highlights. */
    @ColorInt
    public static int getFocusHighlightColor(Context context) {
        final String name = isNightMode(context) ? "system_accent2_700" : "system_accent2_100";
        return getSystemColor(context, name,
                context.getResources().getColor(R.color.md_focus_highlight));
    }
}
