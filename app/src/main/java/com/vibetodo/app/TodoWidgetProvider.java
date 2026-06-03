package com.vibetodo.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

public class TodoWidgetProvider extends AppWidgetProvider {
    static final String ACTION_TAP = "com.vibetodo.app.ACTION_WIDGET_TAP";
    static final String EXTRA_ID = "todo_id";
    static final String EXTRA_OP = "op";
    static final String OP_DONE = "done";
    static final String OP_UNDO = "undo";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TAP.equals(intent.getAction())) {
            String id = intent.getStringExtra(EXTRA_ID);
            String op = intent.getStringExtra(EXTRA_OP);
            if (id != null) {
                if (OP_UNDO.equals(op)) {
                    TodoStore.undoCheckIn(context, id);
                } else {
                    TodoStore.complete(context, id);
                }
                updateWidgets(context);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateOne(context, manager, id);
        }
    }

    static void updateWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, TodoWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) {
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list);
            updateOne(context, manager, id);
        }
    }

    private static void updateOne(Context context, AppWidgetManager manager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo);
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context));
        views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, TodoStore.widgetTextSize(context) + 3);
        views.setViewVisibility(R.id.widget_empty, View.VISIBLE);

        Intent service = new Intent(context, TodoWidgetService.class);
        service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        service.setData(Uri.parse(service.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list, service);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);
        views.setPendingIntentTemplate(R.id.widget_list, tapTemplate(context));

        manager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 10_000, intent, pendingFlags());
    }

    private static PendingIntent tapTemplate(Context context) {
        Intent intent = new Intent(context, TodoWidgetProvider.class);
        intent.setAction(ACTION_TAP);
        return PendingIntent.getBroadcast(context, 20_000, intent, pendingFlags());
    }

    private static int pendingFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }
}
