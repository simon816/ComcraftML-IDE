package com.simon816.CCMLEditor;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Enumeration;

import com.google.minijoe.sys.JsArray;
import com.google.minijoe.sys.JsFunction;
import com.google.minijoe.sys.JsObject;

public class Decompiler {

    private static final int XCODE_START = 0xEA;
    private DataInputStream dis;
    private StringBuffer source;
    private double[] numberLiterals;
    private String[] stringLiterals;
    private String[] globalStringTable;
    private String[] localVariableNames;
    private StringBuffer line;
    private int lineOff;
    private int elseAt;
    private boolean braceOpen;

    public Decompiler(DataInputStream dis) {
        this.dis = dis;
    }

    public String decompile() throws IOException {
        source = new StringBuffer();
        StringBuffer buf = new StringBuffer(7);
        for (int i = 0; i < 7; i++) {
            buf.append((char) dis.read());
        }
        String magic = buf.toString();
        dis.read();
        if (!"MiniJoe".equals(magic)) {
            throw new IOException("Magic does not match \"MiniJoe\"!");
        }
        readBlocks();
        return source.toString();

    }

    private void readBlocks() throws IOException {

        loop: while (true) {
            int type = dis.read();
            int count;
            switch (type) {
            case 0x00:
                System.out.println("Comment");
                // Comment
                dis.readUTF();
                break;
            case 0x10:
                System.out.println("Global strings");
                // Global String Table
                count = dis.readUnsignedShort();
                globalStringTable = new String[count];
                for (int i = 0; i < count; i++) {
                    globalStringTable[i] = dis.readUTF();
                }
                break;
            case 0x20:
                System.out.println("Numbers");
                // Number Literals
                count = dis.readUnsignedShort();
                numberLiterals = new double[count];
                for (int i = 0; i < count; i++) {
                    numberLiterals[i] = dis.readDouble();
                }
                break;
            case 0x30:
                System.out.println("Strings");
                // String Literals
                count = dis.readUnsignedShort();
                stringLiterals = new String[count];
                for (int i = 0; i < count; i++) {
                    int index = dis.readUnsignedShort();
                    stringLiterals[i] = globalStringTable[index];
                }
                break;
            case 0x40:
                System.out.println("Regex");
                // Regex Literals
                count = dis.readUnsignedShort();
                stringLiterals = new String[count];
                for (int i = 0; i < count; i++) {
                    dis.readUnsignedShort();
                }
                break;
            case 0x50:
                System.out.println("Function");
                // Function Literals
                count = dis.readUnsignedShort();
                for (int i = 0; i < count; i++) {
                    readBlocks();
                    // TODO
                }
                break;
            case 0x60:
                System.out.println("Local variables");
                // Local Variable Names
                count = dis.readUnsignedShort();
                localVariableNames = new String[count];
                for (int i = 0; i < count; i++) {
                    int index = dis.readUnsignedShort();
                    localVariableNames[i] = globalStringTable[index];
                }
                break;
            case 0x080:
                System.out.println("Code");
                // Code
                dis.readUnsignedShort();
                dis.readUnsignedShort();
                dis.read();
                int size = dis.readUnsignedShort();
                byte[] code = new byte[size];
                dis.readFully(code);
                reverseEngineer(code);
                break;
            case 0xE0:
                System.out.println("Line Numbers");
                // Line Numbers
                count = dis.readUnsignedShort();
                for (int i = 0; i < count; i++) {
                    dis.readUnsignedShort();
                    dis.readUnsignedShort();
                }
                break;
            case 0x0ff:
                System.out.println("End");
                break loop;
            default:
                throw new IOException("Unknown block type: " + type);
            }
        }
    }

