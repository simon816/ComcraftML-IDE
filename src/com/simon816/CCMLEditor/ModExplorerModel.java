package com.simon816.CCMLEditor;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.simon816.CCMLEditor.FileTypes.*;

class ModExplorerModel implements TreeModel {
    protected RootList root;
    private Vector<TreeModelListener> listeners = new Vector<TreeModelListener>();
    private JTree tree;

    public ModExplorerModel(File rootDirectory, JTree fileTree) {
        root = new RootList(rootDirectory);
        tree=fileTree;
    }

    @Override
    public FileBase getChild(Object parent, int index) {
        FileBase entry = (FileBase) parent;
        return entry.listChildren()[index];
    }

    @Override
    public int getChildCount(Object parent) {
        FileBase entry = (FileBase) parent;
        FileBase[] fileList = entry.listChildren();
        if (fileList != null) {
            return fileList.length;
        }
        return 0;
    }

    @Override
    public RootList getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((FileBase) node).isLeaf();
    }

    @Override
    public int getIndexOfChild(Object parent, Object c) {
        FileBase child = (FileBase) c;
        System.out.println("Get index of " + child + " in " + parent);
        if (parent == null || child == null) {
            return -1;
        }
        for (int i = 0; i < getChildCount(parent); i++) {
            FileBase pchild = getChild(parent, i);
            if (pchild.equals(child)) {
                return i;
            }
        }
        return -1;
    }

    public void addTreeModelListener(TreeModelListener listener) {
        System.out.println("Listner added:" + listener);
        listeners.add(listener);
    }

    public void removeTreeModelListener(TreeModelListener listener) {
        System.out.println("Listner removed:" + listener);
        listeners.remove(listener);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object value) {
        System.out.println("Value for " + path + " Changed to " + value);
    }

    private FileBase[] ListParents(FileBase node) {
        FileBase parent = node.getParent();
        Vector<FileBase> parents = new Vector<FileBase>();
        if (parent == null) {
            parent = node;
        }
        while (parent != null) {
            parents.addElement(parent);
            parent = parent.getParent();
        }
        Collections.reverse(parents);
        System.out.println(parents);
        return parents.toArray(new FileBase[parents.size()]);
    }

    public void InsertNode(FileBase child) {
        FileBase[] parents = ListParents(child);
        int[] childIndices = new int[1];
        childIndices[0] = getIndexOfChild(child.getParent(), child);
        treeNodeAction(parents, childIndices, new Object[] { child }, "INSERT");
    }

    public void InsertRefresh(FileBase child) {
        FileBase[] parents = ListParents(child);
        int[] childIndices = new int[1];
        childIndices[0] = getIndexOfChild(child.getParent(), child);
        treeNodeAction(parents, childIndices, new Object[] { child }, "STRUC");
    }

    public void refresh(FileBase child) {
        FileBase[] parents = ListParents(child);
        treeNodeAction(parents, null, new Object[] { child }, "STRUC");
        tree.expandRow(getIndexOfChild(child.getParent(), child));
    }

    public void RemoveNode(FileBase node) {
        FileBase parent = node.getParent();
        Object[] children = new Object[1];
        children[0] = node;
        int[] childIndices = new int[1];
        childIndices[0] = getIndexOfChild(parent, node);
        FileBase[] parents = ListParents(node);
        treeNodeAction(parents, childIndices, new Object[] { node }, "REMOVE");
    }

    private void treeNodeAction(Object[] path, int[] indices, Object[] children, String action) {
        TreeModelEvent event = new TreeModelEvent(this, path, indices, children);
        Iterator<TreeModelListener> iterator = listeners.iterator();
        TreeModelListener listener = null;
        while (iterator.hasNext()) {
            listener = (TreeModelListener) iterator.next();
            if (action.equals("REMOVE")) {
                listener.treeNodesRemoved(event);
            } else if (action.equals("CHANGE")) {
                listener.treeNodesChanged(event);
            } else if (action.equals("INSERT")) {
                listener.treeNodesInserted(event);
            } else if (action.equals("STRUC")) {
                listener.treeStructureChanged(event);
            }
        }
    }
}
