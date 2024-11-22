package org.devio.rn.splashscreen;

import android.animation.Animator;
import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.StyleRes;
import com.airbnb.lottie.LottieAnimationView;
import java.lang.ref.WeakReference;

/**
 * SplashScreen
 * Manages the splash screen with optional Lottie animation support and fallback mechanisms.
 */
public class SplashScreen {
    private static final String TAG = "SplashScreen";

    private static Dialog mSplashDialog;
    private static WeakReference<Activity> mActivityRef;
    private static boolean isAnimationFinished = false;
    private static boolean isWaitingToHide = false;

    /**
     * Show the splash screen with a custom theme and optional Lottie animation.
     *
     * @param activity   The Activity where the splash screen is displayed.
     * @param themeResId The theme resource ID for the Dialog.
     * @param lottieId   The LottieAnimationView resource ID.
     * @param fallbackId The ImageView or ProgressBar resource ID for fallback.
     */
    public static synchronized void show(final Activity activity, @StyleRes final int themeResId, final int lottieId, final int fallbackId) {
        if (activity == null) {
            Log.w(TAG, "Activity is null. Cannot show splash screen.");
            return;
        }

        mActivityRef = new WeakReference<>(activity);

        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || isActivityDestroyed(activity)) {
                Log.w(TAG, "Activity is not valid. Cannot show splash screen.");
                return;
            }

            mSplashDialog = new Dialog(activity, themeResId);
            mSplashDialog.setContentView(R.layout.launch_screen);
            mSplashDialog.setCancelable(false);

            boolean lottieInitialized = initializeLottie(mSplashDialog, lottieId);

            // If Lottie is not initialized, fallback to an alternative.
            if (!lottieInitialized) {
                initializeFallback(mSplashDialog, fallbackId);
            }

            if (!mSplashDialog.isShowing()) {
                mSplashDialog.show();
                Log.d(TAG, "Splash screen displayed.");
            }
        });
    }

    /**
     * Convenience method to show the splash screen with a default theme.
     *
     * @param activity   The Activity where the splash screen is displayed.
     * @param lottieId   The LottieAnimationView resource ID.
     * @param fallbackId The ImageView or ProgressBar resource ID for fallback.
     */
    public static void show(final Activity activity, final int lottieId, final int fallbackId) {
        show(activity, R.style.SplashScreen_SplashTheme, lottieId, fallbackId);
    }

    /**
     * Attempt to initialize Lottie animation. Returns false if Lottie is unavailable.
     *
     * @param dialog    The Dialog containing the Lottie view.
     * @param lottieId  The resource ID of the LottieAnimationView.
     * @return True if Lottie was successfully initialized, false otherwise.
     */
    private static boolean initializeLottie(Dialog dialog, int lottieId) {
        try {
            LottieAnimationView lottieView = dialog.findViewById(lottieId);
            if (lottieView != null) {
                lottieView.addAnimatorListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        Log.d(TAG, "Lottie animation started.");
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        Log.d(TAG, "Lottie animation ended.");
                        setAnimationFinished(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        Log.d(TAG, "Lottie animation canceled.");
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        Log.d(TAG, "Lottie animation repeated.");
                    }
                });
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Lottie: " + e.getMessage());
        }
        return false;
    }

    /**
     * Initialize a fallback for devices without Lottie support.
     *
     * @param dialog     The Dialog containing the fallback view.
     * @param fallbackId The resource ID of the fallback view.
     */
    private static void initializeFallback(Dialog dialog, int fallbackId) {
        try {
            // Attempt to set a fallback view (e.g., ProgressBar or ImageView).
            ImageView fallbackView = dialog.findViewById(fallbackId);
            if (fallbackView != null) {
                fallbackView.setVisibility(ImageView.VISIBLE);
                Log.d(TAG, "Fallback image displayed.");
                return;
            }

            ProgressBar fallbackProgressBar = dialog.findViewById(fallbackId);
            if (fallbackProgressBar != null) {
                fallbackProgressBar.setVisibility(ProgressBar.VISIBLE);
                Log.d(TAG, "Fallback ProgressBar displayed.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing fallback: " + e.getMessage());
        }
    }

    /**
     * Mark the animation as finished and attempt to dismiss the splash screen if waiting.
     *
     * @param flag True if the animation has finished.
     */
    private static synchronized void setAnimationFinished(boolean flag) {
        isAnimationFinished = flag;
        tryDismiss();
    }

    /**
     * Hide the splash screen, dismissing the Dialog if conditions are met.
     *
     * @param activity The Activity where the splash screen is displayed.
     */
    public static synchronized void hide(Activity activity) {
        if (activity == null) {
            if (mActivityRef == null) {
                Log.w(TAG, "Activity reference is null. Cannot hide splash screen.");
                return;
            }
            activity = mActivityRef.get();
        }

        if (activity == null) {
            Log.w(TAG, "Activity is null. Cannot hide splash screen.");
            return;
        }

        isWaitingToHide = true;
        tryDismiss();
    }

    /**
     * Attempt to dismiss the splash screen based on the current state.
     */
    private static void tryDismiss() {
        if (mSplashDialog == null || !mSplashDialog.isShowing()) {
            return;
        }

        final Activity activity = mActivityRef != null ? mActivityRef.get() : null;
        if (activity == null || activity.isFinishing() || isActivityDestroyed(activity)) {
            Log.w(TAG, "Activity is not valid. Cannot dismiss splash screen.");
            return;
        }

        if (isAnimationFinished && isWaitingToHide) {
            activity.runOnUiThread(() -> {
                if (mSplashDialog != null && mSplashDialog.isShowing()) {
                    mSplashDialog.dismiss();
                    mSplashDialog = null;
                    Log.d(TAG, "Splash screen dismissed.");
                }
            });
        }
    }

    /**
     * Check if the activity is destroyed (for API >= 17).
     *
     * @param activity The Activity to check.
     * @return True if the activity is destroyed, false otherwise.
     */
    private static boolean isActivityDestroyed(Activity activity) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed();
    }
}
