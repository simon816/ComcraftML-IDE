package com.simon816.CCMLEditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import com.google.minijoe.compiler.CompilerException;
import com.google.minijoe.compiler.Eval;

public class FileTypes {

    private static FileBase makeInstanceFor(File realdest, String filename) {
        File file = new File(realdest, filename);
        if (file.isFile()) {
            if (filename.endsWith(".cmod")) {
                return new ModSource(file);
            }
            if (filename.endsWith(".ccm")) {
                return new ModBinary(file);
            }
            if (filename.equalsIgnoreCase("ccmod.info")) {
                return new ModDescriptor(file);
            }
            return new FileBase(file);
        } else {
            if (filename.equalsIgnoreCase("src")) {
                return new SourceFolder(file);
            }
            if (filename.equalsIgnoreCase("res")) {
                return new ResourceFolder(file);
            }
            return new FolderBase(file);
        }
    }

    public static class FileBase {
        protected File realdest;
        private FileBase parent = null;

        public FileBase(File realdest) {
            this.realdest = new File(realdest.getAbsolutePath());
        }

        public FileBase getParent() {
            return parent;
        }

        public String toString() {
            return realdest.getName();
        }

        public FileBase[] listChildren() {
            return null;
        }

        public boolean isLeaf() {
            return true;
        }

        public String getAbsolutePath() {
            return realdest.getAbsolutePath();
        }

        public FileBase setParent(FileBase parent) {
            this.parent = parent;
            return this;
        }

        public boolean equals(FileBase o) {
            return o.getAbsolutePath().equals(this.getAbsolutePath());
        }

        protected String getRelPath() {
            return getAbsolutePath().substring(getModProject().getAbsolutePath().length());
        }

        public void serialize(DataOutputStream stream, int api) throws IOException {
            BufferedReader br = new BufferedReader(new FileReader(realdest));
            String path = getRelPath();
            if (api >= 4) {
                path = path.replace('\\', '/').replace("/res", "");
            }
            stream.writeUTF(path);
            String data = "";
            while (br.ready()) {
                data += br.readLine() + "\n";
            }
            stream.writeUTF(data);
            br.close();
        }

        public ModProject getModProject() {
            FileBase p = this;
            while (!(p instanceof ModProject)) {
                if (p == null || p instanceof RootList) {
                    return null;
                }
                p = p.getParent();
            }
            return (ModProject) p;
        }
    }

    public static class FolderBase extends FileBase {

        public FolderBase(File realdest) {
            super(realdest);
        }

        public boolean isLeaf() {
            return false;
        }

        public FileBase[] listChildren() {
            String[] list = realdest.list();
            FileBase[] o = new FileBase[list.length];
            for (int i = 0; i < list.length; i++) {
                File f = new File(realdest, list[i]);
                if (f.isFile()) {
                    o[i] = new FileBase(f);
                    if (list[i].endsWith(".mod")) {
                        o[i] = new ModDist(f);
                    }
                } else {
                    o[i] = new FolderBase(f);
                }
                o[i].setParent(this);
            }
            return o;
        }

        public void serialize(DataOutputStream stream, int api) throws IOException {

        }
    }

    public static class RootList extends FolderBase {

        public RootList(File realdest) {
            super(realdest);
        }

        public File getRoot() {
            return realdest.getAbsoluteFile();
        }

        public String toString() {
            return realdest.getAbsolutePath();
        }

        public FileBase[] listChildren() {
            String[] files = realdest.list();
            if (files == null) {
                return null;
            }
            ModProject[] Tempmods = new ModProject[files.length];
            int x = 0;
            for (int i = 0; i < files.length; i++) {
                File cmodinf = new File(realdest, files[i] + "\\ccmod.info");
                if (cmodinf.exists()) {
                    Tempmods[x++] = (ModProject) new ModProject(new File(realdest, files[i])).setParent(this);
                }
            }
            ModProject[] mods = new ModProject[x];
            System.arraycopy(Tempmods, 0, mods, 0, x);
            return mods;
        }

    }

    public static class ModProject extends FolderBase {

