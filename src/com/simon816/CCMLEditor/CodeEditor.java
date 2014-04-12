package com.simon816.CCMLEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreeSelectionModel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.google.minijoe.compiler.Disassembler;
import com.simon816.CCMLEditor.FileTypes.FileBase;
import com.simon816.CCMLEditor.FileTypes.ModDist;
import com.simon816.CCMLEditor.FileTypes.ModDistHandler;
import com.simon816.CCMLEditor.FileTypes.ModProject;
import com.simon816.CCMLEditor.VirtualExplorerModel.DeconstructInfo;
import com.simon816.CCMLEditor.VirtualExplorerModel.*;

public class CodeEditor {
    private String version = "0.5.2";
    protected String wikiurl = "https://github.com/simon816/ComcraftModLoader/wiki";
    protected String homeurl = "http://simon816.hostzi.com/dev/ComcraftML/?IDE=" + version;

    private final int iconSize = 24;

    private ModExplorerModel fileSystemModel;
    private SaveFileAction saveAction = new SaveFileAction(this);
    private NewModAction newModAction = new NewModAction(this);
    private PackageModAction packageAction = new PackageModAction(this);
    private JButton[] EditorButtons = new JButton[6];
    private JFrame frame;
    private JTabbedPane tabpane;
    private String workspacedir;
    private JTree fileTree;
    private Vector<File> files = new Vector<File>(16);
    public FileSelectionListner TreeListener;

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("-package")) {
                if (args.length < 3) {
                    System.out.println("Package Usage:");
                    System.out.println("ComcraftML-IDE.jar -package project_source dest_file [OPTIONS]");
                    System.out.println("OPTIONS:");
                    System.out.println("  -api N         set API_LEVEL");
                    System.out.println("  -include_src   include source in mod file");
                } else {
                    int API_LEVEL = 6;
                    boolean INC_SRC = false;
                    for (int i = 3; i < args.length; i++) {
                        if (args[i].equals("-api")) {
                            API_LEVEL = Integer.parseInt(args[i + 1]);
                        } else if (args[i].equals("-include_src")) {
                            INC_SRC = true;
                        }
                    }
                    try {
                        PackageModAction.packageMod(API_LEVEL, INC_SRC, new ModProject(new File(args[1])), new File(args[2]));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.exit(0);
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                new CodeEditor();
            }
        });
    }

    private void makeWindow() {
        frame = new JFrame("Comcraft Mod IDE " + version);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (askSave()) {
                    frame.dispose();
                }
            }
        });
    }

    private File getWorkspaceDir() {

        String dir = System.getProperty("user.home");
        if (System.getProperty("os.name").startsWith("Windows")) {
            dir += "\\Documents";
        }
        dir = JOptionPane.showInputDialog("Workspace Directory:", dir + "\\Comcraft\\");
        if (dir == null || dir.equals("")) {
            frame.dispose();
            return null;
        }
        File workspacedir = new File(dir);
        if (!workspacedir.exists()) {
            workspacedir.mkdirs();
        }
        this.workspacedir = workspacedir.getAbsolutePath();
        return workspacedir;
    }

    private JTree makeTree(File workdir) {
        fileTree = new JTree();
        fileSystemModel = new ModExplorerModel(workdir, fileTree);
        fileTree.setModel(fileSystemModel);
        fileTree.setEditable(false);
        TreeListener = new FileSelectionListner(fileTree, this);
        fileTree.addTreeSelectionListener(TreeListener);
        fileTree.addMouseListener(TreeListener);
        fileTree.setCellRenderer(new CustomTreeCellRenderer());
        fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        BasicTreeUI ui = (BasicTreeUI) fileTree.getUI();
        ui.setExpandedIcon(new ImageIcon(CodeEditor.class.getResource("/images/Ex_icon.png")));
        ui.setCollapsedIcon(new ImageIcon(CodeEditor.class.getResource("/images/Co_icon.png")));
        javax.swing.ToolTipManager.sharedInstance().registerComponent(fileTree);
        fileTree.putClientProperty("JTree.lineStyle", "None");
        fileTree.setRootVisible(false);
        return fileTree;
    }

    private TextEditor makeTextPane(FileBase selected) {
        TextEditor textPane = new TextEditor(selected);
        textPane.setFoldIndicatorEnabled(true);
        return textPane;
    }

    private JTabbedPane makeTabPane() {
        final JTabbedPane pane = new JTabbedPane();
        pane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Component comp = pane.getSelectedComponent();
                EditorButtons[3].setEnabled(false);
                if (comp instanceof Editable) {
                    if (((Editable) comp).canSave()) {
                        EditorButtons[3].setEnabled(true);
                    }
                }
            }
        });
        return pane;
    }

    public CodeEditor() {
        makeWindow();
        JPanel master = new JPanel(new BorderLayout());
        JToolBar toolbar = new JToolBar();
        addButtons(toolbar);
        File workdir = getWorkspaceDir();
        if (workdir == null) {
            return;
        }
        JTree fileTree = makeTree(workdir);
        JScrollPane leftpane = new JScrollPane(fileTree);
        JPanel rightpane = new JPanel(new BorderLayout());
        tabpane = makeTabPane();
        rightpane.add(tabpane);

        JPanel welcomePane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        welcomePane.add(new JLabel("Welcome to the Comcraft Mod Loader IDE"), c);
        c.gridy = 1;
        welcomePane.add(new JLabel("For news, updates and more visit "), c);
        c.gridy = 2;
        welcomePane.add(makeLink(homeurl), c);
        c.gridy = 3;
        welcomePane.add(new JLabel("The Comcraft Mod Loader wiki is here:"), c);
        c.gridy = 4;
        welcomePane.add(makeLink(wikiurl), c);
        c.gridy = 5;
        welcomePane.add(new JLabel("And a tutorial of the IDE can be found here:"), c);
        c.gridy = 6;
        welcomePane.add(makeLink(wikiurl + "/Develop-using-ide"), c);
        addTab("Welcome", welcomePane);
        files.add(null);

        master.add(toolbar, BorderLayout.PAGE_START);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, leftpane, rightpane);
        splitPane.setDividerLocation(200);
        master.add(splitPane);
        frame.add(master);
        frame.pack();
        frame.setVisible(true);
        frame.setSize(1024, 576);
    }

    private JButton makeLink(final String url) {
        JButton button = new JButton();
        button.setText("<HTML><FONT color=\"#000099\"><U>" + url.substring(0, url.indexOf("?") != -1 ? url.indexOf("?") : url.length()) + "</U></FONT></HTML>");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setBackground(Color.WHITE);
        button.setToolTipText(url.toString());
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (IOException e1) {
                    } catch (URISyntaxException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }

        });
        return button;
    }

    private int addTab(String title, Component tab) {
        tabpane.addTab(title, tab);
        int i = tabpane.indexOfComponent(tab);
        tabpane.setTabComponentAt(i, new ButtonTabComponent(this, tabpane));
        tabpane.setSelectedIndex(i);
        return i;
    }

    private DescEditor makeInfoPanel(File file, FileBase selected) {
        DescEditor de = new DescEditor(selected);
        de.readFile(file);
        return de;
    }

    private void addButtons(JToolBar toolBar) {
        EditorButtons[0] = makeNavigationButton("New Mod", "Main_New_Project", newModAction);
        EditorButtons[1] = makeNavigationButton("Import Mod", "Import", new ImportModAction(this));
        EditorButtons[2] = makeNavigationButton("Open .mod File", "Open", new OpenModAction(this));
        EditorButtons[3] = makeNavigationButton("Save File", "Save", saveAction);
        EditorButtons[4] = makeNavigationButton("Package Mod", "Export", packageAction);
        EditorButtons[5] = makeNavigationButton("Help", "Help", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "To create a new mod, click New Mod and fill in the details\n"
                        + "Package your mod into a single .mod file with Package Mod.\n" + "Import a .mod file into your workspace by clicking Import Mod\n"
                        + "Right click on elements in the tree on the left to discover more features\nThere is a link to the tutorial on the Welcome tab.");
            }
        });
        for (int i = 0; i < EditorButtons.length; i++) {
            if (EditorButtons[i] != null)
                toolBar.add(EditorButtons[i]);
        }
    }

    private JButton makeNavigationButton(String label, String imageName, ActionListener action) {
        JButton button = new JButton();
        button.addActionListener(action);
        if (imageName != null) {
            String imgLocation = "/images/" + imageName + iconSize + ".gif";
            URL imageURL = CodeEditor.class.getResource(imgLocation);
            button.setIcon(new ImageIcon(imageURL));
        }
        button.setText(label);

        return button;
    }

    public boolean askSave(int i) {
        if (saveAble(i)) {
            File file = files.get(i);
            System.out.println("Asking save for " + file);
            int answer = JOptionPane.showConfirmDialog(frame, "Save File\n\"" + file.getAbsolutePath() + "\"?");
            if (answer == 0) {
                if (saveAction.Save(i, file)) {
                    System.out.println("Save Success");
                    return true;
                } else {
                    return false;
                }
            } else if (answer == 1) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean saveAble(int i) {
        return tabpane.getComponentAt(i) instanceof Editable && ((Editable) tabpane.getComponentAt(i)).canSave();
    }

    public boolean askSave() {
        for (int i = tabpane.getTabCount() - 1; i > 0; i--) {
            tabpane.setSelectedIndex(i);
            if (askSave(i)) {
                tabpane.remove(i);
                files.remove(i);
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean SaveAll() {
        for (int i = tabpane.getTabCount() - 1; i > 0; i--) {
            if (saveAble(i)) {
                if (!saveAction.Save(i, files.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public TextEditor loadFile(File file, FileBase selected) {
        if (files.contains(file)) {
            tabpane.setSelectedIndex(files.indexOf(file));
            return ((TextEditor) tabpane.getComponentAt(files.indexOf(file)));
        }
        TextEditor textPane = makeTextPane(selected);
        textPane.readFile(file);
        int i = addTab(file.getName(), textPane);
        while (files.size() < i) {
            files.add(null);
        }
        files.add(i, file);
        return textPane;
    }

    public void loadSource(File file, FileBase selected) {
        TextEditor te = loadFile(file, selected);
        te.setJavascript();
    }

    public String getText(int i) {
        Component comp = tabpane.getComponentAt(i);
        if (comp instanceof TextEditor) {
            return ((TextEditor) (comp)).getTextArea().getText();
        } else if (comp instanceof DescEditor) {
            return ((DescEditor) (comp)).getText();
        }
        return null;
    }

    public void loadInfo(File file, FileBase selected) {
        if (files.contains(file)) {
            tabpane.setSelectedIndex(files.indexOf(file));
            return;
        }
        int i = addTab("Mod Descriptor", makeInfoPanel(file, selected));
        while (files.size() < i) {
            files.add(null);
        }
        files.add(i, file);
    }

    public void setFileName(String name) {
        // this.fileLabel.setText(file.getAbsolutePath());

    }

    public TextEditor readString(String text, File file, FileBase selected) {
        if (files.contains(file)) {
            tabpane.setSelectedIndex(files.indexOf(file));
            return (TextEditor) tabpane.getComponentAt(tabpane.getSelectedIndex());
        }
        TextEditor textPane = makeTextPane(selected);
        textPane.setEditable(false);
        textPane.readText(text);
        int i = addTab(file.getName(), textPane);
        while (files.size() < i) {
            files.add(null);
        }
        files.add(i, file);
        return textPane;
    }

    public void readOnlyString(String text, File file, FileBase selected) {
        readString(text, file, selected);
        Component c = tabpane.getSelectedComponent();
        if (c instanceof TextEditor) {
            ((TextEditor) c).savestate = false;
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    public String getWorkspace() {
        return workspacedir;
    }

    public ModExplorerModel getTreeModel() {
        return (ModExplorerModel) fileTree.getModel();
    }

    public TreeSelectionListener[] getTreeListeners() {
        return fileTree.getTreeSelectionListeners();
    }

    public void removeTab(int i) {
        if (tabpane.getComponentAt(i) instanceof Editable) {
            if (askSave(i)) {
                tabpane.remove(i);
                System.out.println("Removing file " + i + " " + files.get(i));
                files.remove(i);
            }
        } else {
            tabpane.remove(i);
            files.remove(i); // Will be a null
        }
    }

    public void closeIfOpen(File f) {
        if (files.contains(f)) {
            int i = files.indexOf(f);
            tabpane.remove(i);
            files.remove(i);
        }
    }

    public int getOpenFileIndex() {
        return tabpane.getSelectedIndex();
    }

    public File getFile(int i) {
        return files.get(i);
    }

    public void packageMod() {
        packageAction.askPackage();
    }

    public void newMod() {
        newModAction.newMod();
    }

    private interface Editable {
        public boolean canSave();

        public void readFile(File file);

        public String getText();

        public FileBase getFile();
    }

    protected class TextEditor extends RTextScrollPane implements Editable {

        private static final long serialVersionUID = 1L;
        private RSyntaxTextArea textarea;
        private boolean savestate = true;
        private FileBase file;

        public TextEditor(FileBase selected) {
            super(new RSyntaxTextArea(15, 40));
            textarea = (RSyntaxTextArea) getTextArea();
            textarea.setCodeFoldingEnabled(true);
            textarea.setAntiAliasingEnabled(true);
            textarea.requestFocusInWindow();
            file = selected;
        }

        public void setJavascript() {
            textarea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        }

        public void setPlain() {
            textarea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }

        @Override
        public FileBase getFile() {
            return file;
        }

        @Override
        public void readFile(File file) {
            try {
                InputStreamReader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
                textarea.read(r, null);
                r.close();
            } catch (IOException ioe) {
                textarea.setText("");
            }
        }

        public void readText(String text) {
            textarea.setText(text);
        }

        @Override
        public String getText() {
            return textarea.getText();
        }

        public void setEditable(boolean edit) {
            textarea.setEditable(edit);
        }

        @Override
        public boolean canSave() {
            return savestate;
        }

    }

    private class DescEditor extends JPanel implements Editable {

        private static final long serialVersionUID = 1L;
        private JTextField Titletext;
        private JTextField Infotext;
        private JTextField Classtext;
        private JTextArea Desctext;
        private FileBase file;

        public DescEditor(FileBase selected) {
            super(new GridBagLayout());
            Titletext = new JTextField(20);
            Infotext = new JTextField(20);
            Classtext = new JTextField(20);
            Desctext = new JTextArea(6, 20);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(10, 0, 0, 0);
            c.anchor = GridBagConstraints.EAST;
            c.gridy = 0;
            c.gridx = 0;
            add(new JLabel("Mod Title "), c);
            c.gridx = 1;
            add(Titletext, c);
            c.gridy = 1;
            c.gridx = 0;
            add(new JLabel("Mod Short Info"), c);
            c.gridx = 1;
            add(Infotext, c);
            c.gridy = 2;
            c.gridx = 0;
            add(new JLabel("Main Class"), c);
            c.gridx = 1;
            add(Classtext, c);
            c.gridy = 3;
            c.gridx = 0;
            add(new JLabel("Mod Full Description"), c);
            c.gridx = 1;
            add(Desctext, c);
            file = selected;
        }

        @Override
        public void readFile(File file) {
            try {
                BufferedReader f = new BufferedReader(new FileReader(file));
                Titletext.setText(f.readLine());
                Infotext.setText(f.readLine());
                Classtext.setText(f.readLine());
                String text = "";
                while (f.ready()) {
                    text += f.readLine() + "\n";
                }
                Desctext.setText(text);
                f.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String getText() {
            return Titletext.getText() + "\n" + Infotext.getText() + "\n" + Classtext.getText() + "\n" + Desctext.getText() + "\n";
        }

        @Override
        public boolean canSave() {
            return true;
        }

        @Override
        public FileBase getFile() {
            return file;
        }
    }

    private class ModBrowser extends JPanel {
        private static final long serialVersionUID = 1L;
        private CodeEditor ce;
        private ModDist mod;

        public ModBrowser(CodeEditor ce) {
            super(new GridBagLayout());
            this.ce = ce;
        }

        public ModBrowser browse(ModDist mod) {
            this.mod = mod;
            VirtualExplorerModel model = new VirtualExplorerModel(mod.getAbsolutePath());
            final VirtualRoot root = (VirtualRoot) model.getRoot();
            final TextEditor textpane = makeTextPane(mod);
            textpane.setEditable(false);
            textpane.savestate = false;
            class hdler extends ModDistHandler {
                private PkgDir cpkg;
                public int ver;
                private int flags;
                private String desc;

                @Override
                public void handleResource(String filename, String data) {
                    new ResFile(filename, root, data, textpane);
                }

                @Override
                public void handleModFile(String pkg, String name, String scode, byte[] binary) {
                    if (scode == null) {
                        // ByteArrayOutputStream ostr = new ByteArrayOutputStream();
                        // PrintStream oldout = System.out;
                        // System.setOut(new PrintStream(ostr));
                        DataInputStream dis = null;
                        try {
                            dis = new DataInputStream(new ByteArrayInputStream(binary));
                            scode = (new Decompiler(dis)).decompile();
                            // new Disassembler(dis).dump();
                            // ostr.flush();
                            // scode = "// Source code not attached, here's a binary dump\n" + ostr.toString();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        // System.setOut(oldout);
                    }
                    new ModFile(name, scode, textpane).setParent(cpkg);
                }

                @Override
                public void HandleVer(int ver) {
                    this.ver = ver;
                }

                @Override
                public void handleUnknown(int opt) {
                }

                @Override
                public void handlePackage(String pkgname) {
                    cpkg = new PkgDir(pkgname, root);
                }

                @Override
                public void handleDescriptor(String modname, String modinfo, String main, String desc) {
                    this.desc = "MOD NAME: " + modname + "\nMOD INFO: " + modinfo + "\nMAIN CLASS: " + main + "\nDESCRIPTION: " + desc;
                }

                @Override
                public void handleFlags(int flags) {
                    this.flags = flags;
                }
            }
            ;
            hdler h = new hdler();
            mod.decompile(h);

            String info = "Comcraft Mod Loader API version " + h.ver + "\nOptions: ";
            Object[][] flags = new Object[][] { { new Integer(0x01), "Attach Source Code" } };
            for (int i = 0; i < flags.length; i++) {
                int f = ((Integer) flags[i][0]).intValue();
                if ((h.flags & f) == f) {
                    info += flags[i][1] + ", ";
                }
            }
            if (h.flags == 0x00) {
                info += "None";
            }
            info += "\n" + h.desc;
            DeconstructInfo metainf = new DeconstructInfo(info.trim(), textpane);
            root.add(metainf);
            JTree tree = new JTree(model);
            tree.setEditable(false);
            VirtualFileSelectionListner tl = new VirtualFileSelectionListner(tree, ce);
            tree.addTreeSelectionListener(tl);
            tree.addMouseListener(tl);
            tree.setCellRenderer(new CustomTreeCellRenderer());
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            BasicTreeUI ui = (BasicTreeUI) tree.getUI();
            ui.setExpandedIcon(new ImageIcon(CodeEditor.class.getResource("/images/Ex_icon.png")));
            ui.setCollapsedIcon(new ImageIcon(CodeEditor.class.getResource("/images/Co_icon.png")));
            javax.swing.ToolTipManager.sharedInstance().registerComponent(tree);
            tree.putClientProperty("JTree.lineStyle", "None");
            tree.setRootVisible(false);
            tree.setSelectionRow(model.getIndexOfChild(root, metainf) + 1);
            tl.TriggerClick();
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, tree, textpane);
            splitPane.setDividerLocation(200);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = gbc.weighty = 1.0;
            add(splitPane, gbc);
            return this;
        }
    }

    public void loadModDist(ModDist mod) {
        for (int i = 0; i < tabpane.getTabCount(); i++) {
            Component t;
            if ((t = tabpane.getComponentAt(i)) instanceof ModBrowser && ((ModBrowser) t).mod.equals(mod)) {
                tabpane.setSelectedIndex(i);
                return;
            }
        }
        addTab(mod.toString(), new ModBrowser(this).browse(mod));
        files.add(null);
    }

    public FileBase getSelectedFileTab() {
        Component c = tabpane.getSelectedComponent();
        if (c instanceof Editable) {
            return ((Editable) c).getFile();
        }
        return null;
    }
}
