package com.simon816.CCMLEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.simon816.CCMLEditor.FileTypes.ModProject;

public class NewModAction implements ActionListener {

    private CodeEditor ce;

    public NewModAction(CodeEditor ce) {
        this.ce = ce;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        newMod();
    }

    public void newMod() {
        JTextField modNameField = new JTextField();
        Object result = JOptionPane.showInputDialog(ce.getFrame(), new Object[] { "Mod Name", modNameField, "Main 'Class'" }, "New Mod",
                JOptionPane.PLAIN_MESSAGE);
        if (result == null) {
            return;
        }
        String modName = modNameField.getText();
        String main = (String) result;
        if (main.isEmpty() || modName.isEmpty()) {
            return;
        }
        newMod(modName, "Version 1.0 by <author>", main);
    }

    public void newMod(String modName, String desc, String main) {

        File folder = new File(ce.getWorkspace(), modName);
        int i = 1;
        while (folder.exists()) {
            folder = new File(ce.getWorkspace(), modName + " (" + (++i) + ")");
        }
        folder.mkdirs();
        String m = main.replace('.', '/');
        if (m.equals(main)) {
            m = "default/package/" + m;
            main = "default.package." + main;
        }
        String pkg = m.substring(0, m.lastIndexOf('/'));
        File p = new File(folder, "src/" + pkg);
        p.mkdirs();
        File info = new File(folder, "ccmod.info");
        new File(folder, "res").mkdirs();
        FileWriter fw;
        try {
            fw = new FileWriter(info);
            fw.write(modName + "\n" + desc + "\n" + main);
            fw.close();
            File mainfile = new File(p, m.substring(pkg.length()) + ".cmod");
            mainfile.createNewFile();
            FileWriter mfw = new FileWriter(mainfile);
            mfw.write("Console.log(\"" + modName + " is Activated!\")\n");
            mfw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        ModExplorerModel model = ce.getTreeModel();
        ModProject mod = (ModProject) new ModProject(folder).setParent(model.getRoot());
        model.InsertNode(mod);
        ce.loadInfo(info, mod);
    }

}
