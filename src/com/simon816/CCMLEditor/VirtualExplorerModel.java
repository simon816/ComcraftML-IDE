package com.simon816.CCMLEditor;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import com.simon816.CCMLEditor.CodeEditor.TextEditor;
import com.simon816.CCMLEditor.FileTypes.FileBase;
import com.simon816.CCMLEditor.FileTypes.ModSource;
import com.simon816.CCMLEditor.FileTypes.ModDescriptor;
import com.simon816.CCMLEditor.FileTypes.Package;
import com.simon816.CCMLEditor.FileTypes.RootList;

public class VirtualExplorerModel extends ModExplorerModel {
    public VirtualExplorerModel(String path) {
        super(new File(path), null);
        root = new VirtualRoot(root);
    }

    public static class ResFile extends FileBase {

        private String path;
        private String data;
        private TextEditor te;

        public ResFile(String path, VirtualRoot root, String data, TextEditor textpane) {
            super(new File(path));
            root.add(this);
            this.path = path;
            this.data = data;
            this.te = textpane;
        }

        @Override
        public FileBase[] listChildren() {
            return new FileBase[0];
        }

        @Override
        public String toString() {
            return path;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

    }

    public static class VirtualRoot extends RootList {

        private Vector<FileBase> children = new Vector<FileBase>();;

        public VirtualRoot(RootList root) {
            super(root.realdest);
        }

        public void add(FileBase file) {
            children.add(file);
        }

        @Override
        public FileBase[] listChildren() {
            return children.toArray(new FileBase[children.size()]);
        }

    }

    public static class PkgDir extends Package {

        private Vector<ModSource> children = new Vector<ModSource>();

        public PkgDir(String rel, VirtualRoot root) {
            super(new File(""), "." + rel);
            root.add(this);
        }

        public void add(ModSource file) {
            children.add(file);
        }

        @Override
        public ModSource[] listChildren() {
            return children.toArray(new ModSource[children.size()]);
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

    }

    public static class ModFile extends ModSource {

        protected String code;
        private TextEditor te;

        public ModFile(String name, String scode, TextEditor textpane) {
            super(new File(name));
            this.code = scode;
            te = textpane;
        }

        public FileBase setParent(PkgDir parent) {
            super.setParent(parent);
            parent.add(this);
            return this;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }
    }

    public static class VirtualFileSelectionListner extends FileSelectionListner {

        public VirtualFileSelectionListner(JTree fileTree, CodeEditor ce) {
            super(fileTree, ce);
        }

        @Override
        protected void onClick() {
            if (selected instanceof ModFile) {
                ModFile mf = (ModFile) selected;
                mf.te.readText(mf.code);
                mf.te.setJavascript();
            }
            if (selected instanceof ResFile) {
                ResFile rf = (ResFile) selected;
                rf.te.readText(rf.data);
                rf.te.setPlain();
            }
            if (selected instanceof DeconstructInfo) {
                DeconstructInfo di = (DeconstructInfo) selected;
                di.te.readText(di.info);
                di.te.setPlain();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
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
        }

        public void TriggerClick() {
            selected = (FileBase) fileTree.getLastSelectedPathComponent();
            if (selected == null) {
                selected = ce.getTreeModel().getRoot();
            }
            onClick();
        }
    }

    public static class DeconstructInfo extends ModDescriptor {

        private String info;
        private TextEditor te;

        public DeconstructInfo(String info, TextEditor textpane) {
            super(new File(""));
            this.info = info;
            this.te = textpane;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public String toString() {
            return "Mod Meta Information";
        }
    }
}
