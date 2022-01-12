package com.example.android.kiosk;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.android.kiosk.utils.RuntimeUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;

/**
 * Once you have this app on your device, go to the command line and run:
 * <p>
 * $ adb shell dpm set-device-owner com.example.android.kiosk/.AdminReceiver
 * <p>
 * Which should give you something like:
 * <p>
 * Success: Device owner set to package com.example.android.kiosk
 * Active admin set to component {com.example.android.kiosk/com.example.android.kiosk.AdminReceiver}
 * <p>
 * From here you’re home free… The API calls to setLockTaskPackages, startLockTask, and stopLockTask are all you need to enter and exit kiosk mode
 * (setLockTaskPackages allows you to pin the screen without asking for user confirmation - but only works when you’re the device owner for the app).
 */
public class MainActivity extends Activity {
    private final String TAG = getClass().getSimpleName();

    public Button mButton;

    public void toggleKioskMode(View view) {
        enableKioskMode(!mIsKioskEnabled);
    }

    public void toggleApplicationHidden(View view) {
        setApplicationHidden("com.freeme.camera");
    }

    public void setDeviceOwnerApp(View view) {
        String result2 = RuntimeUtil.runCmd(/*adb shell */"dpm set-device-owner com.example.android.kiosk/.AdminReceiver");
        Toast.makeText(this, result2, Toast.LENGTH_LONG).show();
    }

    public void removeActiveAdmin(View view) {
        String result  = RuntimeUtil.runCmd(/*adb shell */"dpm remove-active-admin com.example.android.kiosk/.AdminReceiver");
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    public void checkForDeviceOwnerApp(View view) {
        if (mDpm.isDeviceOwnerApp(getPackageName())) {
            mDpm.setLockTaskPackages(deviceAdmin, new String[]{getPackageName()});
            Toast.makeText(this, getString(R.string.device_owner), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.not_device_owner), Toast.LENGTH_SHORT).show();
        }
    }

    public void checkForAdminActive(View view) {
        if (!mDpm.isAdminActive(deviceAdmin)) {
            Toast.makeText(this, getString(R.string.not_device_admin), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.device_admin), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Notice that once the Device Owner application is set, it cannot be unset with the dpm command.
     * You’ll need to programmatically use the DevicePolicyManager.clearDeviceOwnerApp() method or factory reset your device.
     * <p>
     * Try to delete the app, and if you still run into trouble, go to Settings -> Security -> Device Administrators and uncheck the Kiosk app as a device administrator, then re-try uninstalling.
     */
    public void clearDeviceOwnerApp(View view) {
        try {
            mDpm.clearDeviceOwnerApp("com.example.android.kiosk");
            Toast.makeText(this, getString(R.string.not_device_owner), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "clearDeviceOwner can only be called by the device owner", Toast.LENGTH_SHORT).show();
        }
    }

    // 激活设备管理器
    public void enableDeviceManager(View view) {
        // 判断是否激活  如果没有就启动激活设备
        if (!mDpm.isAdminActive(deviceAdmin)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);

            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "激活设备管理器");
            startActivity(intent);
        } else {
            Toast.makeText(getBaseContext(), "设备已经激活，请勿重复激活", Toast.LENGTH_SHORT).show();
        }
    }

