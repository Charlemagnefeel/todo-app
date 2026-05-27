package com.vibetodo.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

public class TodoWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_COMPLETE = "com.vibetodo.app.ACTION_COMPLETE_FROM_WIDGET";
    private static final String EXTRA_ID = "todo_id";
    private static final int CATEGORY_LIMIT = 4;

    private static final int[] ROW_IDS = {
            R.id.widget_category_row_1,
            R.id.widget_category_row_2,
            R.id.widget_category_row_3,
            R.id.widget_category_row_4
    };

    private static final int[] TITLE_IDS = {
            R.id.widget_category_title_1,
            R.id.widget_category_title_2,
            R.id.widget_category_title_3,
            R.id.widget_category_title_4
    };

    private static final int[] META_IDS = {
            R.id.widget_category_meta_1,
            R.id.widget_category_meta_2,
            R.id.widget_category_meta_3,
            R.id.widget_category_meta_4
    };

    private static final int[] DONE_IDS = {
            R.id.widget_category_done_1,
            R.id.widget_category_done_2,
            R.id.widget_category_done_3,
            R.id.widget_category_done_4
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_COMPLETE.equals(intent.getAction())) {
            String id = intent.getStringExtra(EXTRA_ID);
            if (id != null) {
                TodoStore.complete(context, id);
                updateWidgets(context);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateOne(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, TodoWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) {
            updateOne(context, manager, id);
        }
    }

    private static void updateOne(Context context, AppWidgetManager manager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo);
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context));
        views.removeAllViews(R.id.widget_content);

        List<TodoCategory> categories = TodoStore.widgetCategories(context);
        views.setViewVisibility(R.id.widget_empty, categories.isEmpty() ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_content, categories.isEmpty() ? View.GONE : View.VISIBLE);

        for (int i = 0; i < categories.size(); i += 2) {
            if (i + 1 < categories.size()) {
                RemoteViews pair = new RemoteViews(context.getPackageName(), R.layout.widget_category_pair);
                pair.addView(R.id.widget_pair_left, categoryView(context, categories.get(i)));
                pair.addView(R.id.widget_pair_right, categoryView(context, categories.get(i + 1)));
                views.addView(R.id.widget_content, pair);
            } else {
                views.addView(R.id.widget_content, categoryView(context, categories.get(i)));
            }
        }

        manager.updateAppWidget(appWidgetId, views);
    }

    private static RemoteViews categoryView(Context context, TodoCategory category) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_category_full);
        views.setTextViewText(R.id.widget_category_header, category.name);
        views.setOnClickPendingIntent(R.id.widget_category_root, openAppIntent(context));

        List<TodoItem> items = TodoStore.widgetItems(context, category, CATEGORY_LIMIT);
        views.setViewVisibility(R.id.widget_category_empty, items.isEmpty() ? View.VISIBLE : View.GONE);

        for (int i = 0; i < ROW_IDS.length; i++) {
            views.setViewVisibility(ROW_IDS[i], View.GONE);
        }

        for (int i = 0; i < items.size(); i++) {
            TodoItem item = items.get(i);
            views.setViewVisibility(ROW_IDS[i], View.VISIBLE);
            views.setTextViewText(TITLE_IDS[i], item.title);
            views.setTextViewText(META_IDS[i], widgetMeta(item));
            views.setTextViewText(DONE_IDS[i], item.periodic ? "打卡" : "✓");
            views.setOnClickPendingIntent(DONE_IDS[i], completeIntent(context, item));
        }
        return views;
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 10_000, intent, pendingFlags());
    }

    private static PendingIntent completeIntent(Context context, TodoItem item) {
        Intent intent = new Intent(context, TodoWidgetProvider.class);
        intent.setAction(ACTION_COMPLETE);
        intent.putExtra(EXTRA_ID, item.id);
        return PendingIntent.getBroadcast(context, item.id.hashCode(), intent, pendingFlags());
    }

    private static int pendingFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    private static String widgetMeta(TodoItem item) {
        if (item.periodic) {
            if (item.checkInCount() == 0) {
                return "未打卡";
            }
            return item.checkInCount() + "次 / " + elapsedDaysHours(item.lastDoneAt);
        }
        return item.durationLabel();
    }

    private static String elapsedDaysHours(long from) {
        long diff = Math.max(0L, System.currentTimeMillis() - from);
        long hour = 60L * 60L * 1000L;
        long day = 24L * hour;
        long days = diff / day;
        long hours = (diff % day) / hour;
        return days + "天" + hours + "小时";
    }
}
