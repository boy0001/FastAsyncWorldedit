package com.boydti.fawe.installer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.JTextArea;

public class TextAreaOutputStream extends PrintStream {

    public TextAreaOutputStream(final JTextArea textArea) {
        super(new OutputStream() {
            private StringBuffer buffer = new StringBuffer();
            private String newLine = "";

            @Override
            public void write(int b) throws IOException {
                if (b != '\n') {
                    buffer.append((char) b);
                } else {
                    textArea.setText(buffer + newLine + textArea.getText());
                    newLine = "\n";
                    buffer.delete(0, buffer.length());
                    textArea.setVisible(true);
                    textArea.repaint();
                }
            }
        });
    }
}
