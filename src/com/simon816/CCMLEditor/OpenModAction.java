package com.simon816.CCMLEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.simon816.CCMLEditor.FileTypes.ModDist;

public class OpenModAction implements ActionListener {

    private CodeEditor ce;
    final JFileChooser fc = new JFileChooser();

    public OpenModAction(CodeEditor ce) {
        this.ce = ce;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fc.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                if (f.getAbsolutePath().endsWith(".mod")) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "All Mod Files (*.mod)";
            }

        });
        fc.setAcceptAllFileFilterUsed(false);
        int returnVal = fc.showOpenDialog(ce.getFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            ce.loadModDist(new ModDist(file));
        }
    }

}
