package de.robv.android.xposed.installer.advanced;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import de.robv.android.xposed.installer.R;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.DownloadsUtil;

import static de.robv.android.xposed.installer.XposedApp.WRITE_EXTERNAL_PERMISSION;

public class StatusInstallerFragment extends Fragment {

    public static final File DISABLE_FILE = new File(XposedApp.BASE_DIR + "conf/disabled");
    private static Activity sActivity;
    private static ImageView mErrorIcon;
    private static TextView mErrorTv;
    private static View mUpdateView;
    private static View mUpdateButton;
    private static String mUpdateLink;
    private static Fragment sFragment;

    public static void setError(boolean connectionFailed, boolean noSdks) {
        if (!connectionFailed && !noSdks) return;

        mErrorTv.setVisibility(View.VISIBLE);
        mErrorIcon.setVisibility(View.VISIBLE);
        if (noSdks) {
            mErrorIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_warning_grey));
            mErrorTv.setText(String.format(sActivity.getString(R.string.phone_not_compatible), Build.VERSION.SDK_INT, Build.CPU_ABI));
        }
        if (connectionFailed) {
            mErrorIcon.setImageDrawable(sActivity.getResources().getDrawable(R.drawable.ic_no_connection));
            mErrorTv.setText(sActivity.getString(R.string.loadingError));
        }
    }

    public static void setUpdate(final String link, final String changelog) {
        mUpdateLink = link;

        mUpdateView.setVisibility(View.VISIBLE);
        mUpdateButton.setVisibility(View.VISIBLE);
        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new MaterialDialog.Builder(sActivity)
                        .title(R.string.changes)
                        .content(Html.fromHtml(changelog))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                update();
                            }
                        })
                        .positiveText(R.string.update)
                        .negativeText(R.string.later).show();
            }
        });
    }

    private static void update() {
        if (checkPermissions()) return;

        final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/XposedInstaller/XposedInstaller_by_dvdandroid.apk";

        new File(path).delete();

        DownloadsUtil.add(sActivity, "XposedInstaller_by_dvdandroid", mUpdateLink, new DownloadsUtil.DownloadFinishedCallback() {
            @Override
            public void onDownloadFinished(Context context, DownloadsUtil.DownloadInfo info) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(path)), DownloadsUtil.MIME_TYPE_APK);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }, DownloadsUtil.MIME_TYPES.APK, true);
    }

    private static boolean checkPermissions() {
        if (Build.VERSION.SDK_INT < 23) return true;

        if (ActivityCompat.checkSelfPermission(sActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            sFragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sActivity = getActivity();
        sFragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.status_installer, container, false);

        mErrorIcon = (ImageView) v.findViewById(R.id.errorIcon);
        mErrorTv = (TextView) v.findViewById(R.id.errorTv);
        mUpdateView = v.findViewById(R.id.updateView);
        mUpdateButton = v.findViewById(R.id.click_to_update);

        TextView txtInstallError = (TextView) v.findViewById(R.id.framework_install_errors);
        View txtInstallContainer = v.findViewById(R.id.status_container);
        ImageView txtInstallIcon = (ImageView) v.findViewById(R.id.status_icon);

        String installedXposedVersion = XposedApp.getXposedProp().get("version");
        View disableView = v.findViewById(R.id.disableView);
        final Switch xposedDisable = (Switch) v.findViewById(R.id.disableSwitch);

        TextView androidSdk = (TextView) v.findViewById(R.id.android_version);
        TextView manufacturer = (TextView) v.findViewById(R.id.ic_manufacturer);
        TextView cpu = (TextView) v.findViewById(R.id.cpu);

        if (Build.VERSION.SDK_INT >= 21) {
            if (installedXposedVersion != null) {
                int installedXposedVersionInt = extractIntPart(installedXposedVersion);
                if (installedXposedVersionInt == XposedApp.getXposedVersion()) {
                    txtInstallError.setText(getString(R.string.installed_lollipop, installedXposedVersion));
                    txtInstallError.setTextColor(getResources().getColor(R.color.darker_green));
                    txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.darker_green));
                    txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_circle));
                } else {
                    txtInstallError.setText(getString(R.string.installed_lollipop_inactive, installedXposedVersion));
                    txtInstallError.setTextColor(getResources().getColor(R.color.amber_500));
                    txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.amber_500));
                    txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning));
                }
            } else {
                txtInstallError.setText(R.string.not_installed_no_lollipop);
                txtInstallError.setTextColor(getResources().getColor(R.color.warning));
                txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.warning));
                txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_no_image));
                xposedDisable.setVisibility(View.GONE);
                disableView.setVisibility(View.GONE);
            }
        } else {
            int installedXposedVersionInt = XposedApp.getXposedVersion();
            if (installedXposedVersionInt != 0) {
                txtInstallError.setText(getString(R.string.installed_lollipop, installedXposedVersionInt));
                txtInstallError.setTextColor(getResources().getColor(R.color.darker_green));
                txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.darker_green));
                txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_circle));
                if (DISABLE_FILE.exists()) {
                    txtInstallError.setText(getString(R.string.installed_lollipop_inactive, installedXposedVersionInt));
                    txtInstallError.setTextColor(getResources().getColor(R.color.amber_500));
                    txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.amber_500));
                    txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning));
                }
            } else {
                txtInstallError.setText(getString(R.string.not_installed_no_lollipop));
                txtInstallError.setTextColor(getResources().getColor(R.color.warning));
                txtInstallContainer.setBackgroundColor(getResources().getColor(R.color.warning));
                txtInstallIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_no_image));
                xposedDisable.setVisibility(View.GONE);
                disableView.setVisibility(View.GONE);
            }
        }

        xposedDisable.setChecked(!DISABLE_FILE.exists());

        xposedDisable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (DISABLE_FILE.exists()) {
                    DISABLE_FILE.delete();
                    Snackbar.make(xposedDisable, R.string.xposed_on_next_reboot, Snackbar.LENGTH_LONG).show();
                } else {
                    try {
                        DISABLE_FILE.createNewFile();
                        Snackbar.make(xposedDisable, R.string.xposed_off_next_reboot, Snackbar.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Log.e(XposedApp.TAG, "InstallerFragment -> " + e.getMessage());
                    }
                }
            }
        });

        androidSdk.setText(getString(R.string.android_sdk, getAndroidVersion(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        manufacturer.setText(getUIFramework());
        cpu.setText(getArch());

        return v;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                }, 500);
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getAndroidVersion() {
        switch (Build.VERSION.SDK_INT) {
            case 16:
            case 17:
            case 18:
                return "Jelly Bean";
            case 19:
                return "KitKat";
            case 21:
            case 22:
                return "Lollipop";
            case 23:
                return "Marshmallow";
            case 24:
                return "Nougat";
        }
        return "";
    }

    private String getUIFramework() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        if (manufacturer.contains("Samsung")) {
            manufacturer += new File("/system/framework/twframework.jar").exists() ? "(TouchWiz)" : "(AOSP-based ROM)";
        } else if (manufacturer.contains("Xioami")) {
            manufacturer += new File("/system/framework/framework-miui-res.apk").exists() ? "(MIUI)" : "(AOSP-based ROM)";
        }
        return manufacturer;
    }

    private String getArch() {
        String info = "";

        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text;
            while ((text = br.readLine()) != null) {
                if (!text.startsWith("processor")) break;
            }
            br.close();
            String[] array = text.split(":\\s+", 2);
            if (array.length >= 2) {
                info += array[1] + " ";
            }
        } catch (IOException ignored) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            info += Build.SUPPORTED_ABIS[0];
        } else {
            String arch = System.getenv("os.arch");
            if (arch != null) info += arch;
        }
        info += " (";
        if (info.contains("x86")) {
            info += "x86";
        } else if (info.contains("x86_64")) {
            info += "x86_64";
        } else if (info.contains("arm64")) {
            info += "arm64";
        } else {
            info += "arm";
        }
        return info + ")";
    }

    private int extractIntPart(String str) {
        int result = 0, length = str.length();
        for (int offset = 0; offset < length; offset++) {
            char c = str.charAt(offset);
            if ('0' <= c && c <= '9')
                result = result * 10 + (c - '0');
            else
                break;
        }
        return result;
    }
}
