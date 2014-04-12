package com.simon816.CCMLEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import com.google.minijoe.compiler.Disassembler;
import com.simon816.CCMLEditor.FileTypes.*;
import com.simon816.CCMLEditor.FileTypes.FolderBase;
import com.simon816.CCMLEditor.FileTypes.ModProject;
import com.simon816.CCMLEditor.FileTypes.Package;
import com.simon816.CCMLEditor.ModExplorerModel;

class FileSelectionListner extends MouseAdapter implements TreeSelectionListener, ActionListener {
    protected JTree fileTree;
    protected CodeEditor ce;
    protected FileBase selected = null;

    public FileSelectionListner(JTree fileTree, CodeEditor ce) {
        this.fileTree = fileTree;
        this.ce = ce;
    }

    public void valueChanged(TreeSelectionEvent event) {

    }

    protected void onClick() {
        File file = new File(selected.getAbsolutePath());
        if (selected instanceof ModDescriptor) {
            ce.loadInfo(file, selected);
            return;
        }
        if (selected instanceof ModDist) {
            ce.loadModDist((ModDist) selected);
            return;
        }
        if (selected instanceof ModBinary) {
            ByteArrayOutputStream ostr = new ByteArrayOutputStream();
            PrintStream oldout = System.out;
            System.setOut(new PrintStream(ostr));
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new FileInputStream(file));
                new Disassembler(dis).dump();
                ostr.flush();
                ce.readOnlyString(ostr.toString(), file, selected);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.setOut(oldout);
            return;
        } else if (selected.isLeaf()) {
            if (selected instanceof ModSource) {
                ce.loadSource(file, selected);
            } else {
                ce.loadFile(file, selected);
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        // int selRow = fileTree.getRowForLocation(e.getX(), e.getY());
        TreePath selPath = fileTree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null) {
            fileTree.clearSelection();
        }
        selected = (FileBase) fileTree.getLastSelectedPathComponent();
        if (selected == null) {
            selected = ce.getTreeModel().getRoot();
        }
        if (e.getButton() == 1 && e.getClickCount() == 2) {
            onClick();
        }
        if (e.getButton() == 3) {
            System.out.println(selected + " " + selected.getClass());
            JPopupMenu popup = new JPopupMenu();
            JMenuItem refreshitem = new JMenuItem("Refresh");
            refreshitem.addActionListener(this);
            popup.add(refreshitem);
            if (!(selected instanceof ModDescriptor || selected instanceof SourceFolder || selected instanceof ResourceFolder || selected instanceof RootList)) {
                JMenuItem deleteitem = new JMenuItem("Delete");
                deleteitem.addActionListener(this);
                popup.add(deleteitem);
            }
            if (selected instanceof SourceFolder) {
                JMenuItem newpackageitem = new JMenuItem("New Package");
                newpackageitem.addActionListener(this);
                popup.add(newpackageitem);
            } else if (selected instanceof Package) {
                JMenu newsubmenu = new JMenu("New");
                JMenuItem newmodfileitem = new JMenuItem("Mod File");
                newmodfileitem.addActionListener(this);
                newsubmenu.add(newmodfileitem);
                popup.add(newsubmenu);
            } else if (selected instanceof ModProject) {
                JMenuItem packageitem = new JMenuItem("Package Mod");
                packageitem.addActionListener(this);
                popup.add(packageitem);
                JMenuItem fixitem = new JMenuItem("Fix Mod");
                fixitem.addActionListener(this);
                popup.add(fixitem);
            } else if (selected instanceof RootList) {
                JMenuItem newmoditem = new JMenuItem("New Mod");
                newmoditem.addActionListener(this);
                popup.add(newmoditem);
            } else if (selected instanceof ResourceFolder || selected instanceof FolderBase) {
                JMenuItem newfileitem = new JMenuItem("New File");
                newfileitem.addActionListener(this);
                popup.add(newfileitem);
                JMenuItem newfolderitem = new JMenuItem("New Folder");
                newfolderitem.addActionListener(this);
                popup.add(newfolderitem);
            }
            popup.show(fileTree, e.getX(), e.getY());
            return;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ModExplorerModel model = ce.getTreeModel();
        if (e.getActionCommand().equals("Refresh")) {
            model.refresh(selected);
        }
        if (e.getActionCommand().equals("New Package")) {
            String res = JOptionPane.showInputDialog(ce.getFrame(), "Package Name");
            if (res == null) {
                return;
            }
            if (res.isEmpty()) {
                return;
            }
            Package p = new Package(selected, res);
            model.InsertNode(p);

        }
        if (e.getActionCommand().equals("Mod File")) {
            String res = JOptionPane.showInputDialog(ce.getFrame(), "File Name");
            if (res == null) {
                return;
            }
            if (res.isEmpty()) {
                return;
            }
            res = res.replace('/', '_').replace('\\', '_');
            File f = new File(selected.getAbsolutePath(), res + ".cmod");
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            FileBase node = new ModSource(f).setParent(selected);
            ce.loadSource(f, node);
            model.InsertNode(node);
        }
        if (e.getActionCommand().equals("Delete")) {
            FileBase p = selected.getParent();
            model.RemoveNode(selected);
            File file = new File(selected.getAbsolutePath());
            if (file.isDirectory()) {
                deleteloop(file);
            }
            ce.closeIfOpen(file);
            file.delete();
            if (p instanceof Package) {
                if (p.listChildren().length == 0) {
                    ((Package) p).setEmpty();
                }
            }
            if (file.isFile()) {
                selected = null;
            }
        }
        if (e.getActionCommand().equals("New File")) {
            String name = JOptionPane.showInputDialog(ce.getFrame(), "File Name");
            if (name == null) {
                return;
            }
            if (name.isEmpty()) {
                return;
            }
            name = name.replace('/', '_').replace('\\', '_');
            File f = new File(selected.getAbsolutePath(), name);
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            FileBase node = new FileBase(f).setParent(selected);
            ce.loadFile(f, node);
            model.InsertNode(node);
        }
        if (e.getActionCommand().equals("New Folder")) {
            String name = JOptionPane.showInputDialog(ce.getFrame(), "Folder Name");
            if (name == null) {
                return;
            }
            if (name.isEmpty()) {
                return;
            }
            File f = new File(selected.getAbsolutePath(), name);
            f.mkdirs();
            FolderBase node = (FolderBase) new FolderBase(f).setParent(selected);
            model.InsertNode(node);
        }
        if (e.getActionCommand().equals("Package Mod")) {
            ce.packageMod();
        }
        if (e.getActionCommand().equals("New Mod")) {
            ce.newMod();
        }
        if (e.getActionCommand().equals("Fix Mod")) {
            File src = new File(selected.getModProject().getAbsolutePath(), "src");
            File res = new File(selected.getModProject().getAbsolutePath(), "res");
            File md = new File(selected.getModProject().getAbsolutePath(), "ccmod.info");
            if (!src.exists())
                src.mkdirs();
            if (!res.exists())
                res.mkdirs();
            if (!md.exists())
                try {
                    md.createNewFile();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            model.refresh(selected);
        }
    }

    private void deleteloop(File file) {
        String[] l = file.list();
        for (int i = l.length - 1; i >= 0; i--) {
            File tempFile = new File(file, l[i]);
            if (tempFile.isDirectory()) {
                deleteloop(tempFile);
            }
            tempFile.delete();
        }
    }

    public FileBase getSelected() {
        return selected;
    }
}