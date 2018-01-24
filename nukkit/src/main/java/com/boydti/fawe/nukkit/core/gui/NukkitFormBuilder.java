package com.boydti.fawe.nukkit.core.gui;

import cn.nukkit.Player;
import cn.nukkit.form.element.Element;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementSlider;
import cn.nukkit.form.element.ElementStepSlider;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.gui.FormBuilder;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


import static com.google.common.base.Preconditions.checkNotNull;

public class NukkitFormBuilder implements FormBuilder<Player> {
    private Consumer<Map<Integer, Object>> response;
    private final List<Element> elements;
    private final List<ElementButton> buttons;
    private String title = "";
    private String icon;

    public NukkitFormBuilder() {
        this.elements = new ArrayList<>();
        this.buttons = new ArrayList<>();
    }

    @Override
    public FormBuilder setTitle(String text) {
        checkNotNull(text);
        this.title = text;
        return this;
    }

    @Override
    public FormBuilder setIcon(URL icon) {
        checkNotNull(icon);
        this.icon = icon.toString();
        return this;
    }

    @Override
    public FormBuilder addButton(String text, URL image) {
        checkNotNull(text);
        if (!elements.isEmpty()) throw new UnsupportedOperationException("GUI does not support mixed buttons / elements");
        ElementButton button;
        if (image != null) {
            ElementButtonImageData imageData = new ElementButtonImageData("url", image.toString());
            button = new ElementButton(text, imageData);
        } else {
            button = new ElementButton(text);
        }
        buttons.add(button);
        return this;
    }

    @Override
    public FormBuilder addDropdown(String text, int def, String... options) {
        checkNotNull(text);
        checkNotNull(options);
        for (String option : options) checkNotNull(option);
        if (!buttons.isEmpty()) throw new UnsupportedOperationException("GUI does not support mixed buttons / elements");

        elements.add(new ElementDropdown(text, Arrays.asList(options), def));
        return this;
    }

    @Override
    public FormBuilder addInput(String text, String placeholder, String def) {
        checkNotNull(text);
        checkNotNull(placeholder);
        checkNotNull(def);
        if (!buttons.isEmpty()) throw new UnsupportedOperationException("GUI does not support mixed buttons / elements");

        elements.add(new ElementInput(text, placeholder, def));
        return this;
    }

    @Override
    public FormBuilder addLabel(String text) {
        checkNotNull(text);
        if (!buttons.isEmpty()) throw new UnsupportedOperationException("GUI does not support mixed buttons / elements");

        elements.add(new ElementLabel(text));
        return this;
    }

    @Override
    public FormBuilder addSlider(String text, double min, double max, int step, double def) {
        checkNotNull(text);
        if (!buttons.isEmpty()) throw new UnsupportedOperationException("GUI does not support mixed buttons / elements");

        elements.add(new ElementSlider(text, (float) min, (float) max, step, (float) def));
        return this;
    }

    @Override
    public FormBuilder addStepSlider(String text, int def, String... options) {
        checkNotNull(text);
        checkNotNull(options);
        for (String option : options) checkNotNull(option);
        if (!buttons.isEmpty()) throw new UnsupportedOperationException("GUI does not support mixed buttons / elements");

        elements.add(new ElementStepSlider(text, Arrays.asList(options), def));
        return this;
    }

    @Override
    public FormBuilder addToggle(String text, boolean def) {
        checkNotNull(text);
        if (!buttons.isEmpty()) throw new UnsupportedOperationException("GUI does not support mixed buttons / elements");

        elements.add(new ElementToggle(text, def));
        return this;
    }

    @Override
    public FormBuilder setResponder(Consumer<Map<Integer, Object>> handler) {
        this.response = handler;
        return this;
    }

    @Override
    public void display(FawePlayer<Player> fp) {
        FormWindow window;
        if (buttons.isEmpty()) {
            if (icon == null) {
                window = new FormWindowCustom(title, elements);
            } else {
                window = new FormWindowCustom(title, elements, icon);
            }
        } else {
            window = new FormWindowSimple(title, "", buttons);
        }

        if (response != null) {
            window = new ResponseFormWindow(window, response);
        }


        Player player = fp.parent;
        player.showFormWindow(window);
    }
}
