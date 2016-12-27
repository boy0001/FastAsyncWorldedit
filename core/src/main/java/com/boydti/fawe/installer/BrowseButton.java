package com.boydti.fawe.installer;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JFileChooser;

public abstract class BrowseButton extends InteractiveButton {
    public BrowseButton() {
        super("Browse");
    }

    public abstract void onSelect(File folder);

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            onSelect(selectedFile);
        }
    }
}