    // 取消激活设备管理器
    public void disableDeviceManager(View view) {
        if (mDpm.isDeviceOwnerApp(getPackageName())) {
            Toast.makeText(getBaseContext(), "Device owner app can't remove active admin.", Toast.LENGTH_SHORT).show();
        } else {
            if (mDpm.isAdminActive(deviceAdmin)) {
                mDpm.removeActiveAdmin(deviceAdmin);
                Toast.makeText(getBaseContext(), "Had been removed active admin", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), "Not the active admin", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private View mDecorView;
    private DevicePolicyManager mDpm;
    ComponentName deviceAdmin;
    private boolean mIsKioskEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.button_toggle_kiosk);

        mDecorView = getWindow().getDecorView();

        deviceAdmin = new ComponentName(this, AdminReceiver.class);
        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        new RxPermissions(this).request(
                "android.permission.MANAGE_DEVICE_ADMINS"
        ).subscribe(granted -> {
            Toast.makeText(this, (granted ? "" : "NOT ") + "has permission: android.permission.MANAGE_DEVICE_ADMINS", Toast.LENGTH_LONG).show();
        });
    }

    /**
     * To add a little bit of UI cleanliness to the mix, here is the code that puts you into immersive mode,
     * so that the top and bottom bars are hidden by default, and show up when the top/bottom of the screen is dragged inwards (this code does it without resizing):
     */
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * And here is where I enable and disable Android kiosk mode:
     *
     * @param enabled
     */
    private void enableKioskMode(boolean enabled) {
        try {
            if (enabled) {
                if (mDpm.isLockTaskPermitted(this.getPackageName())) {
                    startLockTask();
                    mIsKioskEnabled = true;
                    mButton.setText(getString(R.string.exit_kiosk_mode));
                } else {
                    Toast.makeText(this, getString(R.string.kiosk_not_permitted), Toast.LENGTH_SHORT).show();
                }
            } else {
                stopLockTask();
                mIsKioskEnabled = false;
                mButton.setText(getString(R.string.enter_kiosk_mode));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setApplicationHidden(String packageName) {
        if (mDpm.isDeviceOwnerApp(getPackageName())) {
            if (mDpm.isApplicationHidden(deviceAdmin, packageName)) {
                mDpm.setApplicationHidden(deviceAdmin, packageName, false);
                Toast.makeText(this, packageName + " is show", Toast.LENGTH_SHORT).show();
            } else {
                mDpm.setApplicationHidden(deviceAdmin, packageName, true);
                Toast.makeText(this, packageName + " is hidden", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.not_device_owner), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置解锁方式 需要激活设备管理器（配置策略）
     * <p>
     * PASSWORD_QUALITY_ALPHABETIC    // 用户输入的密码必须要有字母（或者其他字符）
     * PASSWORD_QUALITY_ALPHANUMERIC  // 用户输入的密码必须要有字母和数字。
     * PASSWORD_QUALITY_NUMERIC       // 用户输入的密码必须要有数字
     * PASSWORD_QUALITY_SOMETHING     // 由设计人员决定的。
     * PASSWORD_QUALITY_UNSPECIFIED   // 对密码没有要求。
     */
    public void setLockMethod() {
        if (mDpm.isAdminActive(deviceAdmin)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            mDpm.setPasswordQuality(deviceAdmin, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
            startActivity(intent);
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
    }

    // 立刻锁屏
    public void lockNow() {
        if (mDpm.isAdminActive(deviceAdmin)) {
            mDpm.lockNow();
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
    }

    // 设置多长时间后锁屏
    public void lockByTime() {
        long time = 7 * 1000L;
        if (mDpm.isAdminActive(deviceAdmin)) {
            mDpm.setMaximumTimeToLock(deviceAdmin, time);
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
    }

    // 恢复出厂设置
    public void wipeData() {
        if (mDpm.isAdminActive(deviceAdmin)) {
            mDpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
    }

    // 禁用相机
    public void disableCamera(boolean disabled) {
        if (mDpm.isAdminActive(deviceAdmin)) {
            mDpm.setCameraDisabled(deviceAdmin, disabled);
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
    }

    // 重置密码
    public void resetPassword(String password) {
        if (mDpm.isAdminActive(deviceAdmin)) {
            mDpm.resetPassword(password, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
    }

    // 设定存储设备加密
    public int encryptedStorage(boolean isEncrypt) {
        if (mDpm.isAdminActive(deviceAdmin)) {
            int result = mDpm.setStorageEncryption(deviceAdmin, isEncrypt);
            Log.d(TAG, "encryptedStorage: result = " + result);
            return result;
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
        return -1;
    }

    /**
     * 禁用状态栏
     *
     * @param disabled
     * @return false if attempting to disable the status bar failed. true otherwise.
     */
    public boolean setStatusBarDisabled(boolean disabled) {
        boolean result = false;
        if (mDpm.isAdminActive(deviceAdmin)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                result = mDpm.setStatusBarDisabled(deviceAdmin, disabled);
                Log.d(TAG, "setStatusBarDisabled: result = " + result);
            }
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    /**
     * 单一应用权限设置
     *
     * @return whether the permission was successfully granted or revoked.
     */
    public boolean setPermissionGrantState(@NonNull String packageName, @NonNull String permission, int grantState) {
        boolean result = false;
        if (mDpm.isAdminActive(deviceAdmin)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                result = mDpm.setPermissionGrantState(deviceAdmin, packageName, permission, grantState);
                Log.d(TAG, "setStatusBarDisabled: result = " + result);
            }
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    /**
     * 全局动态权限设置
     */
    public void setPermissionGrantState(int policy) {
        if (mDpm.isAdminActive(deviceAdmin)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mDpm.setPermissionPolicy(deviceAdmin, policy);
            }
        } else {
            Toast.makeText(getBaseContext(), "请先激活设备", Toast.LENGTH_SHORT).show();
        }
    }
}
