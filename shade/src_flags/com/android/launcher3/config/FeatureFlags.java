/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.config;

/**
 * Defines a set of flags used to control various launcher behaviors
 */
public final class FeatureFlags extends BaseFlags {

    private FeatureFlags() {}

    // Feature flag to enable moving the QSB on the 0th screen of the workspace.
    public static boolean QSB_ON_FIRST_SCREEN;

    // When enabled, all legacy icons are wrapped in an adaptive icon.
    public static final boolean FORCE_ADAPTIVE_ICONS = false;

    // When enabled, the workspace will fade in portrait mode while swiping.
    public static final boolean FADE_WORKSPACE = true;

}
