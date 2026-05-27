package com.vibetodo.app;

import org.json.JSONException;
import org.json.JSONObject;

class TodoCategory {
    static final String INDOOR_ID = "indoor";
    static final String OUTDOOR_ID = "outdoor";
    static final String PERIODIC_ID = "periodic";

    String id;
    String name;
    boolean builtIn;
    boolean periodic;
    boolean showInWidget;
    long createdAt;

    static TodoCategory builtIn(String id, String name, boolean periodic, boolean showInWidget) {
        TodoCategory category = new TodoCategory();
        category.id = id;
        category.name = name;
        category.builtIn = true;
        category.periodic = periodic;
        category.showInWidget = showInWidget;
        category.createdAt = 0L;
        return category;
    }

    static TodoCategory custom(String name) {
        TodoCategory category = new TodoCategory();
        category.id = "custom-" + System.currentTimeMillis() + "-" + Math.round(Math.random() * 100000);
        category.name = name;
        category.builtIn = false;
        category.periodic = false;
        category.showInWidget = true;
        category.createdAt = System.currentTimeMillis();
        return category;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("builtIn", builtIn);
        json.put("periodic", periodic);
        json.put("showInWidget", showInWidget);
        json.put("createdAt", createdAt);
        return json;
    }

    static TodoCategory fromJson(JSONObject json) {
        TodoCategory category = new TodoCategory();
        category.id = json.optString("id");
        category.name = json.optString("name");
        category.builtIn = json.optBoolean("builtIn", false);
        category.periodic = json.optBoolean("periodic", false);
        category.showInWidget = json.optBoolean("showInWidget", true);
        category.createdAt = json.optLong("createdAt", 0L);
        return category;
    }
}
