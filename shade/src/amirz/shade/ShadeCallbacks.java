package amirz.shade;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.graphics.ColorUtils;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.AppInfo;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.plugin.PluginManager;
import com.android.launcher3.plugin.button.ButtonPluginClient;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.util.Themes;
import com.google.android.libraries.gsa.launcherclient.LauncherClient;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

import amirz.shade.allapps.search.AppsSearchContainerLayout;
import amirz.shade.feed.FeedProviders;
import amirz.shade.shadespace.ShadespaceView;

import static amirz.shade.ShadeSettings.*;
import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.NORMAL;

public class ShadeCallbacks
        implements LauncherCallbacks, WallpaperColorInfo.OnChangeListener,
        DeviceProfile.OnDeviceProfileChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener, ButtonPluginClient.Callback {
    private final ShadeLauncher mLauncher;

    private ShadeOverlay mOverlayCallbacks;
    private LauncherClient mLauncherClient;
    private boolean mDeferCallbacks;
    private final Bundle mPrivateOptions = new Bundle();
    private boolean mNoFloatingView;

    private ShadespaceView mShadespace;

    ShadeCallbacks(ShadeLauncher launcher) {
        mLauncher = launcher;
    }

    public void deferCallbacksUntilNextResumeOrStop() {
        mDeferCallbacks = true;
    }

    public LauncherClient getLauncherClient() {
        return mLauncherClient;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = Utilities.getPrefs(mLauncher);
        mOverlayCallbacks = new ShadeOverlay(mLauncher);
        mLauncherClient = new LauncherClient(mLauncher, mOverlayCallbacks, getClientOptions(prefs));
        mOverlayCallbacks.setClient(mLauncherClient);

        mLauncher.addOnDeviceProfileChangeListener(this);
        applyDefaultSharedPreferences(prefs);
        prefs.registerOnSharedPreferenceChangeListener(this);

        WallpaperColorInfo instance = WallpaperColorInfo.getInstance(mLauncher);
        instance.addOnChangeListener(this);
        onExtractedColorsChanged(instance);

        mShadespace = mLauncher.findViewById(R.id.search_container_workspace);
    }

    @Override
    public void onDetachedFromWindow() {
        mLauncherClient.onDetachedFromWindow();
    }

    @Override
    public void onAttachedToWindow() {
        mLauncherClient.onAttachedToWindow();
    }

    @Override
    public void onHomeIntent(boolean internalStateHandled) {
        mLauncherClient.hideOverlay(mLauncher.isStarted() && !mLauncher.isForceInvisible());
        if (mLauncher.hasWindowFocus()
                && mLauncher.isInState(NORMAL)
                && mLauncher.getWorkspace().getNextPage() == 0
                && mNoFloatingView) {
            ButtonPluginClient buttonPluginClient =
                    PluginManager.getInstance(mLauncher).getClient(ButtonPluginClient.class);
            if (!buttonPluginClient.onHomeIntent(this)) {
                AppsSearchContainerLayout search =
                        (AppsSearchContainerLayout) mLauncher.getAppsView().getSearchView();
                search.requestSearch();
                mLauncher.getStateManager().goToState(LauncherState.ALL_APPS, true);
            }
        }
    }

    @Override
    public boolean startActivity(Intent intent) {
        return mLauncher.startActivitySafely(mLauncher.getWorkspace(), intent, null);
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile deviceProfile) {
        mLauncherClient.reattachOverlay();
    }

    @Override
    public void onResume() {
        Handler handler = mLauncher.getDragLayer().getHandler();
        if (mDeferCallbacks) {
            if (handler == null) {
                // Finish defer if we are not attached to window.
                checkIfStillDeferred();
            } else {
                // Wait one frame before checking as we can get multiple resume-pause events
                // in the same frame.
                handler.post(this::checkIfStillDeferred);
            }
        } else {
            mLauncherClient.onResume();
        }
        if (mShadespace != null) {
            mShadespace.onResume();
        }
    }

    @Override
    public void onPause() {
        if (!mDeferCallbacks) {
            mLauncherClient.onPause();
        }
        if (mShadespace != null) {
            mShadespace.onPause();
        }
        mNoFloatingView = AbstractFloatingView.getTopOpenView(mLauncher) == null;
    }

    @Override
    public void onStart() {
        if (!mDeferCallbacks) {
            mLauncherClient.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mDeferCallbacks) {
            checkIfStillDeferred();
        } else {
            mLauncherClient.onStop();
        }
    }

    private void checkIfStillDeferred() {
        if (!mDeferCallbacks) {
            return;
        }
        if (!mLauncher.hasBeenResumed() && mLauncher.isStarted()) {
            return;
        }
        mDeferCallbacks = false;

        // Move the client to the correct state. Calling the same method twice is no-op.
        if (mLauncher.isStarted()) {
            mLauncherClient.onStart();
        }
        if (mLauncher.hasBeenResumed()) {
            mLauncherClient.onResume();
        } else {
            mLauncherClient.onPause();
        }
        if (!mLauncher.isStarted()) {
            mLauncherClient.onStop();
        }
    }

    @Override
    public void onDestroy() {
        WallpaperColorInfo.getInstance(mLauncher).removeOnChangeListener(this);
        Utilities.getPrefs(mLauncher).unregisterOnSharedPreferenceChangeListener(this);
        mLauncherClient.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) { }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) { }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) {
        mLauncherClient.dump(prefix, w);
    }

    @Override
    public boolean handleBackPressed() {
        if (!mLauncher.getDragController().isDragging()) {
            AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(mLauncher);
            if (topView != null && topView.onBackPressed()) {
                // Override base because we do not want to call onBackPressed twice.
                return true;
            } else if (mLauncher.isInState(ALL_APPS)) {
                AppsSearchContainerLayout search =
                        (AppsSearchContainerLayout) mLauncher.getAppsView().getSearchUiManager();
                // Reset the search if it has text in it.
                if (search.getText().length() > 0) {
                    search.searchString("");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onTrimMemory(int level) { }

    @Override
    public void onLauncherProviderChange() { }

    @Override
    public void bindAllApplications(ArrayList<AppInfo> apps) { }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public boolean startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData) {
        return false;
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        int alpha = mLauncher.getResources().getInteger(R.integer.extracted_color_gradient_alpha);

        mPrivateOptions.putInt("background_color_hint",
                primaryColor(wallpaperColorInfo, mLauncher, alpha));
        mPrivateOptions.putInt("background_secondary_color_hint",
                secondaryColor(wallpaperColorInfo, mLauncher, alpha));
        mPrivateOptions.putBoolean("is_background_dark",
                Themes.getAttrBoolean(mLauncher, R.attr.isMainColorDark));

        mLauncherClient.setPrivateOptions(mPrivateOptions);
    }

    private LauncherClient.ClientOptions getClientOptions(SharedPreferences prefs) {
        return new LauncherClient.ClientOptions(
                true,
                true, /* enableHotword */
                true /* enablePrewarming */
        );
    }

    /**
     * Writes the default values of settings that can trigger a reload to disk.
     * This prevents a bug where opening the settings menu for the first time immediately causes
     * a reload of the entire launcher, because the preferences triggering it are updated.
     * @param prefs SharedPreferences object to read from and write to.
     */
    private static void applyDefaultSharedPreferences(SharedPreferences prefs) {
        final String defaultFeed = FeedProviders.DEFAULT;
        prefs.edit().putString(PREF_FEED_PROVIDER, prefs.getString(PREF_FEED_PROVIDER, defaultFeed))
                .putString(PREF_GRID_SIZE, prefs.getString(PREF_GRID_SIZE, ""))
                .putString(PREF_DOCK_SEARCH, prefs.getString(PREF_DOCK_SEARCH, ""))
                .apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(PREF_FEED_PROVIDER)) {
            mLauncherClient.disconnect();
            mLauncher.recreate();
        } else if (key.equals(PREF_GRID_SIZE)) {
            mLauncher.kill();
        } else if (key.startsWith(PluginManager.PREF_PLUGIN_PREFIX)) {
            mLauncher.recreate();
        } else if (key.equals(PREF_DOCK_SEARCH)) {
            mLauncher.recreate();
        }
    }

    private static int primaryColor(WallpaperColorInfo wallpaperColorInfo, Context context, int alpha) {
        return compositeAllApps(ColorUtils.setAlphaComponent(
                wallpaperColorInfo.getMainColor(), alpha), context);
    }

    private static int secondaryColor(WallpaperColorInfo wallpaperColorInfo, Context context, int alpha) {
        return compositeAllApps(ColorUtils.setAlphaComponent(
                wallpaperColorInfo.getSecondaryColor(), alpha), context);
    }

    private static int compositeAllApps(int color, Context context) {
        return ColorUtils.compositeColors(
                Themes.getAttrColor(context, R.attr.allAppsScrimColor), color);
    }
}
