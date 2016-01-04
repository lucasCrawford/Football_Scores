package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * Created by lcrawford on 1/3/16.
 */
public class TodayWidgetService extends RemoteViewsService {

    public final String LOG_TAG = TodayWidgetService.class.getSimpleName();

    private static final String[] TABLE_COLS = {
            DatabaseContract.SCORES_TABLE + "." + DatabaseContract.scores_table._ID,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.TIME_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL
    };

    // these indices must match the projection
    static final int INDEX_GAME_ID = 0;
    static final int INDEX_HOME_COL = 1;
    static final int INDEX_AWAY_COL = 2;
    static final int INDEX_TIME_COL = 3;
    static final int INDEX_HOME_GOALS_COL = 4;
    static final int INDEX_AWAY_GOALS_COL = 5;

    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsService.RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                Uri scoresWithDate = DatabaseContract.scores_table.buildScoreWithDate();

                Date fragmentdate = new Date(System.currentTimeMillis());
                SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
                String date = mformat.format(fragmentdate);

                data = getContentResolver().query(scoresWithDate,
                        TABLE_COLS,
                        DatabaseContract.scores_table.DATE_COL + "=?",
                        new String[]{date},
                        null);

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_list_item);

                /* Icons for the two teams */
                int homeIconRes = Utilies.getTeamCrestByTeamName(data.getString(INDEX_HOME_COL));
                int awayIconRes = Utilies.getTeamCrestByTeamName(data.getString(INDEX_AWAY_COL));

                /* Scores string */
                String scoreText = Utilies.getScores(data.getInt(INDEX_HOME_GOALS_COL), data.getInt(INDEX_AWAY_GOALS_COL));

                /* Set the team icons */
                views.setImageViewResource(R.id.home_crest, homeIconRes);
                views.setImageViewResource(R.id.away_crest, awayIconRes);

                /* Set the time and score */
                views.setTextViewText(R.id.data_textview, data.getString(INDEX_TIME_COL));
                views.setTextViewText(R.id.score_textview, scoreText);

                /* Set the team names */
                views.setTextViewText(R.id.home_name, data.getString(INDEX_HOME_COL));
                views.setTextViewText(R.id.away_name, data.getString(INDEX_AWAY_COL));

                setRemoteContentDescription(views, "Crest for each team");
                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                views.setContentDescription(R.id.home_crest, description);
                views.setContentDescription(R.id.away_name, description);
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.scores_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_GAME_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
