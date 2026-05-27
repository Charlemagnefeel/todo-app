package com.vibetodo.app;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class TodoItem {
    static final String PLACE_INDOOR = "indoor";
    static final String PLACE_OUTDOOR = "outdoor";
    static final String DURATION_SHORT = "short";
    static final String DURATION_LONG = "long";

    String id;
    String title;
    String categoryId;
    String place;
    String duration;
    boolean periodic;
    boolean archived;
    long createdAt;
    long archivedAt;
    long lastDoneAt;
    ArrayList<Long> checkIns = new ArrayList<>();

    static TodoItem create(String title, String categoryId, String duration, boolean periodic) {
        TodoItem item = new TodoItem();
        item.id = System.currentTimeMillis() + "-" + Math.round(Math.random() * 100000);
        item.title = title;
        item.categoryId = categoryId;
        item.place = TodoCategory.OUTDOOR_ID.equals(categoryId) ? PLACE_OUTDOOR : PLACE_INDOOR;
        item.duration = duration;
        item.periodic = periodic;
        item.archived = false;
        item.createdAt = System.currentTimeMillis();
        item.archivedAt = 0L;
        item.lastDoneAt = 0L;
        item.checkIns = new ArrayList<>();
        return item;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray checkInArray = new JSONArray();
        for (Long time : checkIns) {
            checkInArray.put(time);
        }
        json.put("id", id);
        json.put("title", title);
        json.put("categoryId", categoryId);
        json.put("place", place);
        json.put("duration", duration);
        json.put("periodic", periodic);
        json.put("archived", archived);
        json.put("createdAt", createdAt);
        json.put("archivedAt", archivedAt);
        json.put("lastDoneAt", lastDoneAt);
        json.put("checkIns", checkInArray);
        return json;
    }

    static TodoItem fromJson(JSONObject json) {
        TodoItem item = new TodoItem();
        item.id = json.optString("id");
        item.title = json.optString("title");
        item.place = json.optString("place", PLACE_INDOOR);
        item.duration = json.optString("duration", DURATION_SHORT);
        item.periodic = json.optBoolean("periodic", false);
        item.categoryId = json.optString("categoryId", "");
        if (item.categoryId.isEmpty()) {
            item.categoryId = item.periodic
                    ? TodoCategory.PERIODIC_ID
                    : PLACE_OUTDOOR.equals(item.place) ? TodoCategory.OUTDOOR_ID : TodoCategory.INDOOR_ID;
        }
        item.archived = json.optBoolean("archived", false);
        item.createdAt = json.optLong("createdAt", System.currentTimeMillis());
        item.archivedAt = json.optLong("archivedAt", 0L);
        item.lastDoneAt = json.optLong("lastDoneAt", 0L);
        item.checkIns = new ArrayList<>();
        JSONArray checkInArray = json.optJSONArray("checkIns");
        if (checkInArray != null) {
            for (int i = 0; i < checkInArray.length(); i++) {
                long time = checkInArray.optLong(i, 0L);
                if (time > 0L) {
                    item.checkIns.add(time);
                }
            }
        } else if (item.lastDoneAt > 0L) {
            item.checkIns.add(item.lastDoneAt);
        }
        if (!item.checkIns.isEmpty()) {
            item.lastDoneAt = item.checkIns.get(0);
        }
        return item;
    }

    void addCheckIn(long time) {
        checkIns.add(0, time);
        lastDoneAt = time;
    }

    int checkInCount() {
        return checkIns.size();
    }

    List<Long> recentCheckIns(int limit) {
        int end = Math.min(limit, checkIns.size());
        return new ArrayList<>(checkIns.subList(0, end));
    }

    String placeLabel() {
        return PLACE_OUTDOOR.equals(place) ? "室外" : "室内";
    }

    String durationLabel() {
        return DURATION_LONG.equals(duration) ? "长期" : "短期";
    }
}