        public ModProject(File realdest) {
            super(realdest);
        }

        public FileBase[] listChildren() {
            FileBase[] files = super.listChildren();
            FileBase[] out = new FileBase[files.length];
            for (int i = 0; i < files.length; i++) {
                out[i] = makeInstanceFor(realdest, files[i].toString()).setParent(this);
            }
            return out;
        }

        public void serialize(DataOutputStream stream, int api, boolean addSource) throws IOException {
            FileBase[] children = listChildren();
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof SourceFolder) {
                    ((SourceFolder) children[i]).serialize(stream, api, addSource);
                } else {
                    children[i].serialize(stream, api);
                }
            }
        }
    }

    public static class SourceFolder extends FolderBase {
        public static final int DATA_CODE = 0x10;

        public SourceFolder(File realdest) {
            super(realdest);
            this.realdest = realdest;
        }

        private void iterateover(Vector<Package> d, String rel) {
            Package p = new Package(new File(realdest, rel), rel);
            p.setParent(this);
            File a = new File(realdest + "\\" + rel);
            String[] list = a.list();
            if (list.length == 0 && !rel.isEmpty()) {
                p.setEmpty();
                d.addElement(p);
                return;
            }
            for (int i = 0; i < list.length; i++) {
                File c = new File(a, list[i]);
                if (c.isDirectory()) {
                    iterateover(d, rel + "\\" + list[i]);
                } else if (!rel.isEmpty()) {
                    if (d.size() > 0) {
                        if (d.lastElement().equals(p)) {
                            continue;
                        }
                    }
                    d.addElement(p);
                }
            }
        }

        public Package[] listChildren() {
            Vector<Package> d = new Vector<Package>();
            iterateover(d, "");
            return (Package[]) d.toArray(new Package[d.size()]);
        }

        public void serialize(DataOutputStream stream, int api, boolean addSource) throws IOException {
            Package[] packages = listChildren();
            stream.write(DATA_CODE);
            stream.write(packages.length);
            for (int i = 0; i < packages.length; i++) {
                packages[i].serialize(stream, api, addSource);
            }
        }
    }

    public static class Package extends FolderBase {
        protected String pkgname;
        private int state;

        public Package(File realdest, String rel) {
            super(realdest);
            state = 1;
            if (rel.isEmpty()) {
                pkgname = "default.package";
            } else {
                pkgname = rel.substring(1).replace('\\', '.');
            }
        }

        public Package(FileBase rootdest, String pkgname) {
            this(new File(rootdest.getAbsolutePath(), pkgname.replace('.', '\\')), "$" + pkgname);
            setParent(rootdest);
            realdest.mkdirs();
            setEmpty();
        }

        public void setEmpty() {
            state = 2;
        }

        public void setNotEmpty() {
            state = 1;
        }

        public boolean isEmpty() {
            return state == 2;
        }

        public boolean equals(Package p) {
            return p.pkgname.equals(this.pkgname);
        }

        public File getRealFile() {
            return realdest.getAbsoluteFile();
        }

        public String toString() {
            return pkgname;
        }

        public ModSource[] listChildren() {
            if (realdest.isDirectory()) {
                String[] list = realdest.list();
                ModSource[] o = new ModSource[list.length];
                for (int i = 0; i < list.length; i++) {
                    o[i] = (ModSource) makeInstanceFor(realdest, list[i]);
                    o[i].setParent(this);
                }
                return o;
            }
            return null;
        }

        public void serialize(DataOutputStream stream, int api, boolean addSource) throws IOException {
            stream.writeUTF(pkgname);
            ModSource[] files = listChildren();
            stream.write(files.length);
            for (int i = 0; i < files.length; i++) {
                files[i].serialize(stream, api, addSource);
            }
        }
    }

    public static class ModSource extends FileBase {

        public ModSource(File realdest) {
            super(realdest);
        }

        public FileBase setParent(FileBase parent) {
            if (parent instanceof Package) {
                ((Package) parent).setNotEmpty();
            }
            return super.setParent(parent);
        }

        public void serialize(DataOutputStream stream, int api, boolean addSource) throws IOException {
            stream.writeUTF(toString().substring(0, toString().length() - ".cmod".length()));
            try {
                String text = getText();
                if (addSource && api >= 4) {
                    stream.writeUTF(text);
                }
                byte[] data = compile(text);
                stream.writeInt(data.length);
                stream.write(data);
            } catch (CompilerException e) {
                e.printStackTrace();
                stream.writeInt(0);
                throw new IOException(e);
            }
        }

        private byte[] compile(String text) throws CompilerException, IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(baos);
            Eval.compile(text, output);
            output.close();
            return baos.toByteArray();

        }

        private String getText() throws IOException {
            String text = "";
            BufferedReader br = new BufferedReader(new FileReader(realdest));
            while (br.ready()) {
                text += br.readLine() + "\n";
            }
            br.close();
            return text;
        }
    }

    public static class ModDescriptor extends FileBase {
        public static final int DATA_CODE = 0x20;

        public ModDescriptor(File realdest) {
            super(realdest);
        }

        public void serialize(DataOutputStream stream, int api) throws IOException {
            stream.write(DATA_CODE);
            BufferedReader br = new BufferedReader(new FileReader(new File(getAbsolutePath())));
            stream.writeUTF(br.readLine());
            stream.writeUTF(br.readLine());
            String pkgname = br.readLine();
            stream.writeUTF(pkgname);
            String desc = "";
            if (api >= 4) {
                while (br.ready()) {
                    desc += br.readLine() + "\n";
                }
                stream.writeUTF(desc.trim());
            }
            br.close();
            if (!new File(this.getModProject().getAbsolutePath(), "src/" + (pkgname.replace('.', '/') + ".cmod")).exists()) {
                throw new IOException("Main Class does not exist!");
            }
        }

        public String toString() {
            return "Mod Descriptor";
        }
    }

    public static class ModBinary extends FileBase {

        public ModBinary(File realdest) {
            super(realdest);
            // TODO Auto-generated constructor stub
        }

        public void serialize(DataOutputStream stream, int api) throws IOException {

        }
    }

    public static class ResourceFolder extends FolderBase {
        public static final int DATA_CODE = 0x30;

        public ResourceFolder(File realdest) {
            super(realdest);
        }

        private void iterateover(Vector<FileBase> d, String rel) {
            File a = new File(realdest, rel);
            String[] list = a.list();
            if (list.length == 0) {
                return;
            }
            for (int i = 0; i < list.length; i++) {
                File c = new File(a, list[i]);
                if (c.isDirectory()) {
                    iterateover(d, rel + "\\" + list[i]);
                } else {
                    d.add(new FileBase(c).setParent(this));
                }
            }
        }

        public void serialize(DataOutputStream stream, int api) throws IOException {
            stream.write(DATA_CODE);
            Vector<FileBase> d = new Vector<FileBase>();
            iterateover(d, "");
            FileBase[] files = d.toArray(new FileBase[d.size()]);
            stream.write(files.length);
            for (int i = 0; i < files.length; i++) {
                files[i].serialize(stream, api);
            }
        }
    }

    public static class ModDist extends FileBase {
        public ModDist(File realdest) {
            super(realdest);
        }

        public String decompile(ModDistHandler h) {
            GZIPInputStream gz;
            String text = "";
            try {
                gz = new GZIPInputStream(new FileInputStream(realdest));
                DataInputStream dis = new DataInputStream(gz);
                byte[] b = new byte[4];
                dis.read(b, 0, 4);
                assert b == new byte[] { 'C', 'C', 'M', 'L' };
                int ver = dis.read();
                h.HandleVer(ver);
                int flags = dis.read();
                h.handleFlags(flags);
                while (dis.available() > 0) {
                    int opt = dis.read();
                    switch (opt) {
                    case ModDescriptor.DATA_CODE:
                        String name = dis.readUTF();
                        String info = dis.readUTF();
                        String main = dis.readUTF();
                        String ldesc = "";
                        if (ver >= 4) {
                            ldesc = dis.readUTF();
                        }
                        h.handleDescriptor(name, info, main, ldesc);
                        break;
                    case ResourceFolder.DATA_CODE:
                        text += "Resources:\n";
                        int l = dis.read();
                        for (int i = 0; i < l; i++) {
                            String respath = dis.readUTF();
                            if (ver < 4) {
                                respath = respath.substring("/res".length()).replace("\\", "/");
                            }
                            h.handleResource(respath, new String(dis.readUTF().getBytes(), "UTF-8"));
                        }
                        break;
                    case SourceFolder.DATA_CODE:
                        text += "Packages:\n";
                        l = dis.read();
                        for (int i = 0; i < l; i++) {
                            String pkg = dis.readUTF();
                            h.handlePackage(pkg);
                            int flen = dis.read();
                            for (int x = 0; x < flen; x++) {
                                String filename = dis.readUTF();
                                String source = null;
                                if (ver >= 4 && (flags & 1) == 1) {
                                    source = dis.readUTF();
                                }
                                byte[] data = new byte[dis.readInt()];
                                dis.readFully(data);
                                h.handleModFile(pkg, filename, source, data);
                            }
                        }
                        break;
                    case -1:
                        break;
                    default:
                        h.handleUnknown(opt);
                        break;
                    }
                }
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return text;
        }

        public void extract(RootList rootlist) {
            String name = realdest.getName().substring(0, realdest.getName().length() - ".mod".length());
            File Mroot = new File(rootlist.getAbsolutePath(), name);
            int i = 1;
            while (Mroot.exists()) {
                Mroot = new File(rootlist.getAbsolutePath(), name + " (" + (++i) + ")");
            }
            Mroot.mkdir();
            final File root = Mroot.getAbsoluteFile();
            try {
                FileInputStream source = new FileInputStream(realdest);
                File destmod = new File(root, "deployed/" + name + ".mod");
                destmod.getParentFile().mkdirs();
                FileOutputStream dest = new FileOutputStream(destmod);
                while (source.available() != 0) {
                    byte[] buf = new byte[512];
                    source.read(buf);
                    dest.write(buf);
                }
                source.close();
                dest.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            decompile(new ModDistHandler() {

                private File cpkg = null;

                @Override
                public void HandleVer(int ver) {
                }

                @Override
                public void handleModFile(String pkgname, String filename, String source, byte[] binary) {
                    try {
                        BufferedWriter Wsource = new BufferedWriter(new FileWriter(new File(cpkg, filename + ".cmod")));
                        if (source != null) {
                            Wsource.write(source);
                        } else {
                            Wsource.write("// No source code attached\n");
                        }
                        Wsource.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void handleUnknown(int opt) {
                }

                @Override
                public void handlePackage(String pkgname) {
                    cpkg = new File(root, "src/" + pkgname.replace('.', '/'));
                    cpkg.mkdirs();
                }

                @Override
                public void handleResource(String pathname, String data) {
                    File resfile = new File(root, "/res" + pathname);
                    resfile.getParentFile().mkdirs();
                    System.out.println(resfile.getAbsolutePath());
                    try {
                        BufferedWriter reswriter = new BufferedWriter(new FileWriter(resfile));
                        reswriter.write(data);
                        reswriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void handleDescriptor(String modname, String modinfo, String main, String desc) {
                    File file = new File(root, "ccmod.info");
                    try {
                        BufferedWriter descwriter = new BufferedWriter(new FileWriter(file));
                        descwriter.write(modname + "\n");
                        descwriter.write(modinfo + "\n");
                        descwriter.write(main + "\n");
                        descwriter.write(desc);
                        descwriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void handleFlags(int flags) {
                }
            });
        }
    }

    public static abstract class ModDistHandler {
        public void HandleVer(int ver) {
        }

        public abstract void handleModFile(String pkgname, String filename, String source, byte[] binary);

        public void handleUnknown(int opt) {
        }

        public abstract void handlePackage(String pkgname);

        public abstract void handleResource(String pathname, String data);

        public abstract void handleDescriptor(String modname, String modinfo, String main, String desc);

        public void handleFlags(int flags) {
        }
    }
}
