package com.vibetodo.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class TodoStore {
    private static final String PREFS_NAME = "vibe_todo";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_CATEGORIES = "categories";

    static List<TodoItem> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_ITEMS, "[]");
        ArrayList<TodoItem> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json != null) {
                    items.add(TodoItem.fromJson(json));
                }
            }
        } catch (JSONException ignored) {
            return items;
        }
        return items;
    }

    static void add(Context context, TodoItem item) {
        List<TodoItem> items = load(context);
        items.add(0, item);
        save(context, items);
    }

    static List<TodoCategory> loadCategories(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_CATEGORIES, "");
        if (raw.isEmpty()) {
            return defaultCategories();
        }

        ArrayList<TodoCategory> categories = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json != null) {
                    TodoCategory category = TodoCategory.fromJson(json);
                    if (!category.id.isEmpty() && !category.name.isEmpty()) {
                        categories.add(category);
                    }
                }
            }
        } catch (JSONException ignored) {
            return defaultCategories();
        }

        ensureBuiltInCategories(categories);
        return categories;
    }

    static TodoCategory findCategory(Context context, String id) {
        for (TodoCategory category : loadCategories(context)) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return TodoCategory.builtIn(TodoCategory.INDOOR_ID, "室内", false, true);
    }

    static String categoryName(Context context, String id) {
        return findCategory(context, id).name;
    }

    static void addCategory(Context context, String name) {
        List<TodoCategory> categories = loadCategories(context);
        categories.add(TodoCategory.custom(name));
        saveCategories(context, categories);
    }

    static void setCategoryVisible(Context context, String id, boolean visible) {
        List<TodoCategory> categories = loadCategories(context);
        for (TodoCategory category : categories) {
            if (category.id.equals(id)) {
                category.showInWidget = visible;
                break;
            }
        }
        saveCategories(context, categories);
    }

    static List<TodoCategory> widgetCategories(Context context) {
        ArrayList<TodoCategory> result = new ArrayList<>();
        for (TodoCategory category : loadCategories(context)) {
            if (category.showInWidget) {
                result.add(category);
            }
        }
        return result;
    }

    static TodoItem find(Context context, String id) {
        for (TodoItem item : load(context)) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        return null;
    }

    static boolean complete(Context context, String id) {
        List<TodoItem> items = load(context);
        boolean changed = false;
        long now = System.currentTimeMillis();
        for (TodoItem item : items) {
            if (item.id.equals(id)) {
                if (item.periodic) {
                    item.addCheckIn(now);
                } else {
                    item.archived = true;
                    item.archivedAt = now;
                }
                changed = true;
                break;
            }
        }
        if (changed) {
            save(context, items);
        }
        return changed;
    }

    static boolean update(Context context, String id, String title, String categoryId, String duration) {
        List<TodoItem> items = load(context);
        boolean changed = false;
        TodoCategory category = findCategory(context, categoryId);
        for (TodoItem item : items) {
            if (item.id.equals(id)) {
                boolean wasPeriodic = item.periodic;
                item.title = title;
                item.categoryId = category.id;
                item.place = TodoCategory.OUTDOOR_ID.equals(category.id) ? TodoItem.PLACE_OUTDOOR : TodoItem.PLACE_INDOOR;
                item.duration = duration;
                item.periodic = category.periodic;
                if (!wasPeriodic && category.periodic) {
                    item.checkIns.clear();
                    item.lastDoneAt = 0L;
                }
                changed = true;
                break;
            }
        }
        if (changed) {
            save(context, items);
        }
        return changed;
    }

    static boolean restore(Context context, String id) {
        List<TodoItem> items = load(context);
        boolean changed = false;
        for (TodoItem item : items) {
            if (item.id.equals(id)) {
                item.archived = false;
                item.archivedAt = 0L;
                changed = true;
                break;
            }
        }
        if (changed) {
            save(context, items);
        }
        return changed;
    }

    static boolean delete(Context context, String id) {
        List<TodoItem> items = load(context);
        boolean changed = false;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).id.equals(id)) {
                items.remove(i);
                changed = true;
                break;
            }
        }
        if (changed) {
            save(context, items);
        }
        return changed;
    }

    static List<TodoItem> widgetItems(Context context, TodoCategory category, int limit) {
        List<TodoItem> all = load(context);
        ArrayList<TodoItem> result = new ArrayList<>();
        for (TodoItem item : all) {
            if (!item.archived && category.id.equals(item.categoryId)) {
                result.add(item);
            }
        }
        sortTasks(result);
        if (result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }
        return result;
    }

    static List<TodoItem> sorted(List<TodoItem> source) {
        ArrayList<TodoItem> result = new ArrayList<>(source);
        sortTasks(result);
        return result;
    }

    private static void sortTasks(List<TodoItem> items) {
        Collections.sort(items, new Comparator<TodoItem>() {
            @Override
            public int compare(TodoItem left, TodoItem right) {
                int duration = durationRank(left.duration) - durationRank(right.duration);
                if (duration != 0) {
                    return duration;
                }
                return Long.compare(right.createdAt, left.createdAt);
            }
        });
    }

    private static int durationRank(String duration) {
        return TodoItem.DURATION_LONG.equals(duration) ? 1 : 0;
    }

    private static ArrayList<TodoCategory> defaultCategories() {
        ArrayList<TodoCategory> categories = new ArrayList<>();
        categories.add(TodoCategory.builtIn(TodoCategory.INDOOR_ID, "室内", false, true));
        categories.add(TodoCategory.builtIn(TodoCategory.OUTDOOR_ID, "室外", false, true));
        categories.add(TodoCategory.builtIn(TodoCategory.PERIODIC_ID, "定期", true, true));
        return categories;
    }

    private static void ensureBuiltInCategories(List<TodoCategory> categories) {
        ensureBuiltInCategory(categories, TodoCategory.INDOOR_ID, "室内", false);
        ensureBuiltInCategory(categories, TodoCategory.OUTDOOR_ID, "室外", false);
        ensureBuiltInCategory(categories, TodoCategory.PERIODIC_ID, "定期", true);
    }

    private static void ensureBuiltInCategory(List<TodoCategory> categories, String id, String name, boolean periodic) {
        for (TodoCategory category : categories) {
            if (id.equals(category.id)) {
                category.name = name;
                category.builtIn = true;
                category.periodic = periodic;
                return;
            }
        }
        int index = TodoCategory.OUTDOOR_ID.equals(id) ? 1 : TodoCategory.PERIODIC_ID.equals(id) ? 2 : 0;
        categories.add(Math.min(index, categories.size()), TodoCategory.builtIn(id, name, periodic, true));
    }

    private static void save(Context context, List<TodoItem> items) {
        JSONArray array = new JSONArray();
        for (TodoItem item : items) {
            try {
                array.put(item.toJson());
            } catch (JSONException ignored) {
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ITEMS, array.toString())
                .apply();
    }

    private static void saveCategories(Context context, List<TodoCategory> categories) {
        JSONArray array = new JSONArray();
        for (TodoCategory category : categories) {
            try {
                array.put(category.toJson());
            } catch (JSONException ignored) {
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CATEGORIES, array.toString())
                .apply();
    }
}
