package com.boydti.fawe.installer;

import java.awt.event.ActionEvent;

public class CloseButton extends InteractiveButton {
    public CloseButton() {
        super("X");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.exit(0);
    }
}
