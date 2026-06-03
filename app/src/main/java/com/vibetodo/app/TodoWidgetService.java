package com.vibetodo.app;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

public class TodoWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    private static class Factory implements RemoteViewsFactory {
        private static final int TYPE_PAIR_HEADER = 0;
        private static final int TYPE_PAIR_TASK = 1;
        private static final int TYPE_DIVIDER = 2;
        private static final int TYPE_FULL_HEADER = 3;
        private static final int TYPE_FULL_TASK = 4;

        private final Context context;
        private final ArrayList<Row> rows = new ArrayList<>();

        Factory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {
            load();
        }

        @Override
        public void onDataSetChanged() {
            load();
        }

        @Override
        public void onDestroy() {
            rows.clear();
        }

        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= rows.size()) {
                return null;
            }
            Row row = rows.get(position);
            if (row.type == TYPE_PAIR_HEADER) {
                return pairHeader(row);
            }
            if (row.type == TYPE_PAIR_TASK) {
                return pairTask(row);
            }
            if (row.type == TYPE_DIVIDER) {
                return new RemoteViews(context.getPackageName(), R.layout.widget_divider);
            }
            if (row.type == TYPE_FULL_HEADER) {
                return fullHeader(row);
            }
            return fullTask(row);
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 5;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        private void load() {
            rows.clear();
            List<TodoCategory> categories = TodoStore.widgetCategories(context);
            for (int i = 0; i < categories.size(); i += 2) {
                if (i + 1 < categories.size()) {
                    addPair(categories.get(i), categories.get(i + 1));
                } else {
                    addFull(categories.get(i));
                }
            }
        }

        private void addPair(TodoCategory left, TodoCategory right) {
            rows.add(Row.pairHeader(left, right));

            List<TodoItem> leftShort = TodoStore.widgetItems(context, left, TodoItem.DURATION_SHORT);
            List<TodoItem> rightShort = TodoStore.widgetItems(context, right, TodoItem.DURATION_SHORT);
            List<TodoItem> leftLong = TodoStore.widgetItems(context, left, TodoItem.DURATION_LONG);
            List<TodoItem> rightLong = TodoStore.widgetItems(context, right, TodoItem.DURATION_LONG);

            int shortRows = Math.max(leftShort.size(), rightShort.size());
            int longRows = Math.max(leftLong.size(), rightLong.size());
            if (shortRows == 0 && longRows == 0) {
                rows.add(Row.pairText("暂无", "暂无"));
                return;
            }

            for (int i = 0; i < shortRows; i++) {
                rows.add(Row.pairTask(itemAt(leftShort, i), itemAt(rightShort, i)));
            }
            if (shortRows > 0 && longRows > 0) {
                rows.add(Row.divider());
            }
            for (int i = 0; i < longRows; i++) {
                rows.add(Row.pairTask(itemAt(leftLong, i), itemAt(rightLong, i)));
            }
        }

        private void addFull(TodoCategory category) {
            rows.add(Row.fullHeader(category));

            List<TodoItem> shortItems = TodoStore.widgetItems(context, category, TodoItem.DURATION_SHORT);
            List<TodoItem> longItems = TodoStore.widgetItems(context, category, TodoItem.DURATION_LONG);
            if (shortItems.isEmpty() && longItems.isEmpty()) {
                rows.add(Row.fullText("暂无"));
                return;
            }

            for (TodoItem item : shortItems) {
                rows.add(Row.fullTask(item));
            }
            if (!shortItems.isEmpty() && !longItems.isEmpty()) {
                rows.add(Row.divider());
            }
            for (TodoItem item : longItems) {
                rows.add(Row.fullTask(item));
            }
        }

        private TodoItem itemAt(List<TodoItem> items, int index) {
            return index < items.size() ? items.get(index) : null;
        }

        private RemoteViews pairHeader(Row row) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_pair_header);
            int size = TodoStore.widgetTextSize(context);
            views.setTextViewText(R.id.widget_left_header, row.leftCategory.name);
            views.setTextViewText(R.id.widget_right_header, row.rightCategory.name);
            views.setTextViewTextSize(R.id.widget_left_header, TypedValue.COMPLEX_UNIT_SP, size);
            views.setTextViewTextSize(R.id.widget_right_header, TypedValue.COMPLEX_UNIT_SP, size);
            return views;
        }

        private RemoteViews fullHeader(Row row) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_full_header);
            int size = TodoStore.widgetTextSize(context);
            views.setTextViewText(R.id.widget_full_header, row.category.name);
            views.setTextViewTextSize(R.id.widget_full_header, TypedValue.COMPLEX_UNIT_SP, size);
            return views;
        }

        private RemoteViews pairTask(Row row) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_pair_task);
            fillPairCell(views, true, row.leftItem, row.leftText);
            fillPairCell(views, false, row.rightItem, row.rightText);
            return views;
        }

        private RemoteViews fullTask(Row row) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_full_task);
            fillFullCell(views, row.item, row.text);
            return views;
        }

        private void fillPairCell(RemoteViews views, boolean left, TodoItem item, String text) {
            int cell = left ? R.id.widget_left_cell : R.id.widget_right_cell;
            int title = left ? R.id.widget_left_title : R.id.widget_right_title;
            int meta = left ? R.id.widget_left_meta : R.id.widget_right_meta;
            int done = left ? R.id.widget_left_done : R.id.widget_right_done;
            int undo = left ? R.id.widget_left_undo : R.id.widget_right_undo;
            fillCell(views, cell, title, meta, done, undo, item, text);
        }

        private void fillFullCell(RemoteViews views, TodoItem item, String text) {
            fillCell(views, R.id.widget_full_cell, R.id.widget_full_title, R.id.widget_full_meta,
                    R.id.widget_full_done, R.id.widget_full_undo, item, text);
        }

        private void fillCell(RemoteViews views, int cell, int title, int meta, int done, int undo, TodoItem item, String text) {
            int size = TodoStore.widgetTextSize(context);
            views.setTextViewTextSize(title, TypedValue.COMPLEX_UNIT_SP, size);
            views.setTextViewTextSize(meta, TypedValue.COMPLEX_UNIT_SP, Math.max(9, size - 3));
            views.setTextViewTextSize(done, TypedValue.COMPLEX_UNIT_SP, size + 3);
            views.setTextViewTextSize(undo, TypedValue.COMPLEX_UNIT_SP, size + 3);

            if (item == null) {
                if (text == null) {
                    views.setViewVisibility(cell, View.INVISIBLE);
                } else {
                    views.setViewVisibility(cell, View.VISIBLE);
                    views.setTextViewText(title, text);
                    views.setTextViewText(meta, "");
                    views.setViewVisibility(done, View.INVISIBLE);
                    views.setViewVisibility(undo, View.INVISIBLE);
                }
                return;
            }

            views.setViewVisibility(cell, View.VISIBLE);
            views.setTextViewText(title, item.title);
            views.setTextViewText(meta, meta(item));
            views.setTextViewText(done, item.periodic ? "+" : "✓");
            views.setViewVisibility(done, View.VISIBLE);
            views.setViewVisibility(undo, item.periodic ? View.VISIBLE : View.GONE);
            views.setTextColor(undo, item.checkInCount() > 0 ? 0xFF8A2B2B : 0xFFAAA49A);
            views.setOnClickFillInIntent(done, tapIntent(item, TodoWidgetProvider.OP_DONE));
            views.setOnClickFillInIntent(undo, tapIntent(item, TodoWidgetProvider.OP_UNDO));
        }

        private Intent tapIntent(TodoItem item, String op) {
            Intent intent = new Intent();
            intent.putExtra(TodoWidgetProvider.EXTRA_ID, item.id);
            intent.putExtra(TodoWidgetProvider.EXTRA_OP, op);
            return intent;
        }

        private String meta(TodoItem item) {
            if (item.periodic) {
                if (item.checkInCount() == 0) {
                    return "未打卡";
                }
                return item.checkInCount() + "次 / " + elapsed(item.lastDoneAt);
            }
            return item.durationLabel();
        }

        private String elapsed(long from) {
            long diff = Math.max(0L, System.currentTimeMillis() - from);
            long hour = 60L * 60L * 1000L;
            long day = 24L * hour;
            long days = diff / day;
            long hours = (diff % day) / hour;
            return days + "天" + hours + "小时";
        }
    }

    private static class Row {
        int type;
        TodoCategory leftCategory;
        TodoCategory rightCategory;
        TodoCategory category;
        TodoItem leftItem;
        TodoItem rightItem;
        TodoItem item;
        String leftText;
        String rightText;
        String text;

        static Row pairHeader(TodoCategory left, TodoCategory right) {
            Row row = new Row();
            row.type = Factory.TYPE_PAIR_HEADER;
            row.leftCategory = left;
            row.rightCategory = right;
            return row;
        }

        static Row pairTask(TodoItem left, TodoItem right) {
            Row row = new Row();
            row.type = Factory.TYPE_PAIR_TASK;
            row.leftItem = left;
            row.rightItem = right;
            return row;
        }

        static Row pairText(String left, String right) {
            Row row = pairTask(null, null);
            row.leftText = left;
            row.rightText = right;
            return row;
        }

        static Row fullHeader(TodoCategory category) {
            Row row = new Row();
            row.type = Factory.TYPE_FULL_HEADER;
            row.category = category;
            return row;
        }

        static Row fullTask(TodoItem item) {
            Row row = new Row();
            row.type = Factory.TYPE_FULL_TASK;
            row.item = item;
            return row;
        }

        static Row fullText(String text) {
            Row row = fullTask(null);
            row.text = text;
            return row;
        }

        static Row divider() {
            Row row = new Row();
            row.type = Factory.TYPE_DIVIDER;
            return row;
        }
    }
}