    private void reverseEngineer(byte[] code) {
        Context context = new Context();
        int i = 0;
        // JsArray stack = new JsArray();
        JsArray test = new JsArray();
        // stack.setObject(0, null);
        line = new StringBuffer();
        lineOff = 0;
        int sp = 0;
        while (i < code.length) {
            int opcode = code[i++];
            // System.out.println("OPCODE:"+opcode);
            if (opcode >= 0) {
                // System.out.println("No opcode data");
                // No data operation
                switch (opcode) {
                case 0x00:

                    break;
                case JsFunction.OP_ADD:
                    // stack.setObject(sp++, stack.getObject(sp - 3) + " + " + stack.getObject(sp - 2));
                    test.setObject(sp - 2, test.getString(sp - 2) + " + " + test.getString(sp - 1));
                    context.clear(sp - 1);
                    context.clear(sp - 2);
                    sp--;
                    break;
                case 0x02:

                    break;
                case 0x03:

                    break;
                case 0x04:

                    break;
                case 0x05:

                    break;
                case 0x06:

                    break;
                case 0x07:

                    break;
                case JsFunction.OP_DUP:
                    // source.append(stack.toString() + "\n");
                    test.copy(sp - 1, test, sp);
                    sp++;
                    break;
                case 0x09:

                    break;

                case JsFunction.OP_CTX_GET:
                    context.vmGetOperation(test, sp - 1, sp - 1);
                    break;

                case JsFunction.OP_CTX_SET:
                    context.vmSetOperation(test, sp - 1, sp - 2);
                    sp--; // take away name, not value
                    break;
                // case JsFunction.OP_CTX_GET:
                // // stack.setObject(sp++, stack.getString(sp - 1));
                // String var = test.getString(sp - 1);
                // test.setObject(sp - 1, new Variable(var));
                // break;
                case JsFunction.OP_GET:
                    // StringVar ctx = (StringVar) test.getObject(sp - 2);
                    line.append("[" + test.getString(sp - 1) + "]");
                    // System.out.println("GetMember name: " + test.getObject(sp - 1));
                    // ctx.vmGetOperation(test, sp - 1, sp - 2);
                    sp--;
                    // String var = test.getString(sp - 1);
                    // test.setObject(sp - 1, new Variable(var));
                    // System.out.println("OP_GET " + test + " [" + sp + "]");
                    // ((Variable) test.getObject(sp - 2)).getVar(sp - 2).addVar(test.getString(sp - 1));
                    // sp--;
                    break;
                case 0x0c:

                    break;
                case 0x0d:

                    break;
                case 0x0e:

                    break;
                case 0x0f:

                    break;
                case JsFunction.OP_INC:
                    // TODO INC not working
                    test.setObject(sp - 1, test.getString(sp - 1) + "++");
                    context.clear(sp - 1);
                    break;
                case 0x11:

                    break;
                case 0x12:

                    break;
                case 0x13:

                    break;
                case JsFunction.OP_MUL:
                    // stack.setObject(sp++, stack.getObject(sp - 3) + " * " + stack.getObject(sp - 2));
                    test.setObject(sp - 2, test.getString(sp - 2) + " * " + test.getString(sp - 1));
                    sp--;
                    context.clear(sp - 1);
                    context.clear(sp - 2);
                    break;
                case 0x15:

                    break;
                case 0x16:

                    break;
                case 0x17:

                    break;
                case 0x18:

                    break;
                case 0x19:

                    break;
                case 0x1a:

                    break;
                case JsFunction.OP_DROP:
                    // source.append(stack.toString() + "\n");
                    context.release();
                    if (braceOpen && elseAt == 0) {
                        System.out.println(line.append("\n}"));
                        braceOpen = false;
                    } else {
                        System.out.println(line.append(";"));
                    }
                    line = null;
                    line = new StringBuffer();
                    lineOff = 0;
                    sp--;
                    break;
                case JsFunction.OP_PUSH_TRUE:
                    test.setBoolean(sp++, true);
                    break;
                case JsFunction.OP_PUSH_FALSE:
                    test.setBoolean(sp++, false);
                    break;
                case 0x1e:

                    break;
                // case 0x1f:
                // // Context Set
                // // source.append(stack.getString(sp - 1) + " = " + stack.getObject(sp - 2) + "\n");
                // sp--;
                // break;
                case 0x20:

                    break;
                case 0x21:

                    break;
                case 0x22:

                    break;
                case 0x23:

                    break;
                case 0x24:

                    break;
                case 0x25:

                    break;
                case 0x26:

                    break;
                case 0x27:

                    break;
                case 0x28:

                    break;
                case 0x29:

                    break;
                case 0x2a:

                    break;
                case 0x2b:

                    break;
                case 0x2c:

                    break;
                case 0x2d:

                    break;
                case 0x2e:

                    break;
                case 0x2f:

                    break;
                case 0x30:

                    break;
                case 0x31:

                    break;
                case 0x32:

                    break;
                case 0x33:

                    break;
                }
            } else {
                // System.out.println("Opcode has data");
                // Operation has data
                int data = code[i];
                int imm;
                if ((opcode & 1) == 0) {
                    imm = code[i++];
                } else {
                    imm = (code[i] << 8) | (code[i + 1] & 0xff);
                    i++;
                    // System.out.println("Jump: " + code[i]);
                }

                Object content = null;
                switch (opcode & 0xfe) {
                case 0xEC:
                    content = new Double(numberLiterals[imm]);
                    break;
                case 0xFE:
                    content = stringLiterals[imm];
                    break;
                }
                switch (((opcode & 0x0ff) - XCODE_START) >>> 1) {
                case 0x00:

                    break;
                case 0x01:

                    break;
                case 0x02:
                    String ln = "} else {\n";
                    if (elseAt > 0) {
                        line.append(ln);
                        elseAt = 0;
                        lineOff += ln.length();
                        braceOpen = true;
                    }
                    break;
                case 0x03:
                    // If
                    ln = "if (" + test.getObject(sp - 1) + ") {\n";
                    line.append(ln);
                    elseAt = imm;
                    // System.out.println(elseAt);
                    lineOff += ln.length();
                    braceOpen = true;
                    break;
                case 0x04:
                    // Function call
                    line.append("(");
                    for (; data > 0; --data) {
                        line.append(test.getObject(sp - data) + (data > 1 ? ", " : ""));
                        context.clear(sp - data);
                    }
                    sp -= data;
                    line.append(")");
                    break;
                case 0x05:

                    break;
                case 0x06:

                    break;
                case 0x07:

                    break;
                case 0x08:

                    break;
                case 0x09:
                    // Push Integer
                    // stack.setInt(sp++, data);
                    test.setNumber(sp++, imm);
                    break;
                case 0x0a:
                    // Push String
                    test.setObject(sp++, new StringVar((String) content));
                    // stack.setObject(sp++, content);
                    break;
                }
            }
        }
        System.out.println(test);
        System.out.println(context);
    }

