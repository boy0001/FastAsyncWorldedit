package com.boydti.fawe.nukkit.core.gui;

import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;

public class DelegateFormWindow extends FormWindow {
    private final FormWindow parent;

    public DelegateFormWindow(FormWindow parent) {
        this.parent = parent;
    }
    @Override
    public String getJSONData() {
        return parent.getJSONData();
    }

    @Override
    public void setResponse(String s) {
        parent.setResponse(s);
    }

    @Override
    public FormResponse getResponse() {
        return parent.getResponse();
    }
}
