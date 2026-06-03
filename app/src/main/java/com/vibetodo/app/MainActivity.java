package com.vibetodo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_EXPORT = 1;
    private static final int REQ_IMPORT = 2;
    private static final String DRAG_TASK = "todo_task";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            renderLists();
            handler.postDelayed(this, 60_000L);
        }
    };

    private LinearLayout list;
    private EditText titleBox;
    private EditText catBox;
    private LinearLayout catChoices;
    private LinearLayout catPrefs;
    private TextView formTitle;
    private Button saveBtn;
    private Button cancelBtn;
    private final ArrayList<Button> catBtns = new ArrayList<>();
    private final ArrayList<Button> sizeBtns = new ArrayList<>();
    private Button shortButton;
    private Button longButton;
    private String catId = TodoCategory.INDOOR_ID;
    private String duration = TodoItem.DURATION_SHORT;
    private String editingId;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        renderLists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderLists();
        TodoWidgetProvider.updateWidgets(this);
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 60_000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT) {
            writeBackup(uri);
        } else if (requestCode == REQ_IMPORT) {
            confirmImport(uri);
        }
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setBackgroundColor(Color.parseColor("#F8F5EF"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("自用待办", 28, Color.parseColor("#202124"), true);
        root.addView(title);

        TextView subtitle = text("用类别整理任务；普通任务完成后归档，定期任务保留每次打卡记录。", 14, Color.parseColor("#636963"), false);
        subtitle.setPadding(0, dp(6), 0, dp(18));
        root.addView(subtitle);

        root.addView(buildInputPanel());
        root.addView(buildCategoryPanel());

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(18), 0, 0);
        root.addView(list);

        setContentView(scrollView);
    }

    private View buildInputPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackgroundResource(R.drawable.card_bg);

        formTitle = text("新增待办", 16, Color.parseColor("#202124"), true);
        formTitle.setPadding(0, 0, 0, dp(10));
        panel.addView(formTitle);

        titleBox = new EditText(this);
        titleBox.setHint("写下要做的事");
        titleBox.setSingleLine(false);
        titleBox.setMinLines(1);
        titleBox.setMaxLines(3);
        titleBox.setTextSize(16);
        titleBox.setPadding(dp(10), dp(8), dp(10), dp(8));
        panel.addView(titleBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        panel.addView(label("类别"));
        catChoices = new LinearLayout(this);
        catChoices.setOrientation(LinearLayout.VERTICAL);
        panel.addView(catChoices);
        renderCategoryChoices();

        panel.addView(label("周期"));
        LinearLayout durationRow = row();
        shortButton = choiceButton("短期");
        longButton = choiceButton("长期");
        shortButton.setOnClickListener(v -> {
            duration = TodoItem.DURATION_SHORT;
            updateChoiceStyles();
        });
        longButton.setOnClickListener(v -> {
            duration = TodoItem.DURATION_LONG;
            updateChoiceStyles();
        });
        durationRow.addView(shortButton, cellParams());
        durationRow.addView(longButton, cellParams());
        panel.addView(durationRow);

        saveBtn = new Button(this);
        saveBtn.setAllCaps(false);
        saveBtn.setText("添加");
        saveBtn.setTextSize(16);
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setBackground(bg(Color.parseColor("#247A4D"), 8, 0));
        saveBtn.setOnClickListener(v -> saveTask());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46));
        addParams.topMargin = dp(8);
        panel.addView(saveBtn, addParams);

        cancelBtn = new Button(this);
        cancelBtn.setAllCaps(false);
        cancelBtn.setText("取消编辑");
        cancelBtn.setTextSize(14);
        cancelBtn.setTextColor(Color.parseColor("#202124"));
        cancelBtn.setBackground(bg(Color.TRANSPARENT, 8, Color.parseColor("#DDD7CD")));
        cancelBtn.setVisibility(View.GONE);
        cancelBtn.setOnClickListener(v -> resetForm());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42));
        cancelParams.topMargin = dp(8);
        panel.addView(cancelBtn, cancelParams);

        updateChoiceStyles();
        return panel;
    }

    private View buildCategoryPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackgroundResource(R.drawable.card_bg);
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        panelParams.topMargin = dp(12);
        panel.setLayoutParams(panelParams);

        TextView title = text("类别和小组件显示", 16, Color.parseColor("#202124"), true);
        title.setPadding(0, 0, 0, dp(8));
        panel.addView(title);

        catBox = new EditText(this);
        catBox.setHint("新增自定义类别");
        catBox.setSingleLine(true);
        catBox.setTextSize(15);
        catBox.setPadding(dp(10), dp(8), dp(10), dp(8));
        panel.addView(catBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button addCatBtn = new Button(this);
        addCatBtn.setAllCaps(false);
        addCatBtn.setText("添加类别");
        addCatBtn.setTextSize(15);
        addCatBtn.setTextColor(Color.WHITE);
        addCatBtn.setBackground(bg(Color.parseColor("#2B5C8A"), 8, 0));
        addCatBtn.setOnClickListener(v -> addCategory());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42));
        addParams.topMargin = dp(8);
        panel.addView(addCatBtn, addParams);

        catPrefs = new LinearLayout(this);
        catPrefs.setOrientation(LinearLayout.VERTICAL);
        catPrefs.setPadding(0, dp(8), 0, 0);
        panel.addView(catPrefs);
        renderCategorySettings();

        panel.addView(label("小组件字号"));
        LinearLayout sizeRow = row();
        sizeRow.addView(widgetSizeButton("小", 11), cellParams());
        sizeRow.addView(widgetSizeButton("中", 13), cellParams());
        sizeRow.addView(widgetSizeButton("大", 16), cellParams());
        panel.addView(sizeRow);
        updateSizeStyles();

        LinearLayout dataRow = row();
        Button exportBtn = choiceButton("导出数据");
        exportBtn.setOnClickListener(v -> exportData());
        Button importBtn = choiceButton("导入数据");
        importBtn.setOnClickListener(v -> importData());
        dataRow.addView(exportBtn, cellParams());
        dataRow.addView(importBtn, cellParams());
        LinearLayout.LayoutParams dataParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dataParams.topMargin = dp(10);
        panel.addView(dataRow, dataParams);

        return panel;
    }

    private void renderCategoryChoices() {
        if (catChoices == null) {
            return;
        }
        catChoices.removeAllViews();
        catBtns.clear();

        List<TodoCategory> categories = TodoStore.loadCategories(this);
        boolean found = false;
        for (TodoCategory cat : categories) {
            if (cat.id.equals(catId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            catId = categories.get(0).id;
        }

        LinearLayout line = null;
        for (int i = 0; i < categories.size(); i++) {
            if (i % 2 == 0) {
                line = row();
                catChoices.addView(line);
            }
            TodoCategory cat = categories.get(i);
            Button button = choiceButton(cat.name);
            button.setTag(cat.id);
            button.setOnClickListener(v -> {
                catId = (String) v.getTag();
                updateChoiceStyles();
            });
            catBtns.add(button);
            line.addView(button, cellParams());
        }
        updateChoiceStyles();
    }

    private void renderCategorySettings() {
        if (catPrefs == null) {
            return;
        }
        catPrefs.removeAllViews();
        for (TodoCategory cat : TodoStore.loadCategories(this)) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText("显示在小组件：" + cat.name);
            checkBox.setTextColor(Color.parseColor("#202124"));
            checkBox.setTextSize(14);
            checkBox.setChecked(cat.showInWidget);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                TodoStore.setCategoryVisible(this, cat.id, isChecked);
                TodoWidgetProvider.updateWidgets(this);
            });
            catPrefs.addView(checkBox);
        }
    }

    private void addCategory() {
        String name = catBox.getText().toString().trim();
        if (name.isEmpty()) {
            catBox.setError("先写类别名");
            catBox.requestFocus();
            return;
        }
        TodoStore.addCategory(this, name);
        catBox.setText("");
        List<TodoCategory> categories = TodoStore.loadCategories(this);
        catId = categories.get(categories.size() - 1).id;
        renderCategoryChoices();
        renderCategorySettings();
        renderLists();
        TodoWidgetProvider.updateWidgets(this);
        hideKeyboard();
    }

    private Button widgetSizeButton(String label, int size) {
        Button button = choiceButton(label);
        button.setTag(size);
        button.setOnClickListener(v -> {
            TodoStore.setWidgetTextSize(this, (Integer) v.getTag());
            updateSizeStyles();
            TodoWidgetProvider.updateWidgets(this);
        });
        sizeBtns.add(button);
        return button;
    }

    private void exportData() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "todo-backup-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.CHINA).format(new Date()) + ".json");
        startActivityForResult(intent, REQ_EXPORT);
    }

    private void importData() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQ_IMPORT);
    }

    private void writeBackup(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                throw new IOException("open output failed");
            }
            String raw = TodoStore.exportData(this).toString(2);
            out.write(raw.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "数据已导出", Toast.LENGTH_SHORT).show();
        } catch (IOException | JSONException e) {
            showError("导出失败");
        }
    }

    private void confirmImport(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle("导入数据")
                .setMessage("导入会覆盖当前所有任务和类别，确定继续吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("导入", (dialog, which) -> readBackup(uri))
                .show();
    }

    private void readBackup(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("open input failed");
            }
            TodoStore.importData(this, new JSONObject(readAll(in)));
            resetForm();
            renderCategoryChoices();
            renderCategorySettings();
            updateSizeStyles();
            renderLists();
            TodoWidgetProvider.updateWidgets(this);
            Toast.makeText(this, "数据已导入", Toast.LENGTH_SHORT).show();
        } catch (IOException | JSONException e) {
            showError("导入失败，请确认文件是待办备份");
        }
    }

    private String readAll(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("出错了")
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .show();
    }

    private void saveTask() {
        String title = titleBox.getText().toString().trim();
        if (title.isEmpty()) {
            titleBox.setError("先写一个待办");
            titleBox.requestFocus();
            return;
        }

        TodoCategory category = TodoStore.findCategory(this, catId);
        if (editingId == null) {
            TodoItem item = TodoItem.create(title, category.id, duration, category.periodic);
            TodoStore.add(this, item);
        } else {
            TodoStore.update(this, editingId, title, category.id, duration);
        }
        resetForm();
        hideKeyboard();
        renderLists();
        TodoWidgetProvider.updateWidgets(this);
    }

    private void startEdit(TodoItem item) {
        editingId = item.id;
        formTitle.setText("编辑待办");
        saveBtn.setText("保存修改");
        cancelBtn.setVisibility(View.VISIBLE);

        titleBox.setText(item.title);
        titleBox.setSelection(titleBox.getText().length());
        catId = item.categoryId;
        duration = item.duration;
        updateChoiceStyles();
        titleBox.requestFocus();
    }

    private void resetForm() {
        editingId = null;
        if (formTitle != null) {
            formTitle.setText("新增待办");
        }
        if (saveBtn != null) {
            saveBtn.setText("添加");
        }
        if (cancelBtn != null) {
            cancelBtn.setVisibility(View.GONE);
        }
        if (titleBox != null) {
            titleBox.setText("");
        }
        catId = TodoCategory.INDOOR_ID;
        duration = TodoItem.DURATION_SHORT;
        updateChoiceStyles();
    }

    private void renderLists() {
        if (list == null) {
            return;
        }

        list.removeAllViews();
        List<TodoItem> items = TodoStore.sorted(TodoStore.load(this));

        int activeCount = 0;
        for (TodoCategory category : TodoStore.loadCategories(this)) {
            int sectionCount = addCategorySection(category, items);
            activeCount += sectionCount;
        }
        if (activeCount == 0) {
            TextView empty = text("这里暂时没有任务", 14, Color.parseColor("#636963"), false);
            empty.setPadding(dp(4), dp(14), 0, dp(8));
            list.addView(empty);
        }
        addArchivedSection(items);
    }

    private int addCategorySection(TodoCategory category, List<TodoItem> items) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        int count = 0;
        for (TodoItem item : items) {
            if (!item.archived && category.id.equals(item.categoryId)) {
                section.addView(taskCard(item));
                count++;
            }
        }
        if (count > 0) {
            TextView header = text(category.name, 18, Color.parseColor("#202124"), true);
            header.setPadding(0, dp(14), 0, dp(8));
            list.addView(header);
            list.addView(section);
        }
        return count;
    }

    private void addArchivedSection(List<TodoItem> items) {
        TextView header = text("已归档", 18, Color.parseColor("#202124"), true);
        header.setPadding(0, dp(14), 0, dp(8));
        list.addView(header);

        int count = 0;
        for (TodoItem item : items) {
            if (item.archived) {
                list.addView(taskCard(item));
                count++;
            }
        }

        if (count == 0) {
            TextView empty = text("还没有归档任务", 14, Color.parseColor("#636963"), false);
            empty.setPadding(dp(4), dp(2), 0, dp(8));
            list.addView(empty);
        }
    }

    private View taskCard(TodoItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackgroundResource(R.drawable.card_bg);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(8);
        card.setLayoutParams(cardParams);
        setupDrag(card, item);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = text(item.title, 16, Color.parseColor("#202124"), true);
        topRow.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button edit = new Button(this);
        edit.setAllCaps(false);
        edit.setText("编辑");
        edit.setTextSize(14);
        edit.setTextColor(Color.parseColor("#202124"));
        edit.setBackground(bg(Color.TRANSPARENT, 8, Color.parseColor("#DDD7CD")));
        edit.setOnClickListener(v -> startEdit(item));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dp(58), dp(42));
        editParams.rightMargin = dp(8);
        topRow.addView(edit, editParams);

        Button delete = new Button(this);
        delete.setAllCaps(false);
        delete.setText("删除");
        delete.setTextSize(14);
        delete.setTextColor(Color.parseColor("#8A2B2B"));
        delete.setBackground(bg(Color.TRANSPARENT, 8, Color.parseColor("#E1C5C5")));
        delete.setOnClickListener(v -> confirmDelete(item));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(58), dp(42));
        deleteParams.rightMargin = dp(8);
        topRow.addView(delete, deleteParams);

        if (item.periodic && !item.archived) {
            Button undo = new Button(this);
            undo.setAllCaps(false);
            undo.setText("-1");
            undo.setTextSize(14);
            undo.setTextColor(item.checkInCount() > 0 ? Color.parseColor("#8A2B2B") : Color.parseColor("#AAA49A"));
            undo.setBackground(bg(Color.TRANSPARENT, 8, Color.parseColor("#E1C5C5")));
            undo.setEnabled(item.checkInCount() > 0);
            undo.setOnClickListener(v -> {
                TodoStore.undoCheckIn(this, item.id);
                renderLists();
                TodoWidgetProvider.updateWidgets(this);
            });
            LinearLayout.LayoutParams undoParams = new LinearLayout.LayoutParams(dp(54), dp(42));
            undoParams.rightMargin = dp(8);
            topRow.addView(undo, undoParams);
        }

        Button action = new Button(this);
        action.setAllCaps(false);
        action.setText(item.archived ? "还原" : item.periodic ? "打卡" : "✓");
        action.setTextSize(item.periodic || item.archived ? 14 : 20);
        action.setTextColor(Color.WHITE);
        int color = item.archived ? Color.parseColor("#2B5C8A") : Color.parseColor("#247A4D");
        action.setBackground(bg(color, 8, 0));
        action.setOnClickListener(v -> {
            if (item.archived) {
                TodoStore.restore(this, item.id);
            } else {
                TodoStore.complete(this, item.id);
            }
            renderLists();
            TodoWidgetProvider.updateWidgets(this);
        });
        topRow.addView(action, new LinearLayout.LayoutParams(dp(item.periodic || item.archived ? 70 : 48), dp(42)));

        card.addView(topRow);

        TextView meta = text(metaText(item), 13, Color.parseColor("#636963"), false);
        meta.setPadding(0, dp(6), 0, 0);
        card.addView(meta);

        return card;
    }

    private void setupDrag(View card, TodoItem item) {
        if (item.archived) {
            return;
        }
        card.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText(DRAG_TASK, item.id);
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(data, shadow, item.id, 0);
            } else {
                v.startDrag(data, shadow, item.id, 0);
            }
            return true;
        });
        card.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getLocalState() instanceof String;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.72f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setAlpha(1f);
                    return true;
                case DragEvent.ACTION_DROP:
                    v.setAlpha(1f);
                    String fromId = (String) event.getLocalState();
                    boolean after = event.getY() > v.getHeight() / 2f;
                    if (TodoStore.move(this, fromId, item.id, after)) {
                        renderLists();
                        TodoWidgetProvider.updateWidgets(this);
                    } else if (!item.id.equals(fromId)) {
                        Toast.makeText(this, "只能在同类别、同长短期内排序", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                default:
                    return true;
            }
        });
    }

    private void confirmDelete(TodoItem item) {
        new AlertDialog.Builder(this)
                .setTitle("删除待办")
                .setMessage("确定删除“" + item.title + "”吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    TodoStore.delete(this, item.id);
                    if (item.id.equals(editingId)) {
                        resetForm();
                    }
                    renderLists();
                    TodoWidgetProvider.updateWidgets(this);
                })
                .show();
    }

    private String metaText(TodoItem item) {
        String base = TodoStore.categoryName(this, item.categoryId) + " / " + item.durationLabel();
        if (item.archived) {
            return base + " / 已归档：" + formatTime(item.archivedAt);
        }
        if (item.periodic) {
            if (item.checkInCount() == 0) {
                return base + " / 还没打卡";
            }
            return base
                    + " / 已打卡 " + item.checkInCount() + " 次"
                    + "\n上次：" + formatTime(item.lastDoneAt) + " / 距今：" + elapsedDaysHours(item.lastDoneAt)
                    + "\n记录：" + checkInDates(item);
        }
        return base + " / 创建：" + formatTime(item.createdAt);
    }

    private String elapsedDaysHours(long from) {
        long diff = Math.max(0L, System.currentTimeMillis() - from);
        long hour = 60L * 60L * 1000L;
        long day = 24L * hour;
        long days = diff / day;
        long hours = (diff % day) / hour;
        return days + "天" + hours + "小时";
    }

    private String checkInDates(TodoItem item) {
        StringBuilder builder = new StringBuilder();
        List<Long> recent = item.recentCheckIns(Integer.MAX_VALUE);
        for (int i = 0; i < recent.size(); i++) {
            if (i > 0) {
                builder.append("、");
            }
            builder.append(formatTime(recent.get(i)));
        }
        if (item.checkInCount() > recent.size()) {
            builder.append(" 等 ").append(item.checkInCount()).append(" 次");
        }
        return builder.toString();
    }

    private String formatTime(long time) {
        if (time <= 0L) {
            return "无";
        }
        return dateFmt.format(new Date(time));
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.05f);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 13, Color.parseColor("#636963"), true);
        view.setPadding(0, dp(14), 0, dp(6));
        return view;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout.LayoutParams cellParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.rightMargin = dp(8);
        return params;
    }

    private Button choiceButton(String value) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(value);
        button.setTextSize(14);
        return button;
    }

    private void updateChoiceStyles() {
        for (Button button : catBtns) {
            Object tag = button.getTag();
            styleChoice(button, tag != null && tag.equals(catId));
        }
        styleChoice(shortButton, TodoItem.DURATION_SHORT.equals(duration));
        styleChoice(longButton, TodoItem.DURATION_LONG.equals(duration));
    }

    private void updateSizeStyles() {
        int size = TodoStore.widgetTextSize(this);
        for (Button button : sizeBtns) {
            Object tag = button.getTag();
            styleChoice(button, tag instanceof Integer && ((Integer) tag) == size);
        }
    }

    private void styleChoice(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setTextColor(selected ? Color.WHITE : Color.parseColor("#202124"));
        int fill = selected ? Color.parseColor("#2B5C8A") : Color.TRANSPARENT;
        int stroke = selected ? Color.TRANSPARENT : Color.parseColor("#DDD7CD");
        button.setBackground(bg(fill, 8, stroke));
    }

    private GradientDrawable bg(int fill, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(radius));
        drawable.setColor(fill);
        if (stroke != 0) {
            drawable.setStroke(dp(1), stroke);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View current = getCurrentFocus();
        if (imm != null && current != null) {
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            current.clearFocus();
        }
    }
}

