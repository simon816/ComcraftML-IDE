package com.simon816.CCMLEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SaveFileAction implements ActionListener {
    private CodeEditor ce;

    public SaveFileAction(CodeEditor ce) {
        this.ce = ce;
    }

    public void actionPerformed(ActionEvent e) {
        doSave(e.getActionCommand().equals("Save File"));
    }

    public Boolean doSave(Boolean statement) {
        if (statement) {
            return doSave();
        }
        return false;
    }

    public Boolean doSave() {
        int i = ce.getOpenFileIndex();
        return Save(i, ce.getFile(i));
    }

    public boolean Save(int i, File file) {
        try {
            OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            output.write(ce.getText(i));
            output.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}