package org.fdroid.fdroid.views.main;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.fdroid.database.AppListItem;
import org.fdroid.database.AppPrefsDao;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.views.installed.InstalledAppListAdapter;

import java.util.List;

public class InstalledAppsViewBinder extends AppCompatActivity{
    public static final String TAG = "CategoriesViewBinder";
    private FDroidDatabase db;
    private final AppCompatActivity activity;
    private final InstalledAppListAdapter adapter;
    private RecyclerView appList;
    private TextView emptyState;

    InstalledAppsViewBinder(final AppCompatActivity activity, FrameLayout parent) {
        this.activity = activity;

//        FDroidApp fdroidApp = (FDroidApp) activity.getApplication();
//        fdroidApp.setSecureWindow(activity);
//
//        fdroidApp.applyPureBlackBackgroundInDarkTheme(activity);

//        activity.setContentView(R.layout.installed_apps_layout);
        View installedAppsView = activity.getLayoutInflater().inflate(R.layout.installed_apps_layout, parent, true);

//        MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
//        activity.setSupportActionBar(toolbar);
//        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new InstalledAppListAdapter(activity);

        appList = installedAppsView.findViewById(R.id.app_list);
        appList.setHasFixedSize(true);
        appList.setLayoutManager(new LinearLayoutManager(activity));
        appList.setAdapter(adapter);

        emptyState = installedAppsView.findViewById(R.id.empty_state);

        db = DBHelper.getDb(activity);
        db.getAppDao().getInstalledAppListItems(activity.getPackageManager()).observe(activity, this::onLoadFinished);
    }

    private void onLoadFinished(List<AppListItem> items) {
        adapter.setApps(items);

        if (adapter.getItemCount() == 0) {
            appList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            appList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        // load app prefs for each app off the UiThread and update item if updates are ignored
        AppPrefsDao appPrefsDao = db.getAppPrefsDao();
        for (AppListItem item : items) {
            Utils.observeOnce(appPrefsDao.getAppPrefs(item.getPackageName()), activity, appPrefs -> {
                if (appPrefs.getIgnoreVersionCodeUpdate() > 0) adapter.updateItem(item, appPrefs);
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        activity.getMenuInflater().inflate(R.menu.installed_apps, menu);
        return activity.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_share) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("packageName,versionCode,versionName\n");
            for (int i = 0; i < adapter.getItemCount(); i++) {
                App app = adapter.getItem(i);
                if (app != null) {
                    stringBuilder.append(app.packageName).append(',')
                            .append(app.installedVersionCode).append(',')
                            .append(app.installedVersionName).append('\n');
                }
            }
            ShareCompat.IntentBuilder intentBuilder = new ShareCompat.IntentBuilder(this)
                    .setSubject(getString(R.string.send_installed_apps))
                    .setChooserTitle(R.string.send_installed_apps)
                    .setText(stringBuilder.toString())
                    .setType("text/csv");
            startActivity(intentBuilder.getIntent());
        }
        return super.onOptionsItemSelected(item);
    }
}
