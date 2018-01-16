package com.boydti.fawe.nukkit.core.gui;

import cn.nukkit.form.window.FormWindow;
import java.util.List;
import java.util.function.Consumer;


import static com.google.common.base.Preconditions.checkNotNull;

public class ResponseFormWindow extends DelegateFormWindow {
    private final Consumer<List<String>> task;

    public ResponseFormWindow(FormWindow parent, Consumer<List<String>> onResponse) {
        super(parent);
        checkNotNull(onResponse);
        this.task = onResponse;
    }

    public void respond(List<String> response) {
        task.accept(response);
    }
}