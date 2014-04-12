package com.simon816.CCMLEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.simon816.CCMLEditor.FileTypes.FileBase;
import com.simon816.CCMLEditor.FileTypes.ModProject;

public class PackageModAction implements ActionListener {

    private CodeEditor ce;

    public PackageModAction(CodeEditor ce) {
        this.ce = ce;
    }

    public void actionPerformed(ActionEvent e) {
        askPackage();
    }

    public void askPackage() {
        ArrayList<String> warns = new ArrayList<String>();
        if (!ce.SaveAll()) {
            warns.add("Not all files were able to save.");
        }
        try {
            ModProject mod = null;
            FileBase sel = ce.getSelectedFileTab();
            if (sel == null)
                sel = ce.TreeListener.getSelected();
            if (sel != null) {
                mod = sel.getModProject();
            }
            if (sel == null || mod == null) {
                JOptionPane.showMessageDialog(ce.getFrame(), "Cannot determine currently selected mod");
                return;
            }
            String[] combochoices = { "0.6 (API 6)", "0.5 (API 5)", "0.4 (API 4)", "0.3 (API 3)" };
            Object[] msgElements = { new JTextField("deployed\\" + mod + ".mod", 15), new JCheckBox("Attach Source Code"),
                    new JLabel("Comcraft Mod Loader Version Compatibility"), new JComboBox<Object>(combochoices) };

            int res = JOptionPane.showConfirmDialog(ce.getFrame(), msgElements, "Package Mod Options", JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.CLOSED_OPTION || res == JOptionPane.CANCEL_OPTION) {
                return;
            }
            String filename = ((JTextField) msgElements[0]).getText();
            if (filename.isEmpty()) {
                return;
            }
            File modfile = new File(mod.getAbsolutePath(), filename);
            boolean addSource = ((JCheckBox) msgElements[1]).isSelected();
            int API_LEVEL = (combochoices.length + 2) - ((JComboBox<?>) msgElements[3]).getSelectedIndex();
            if (API_LEVEL < 4) {
                if (addSource)
                    warns.add("Source not added as compatability is below API 4");
                warns.add("Long description not added as compatability is below API 4");
            }
            packageMod(API_LEVEL, addSource, mod, modfile);
            String warnings = "";
            Object[] warnsarr = warns.toArray();
            for (int i = 0; i < warnsarr.length; i++) {
                if (warnsarr[i] != null) {
                    warnings += "\nError: " + warnsarr[i];
                }
            }
            JOptionPane.showMessageDialog(ce.getFrame(), "Mod successfully deployed into this file:\n" + modfile.getAbsolutePath() + warnings);
            ce.getTreeModel().refresh(mod);// .InsertRefresh(new
                                           // FileBase(modfile).setParent(new
                                           // FolderBase(modfile.getParentFile()).setParent(mod)));
        } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(ce.getFrame(), "Error caught during mod packaging:\n" + e1.getMessage(), "Package Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void packageMod(int API_LEVEL, boolean addSource, ModProject mod, File modfile) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream block = new DataOutputStream(baos);
        block.write(new byte[] { 'C', 'C', 'M', 'L' }); // 4-byte head
        block.write(API_LEVEL);
        int flags = 0x00;
        if (addSource)
            flags += 0x01;
        block.write(flags);
        mod.serialize(block, API_LEVEL, addSource);
        block.close();
        byte[] buf = baos.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream o = new GZIPOutputStream(out);
        o.write(buf, 0, buf.length);
        o.finish();
        o.close();
        byte[] b = out.toByteArray();
        out.close();
        if (!modfile.getParentFile().exists()) {
            modfile.getParentFile().mkdir();
        }
        FileOutputStream f = new FileOutputStream(modfile);
        f.write(b, 0, b.length);
        f.close();
    }
}