    private static class StringVar {
        private String content;
        public boolean literal;

        public StringVar(String content) {
            this.content = content != null ? content : "";
            literal = true;
        }

        @Override
        public String toString() {
            if (literal) {
                return literalString();
            }
            return content;
        }

        public void setLiteral(boolean lit) {
            this.literal = lit;
        }

        public String literalString() {
            return "\"" + content + "\"";
        }
    }

    private class Context extends JsObject {

        public Context() {
            super(JsObject.OBJECT_PROTOTYPE);
            // TODO Auto-generated constructor stub
        }

        public void clear(int i) {
            Enumeration<?> e = keys();
            while (e.hasMoreElements()) {
                String k = (String) e.nextElement();
                if (getObject(k).equals(new Integer(i))) {
                    delete(k);
                }
            }
        }

        public void release() {
            Enumeration<?> e = keys();
            while (e.hasMoreElements()) {
                line.insert(lineOff, e.nextElement());
            }
            this.data.clear();
        }

        @Override
        public String toString() {
            return data.toString();
        }

        @Override
        public void vmGetOperation(JsArray stack, int keyIndex, int valueIndex) {
            StringVar obj = (StringVar) stack.getObject(keyIndex);
            obj.setLiteral(false);
            // System.out.print(obj);
            stack.setObject(valueIndex, obj);
            setObject(obj.toString(), new Integer(valueIndex));
            // super.vmGetOperation(stack, keyIndex, valueIndex);
            // System.out.println(stack.getString(keyIndex) + "|" + stack.getObject(valueIndex));
        }

        @Override
        public void vmSetOperation(JsArray stack, int keyIndex, int valueIndex) {
            // super.vmSetOperation(stack, keyIndex, valueIndex);
            StringVar obj = (StringVar) stack.getObject(keyIndex);
            obj.setLiteral(false);
            line.append(obj + " = " + stack.getString(valueIndex));
        }

    }
}
