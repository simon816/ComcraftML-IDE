package com.simon816.CCMLEditor;

import java.awt.Component;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.simon816.CCMLEditor.FileTypes.*;
import com.simon816.CCMLEditor.FileTypes.Package;

public class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public CustomTreeCellRenderer() {
        super.setLeafIcon(new ImageIcon(CodeEditor.class.getResource("/images/file_obj.png")));
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        FileBase entry = (FileBase) value;
        setToolTipText(value.toString());
        URL imageURL = null;
        if (!leaf) {
            if (entry instanceof Package) {
                Package pkg = (Package) entry;
                if (pkg.isEmpty()) {
                    imageURL = CodeEditor.class.getResource("/images/empty_pack_obj.png");
                } else {
                    imageURL = CodeEditor.class.getResource("/images/package_obj.png");
                }
            } else if (entry instanceof ModProject) {
                imageURL = CodeEditor.class.getResource("/images/prj_obj.gif");
            } else if (entry instanceof SourceFolder || entry instanceof ResourceFolder) {
                imageURL = CodeEditor.class.getResource("/images/packagefolder_obj.png");
            }

        } else {
            if (entry instanceof ModSource) {
                imageURL = CodeEditor.class.getResource("/images/jcu_obj.png");
            } else if (entry instanceof ModBinary) {
                imageURL = CodeEditor.class.getResource("/images/classf_obj.png");
            } else if (entry instanceof ModDescriptor) {
                imageURL = CodeEditor.class.getResource("/images/template_obj.png");
            } else if (entry instanceof ModDist) {
                imageURL = CodeEditor.class.getResource("/images/document-binary-icon.png");
            }
        }
        if (imageURL != null) {
            setIcon(new ImageIcon(imageURL));
        }

        return this;
    }

}
