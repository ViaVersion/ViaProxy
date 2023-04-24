/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viaproxy.injection;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.IBytecodeTransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Java17ToJava8 implements IBytecodeTransformer {

    private static final char STACK_ARG_CONSTANT = '\u0001';
    private static final char BSM_ARG_CONSTANT = '\u0002';

    final TransformerManager transformerManager;
    private final int nativeClassVersion;
    private final List<String> whitelistedPackages = new ArrayList<>();

    public Java17ToJava8(final TransformerManager transformerManager) {
        this.transformerManager = transformerManager;

        final String classVersion = System.getProperty("java.class.version");
        final String[] versions = classVersion.split("\\.");
        final int majorVersion = Integer.parseInt(versions[0]);
        final int minorVersion = Integer.parseInt(versions[1]);
        this.nativeClassVersion = minorVersion << 16 | majorVersion;
    }

    public Java17ToJava8 addWhitelistedPackage(final String packageName) {
        this.whitelistedPackages.add(packageName);
        return this;
    }

    @Override
    public byte[] transform(String className, byte[] bytecode, boolean calculateStackMapFrames) {
        for (String whitelistedPackage : this.whitelistedPackages) {
            if (!className.startsWith(whitelistedPackage)) return null;
        }

        final ClassNode classNode = ASMUtils.fromBytes(bytecode);
        if (classNode.version <= this.nativeClassVersion) return null;

        classNode.version = Opcodes.V1_8;
        this.makePublic(classNode);
        this.convertStringConcatFactory(classNode);
        this.convertListMethods(classNode);
        this.convertSetMethods(classNode);
        this.convertMapMethods(classNode);
        this.convertStreamMethods(classNode);
        this.convertMiscMethods(classNode);
        this.removeRecords(classNode);

        if (calculateStackMapFrames) {
            return ASMUtils.toBytes(classNode, this.transformerManager.getClassTree(), this.transformerManager.getClassProvider());
        } else {
            return ASMUtils.toStacklessBytes(classNode);
        }
    }

    private void makePublic(final ClassNode classNode) {
        classNode.access = ASMUtils.setAccess(classNode.access, Opcodes.ACC_PUBLIC);
        for (MethodNode methodNode : classNode.methods) methodNode.access = ASMUtils.setAccess(methodNode.access, Opcodes.ACC_PUBLIC);
        for (FieldNode fieldNode : classNode.fields) fieldNode.access = ASMUtils.setAccess(fieldNode.access, Opcodes.ACC_PUBLIC);
    }

    private void convertStringConcatFactory(final ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    InvokeDynamicInsnNode insn = (InvokeDynamicInsnNode) instruction;
                    if (insn.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory") && insn.bsm.getName().equals("makeConcatWithConstants")) {
                        String pattern = (String) insn.bsmArgs[0];
                        Type[] stackArgs = Type.getArgumentTypes(insn.desc);
                        Object[] bsmArgs = Arrays.copyOfRange(insn.bsmArgs, 1, insn.bsmArgs.length);
                        int stackArgsCount = count(pattern, STACK_ARG_CONSTANT);
                        int bsmArgsCount = count(pattern, BSM_ARG_CONSTANT);

                        if (stackArgs.length != stackArgsCount) throw new IllegalStateException("Stack args count does not match");
                        if (bsmArgs.length != bsmArgsCount) throw new IllegalStateException("BSM args count does not match");

                        int freeVarIndex = ASMUtils.getFreeVarIndex(method);
                        int[] stackIndices = new int[stackArgsCount];
                        for (int i = 0; i < stackArgs.length; i++) {
                            stackIndices[i] = freeVarIndex;
                            freeVarIndex += stackArgs[i].getSize();
                        }
                        for (int i = stackIndices.length - 1; i >= 0; i--) {
                            method.instructions.insertBefore(insn, new VarInsnNode(stackArgs[i].getOpcode(Opcodes.ISTORE), stackIndices[i]));
                        }

                        InsnList converted = convertStringConcatFactory(pattern, stackArgs, stackIndices, bsmArgs);
                        method.instructions.insertBefore(insn, converted);
                        method.instructions.remove(insn);
                    }
                }
            }
        }
    }

    private void convertListMethods(final ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                    final MethodInsnNode min = (MethodInsnNode) insn;
                    if (!min.owner.equals("java/util/List")) continue;

                    final InsnList list = new InsnList();

                    if (min.name.equals("of")) {
                        final Type[] args = Type.getArgumentTypes(min.desc);
                        if (args.length != 1 || args[0].getSort() != Type.ARRAY) {
                            int freeVarIndex = ASMUtils.getFreeVarIndex(method);

                            int argCount = args.length;
                            list.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
                            list.add(new InsnNode(Opcodes.DUP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V"));
                            list.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex));
                            for (int i = 0; i < argCount; i++) {
                                list.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
                                list.add(new InsnNode(Opcodes.SWAP));
                                list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z"));
                                list.add(new InsnNode(Opcodes.POP));
                            }
                            list.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
                            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableList", "(Ljava/util/List;)Ljava/util/List;"));
                        }
                    } else if (min.name.equals("copyOf")) {
                        list.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
                        list.add(new InsnNode(Opcodes.DUP_X1));
                        list.add(new InsnNode(Opcodes.SWAP));
                        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "(Ljava/util/Collection;)V"));
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableList", "(Ljava/util/List;)Ljava/util/List;"));
                    }

                    if (list.size() != 0) {
                        method.instructions.insertBefore(insn, list);
                        method.instructions.remove(insn);
                    }
                }
            }
        }
    }

    private void convertSetMethods(final ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                    final MethodInsnNode min = (MethodInsnNode) insn;
                    if (!min.owner.equals("java/util/Set")) continue;

                    final InsnList list = new InsnList();

                    if (min.name.equals("of")) {
                        final Type[] args = Type.getArgumentTypes(min.desc);
                        if (args.length != 1 || args[0].getSort() != Type.ARRAY) {
                            int freeVarIndex = ASMUtils.getFreeVarIndex(method);

                            int argCount = args.length;
                            list.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashSet"));
                            list.add(new InsnNode(Opcodes.DUP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashSet", "<init>", "()V"));
                            list.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex));
                            for (int i = 0; i < argCount; i++) {
                                list.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
                                list.add(new InsnNode(Opcodes.SWAP));
                                list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z"));
                                list.add(new InsnNode(Opcodes.POP));
                            }
                            list.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
                            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableSet", "(Ljava/util/Set;)Ljava/util/Set;"));
                        }
                    } else if (min.name.equals("copyOf")) {
                        list.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashSet"));
                        list.add(new InsnNode(Opcodes.DUP_X1));
                        list.add(new InsnNode(Opcodes.SWAP));
                        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashSet", "<init>", "(Ljava/util/Collection;)V"));
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableSet", "(Ljava/util/Set;)Ljava/util/Set;"));
                    }

                    if (list.size() != 0) {
                        method.instructions.insertBefore(insn, list);
                        method.instructions.remove(insn);
                    }
                }
            }
        }
    }

    private void convertMapMethods(final ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                    final MethodInsnNode min = (MethodInsnNode) insn;
                    if (!min.owner.equals("java/util/Map")) continue;

                    final InsnList list = new InsnList();

                    if (min.name.equals("of")) {
                        final Type[] args = Type.getArgumentTypes(min.desc);
                        if (args.length != 1 || args[0].getSort() != Type.ARRAY) {
                            int freeVarIndex = ASMUtils.getFreeVarIndex(method);

                            int argCount = args.length;
                            if (argCount % 2 != 0) {
                                throw new RuntimeException("Map.of() requires an even number of arguments");
                            }

                            list.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashMap"));
                            list.add(new InsnNode(Opcodes.DUP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V"));
                            list.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex));
                            for (int i = 0; i < argCount / 2; i++) {
                                list.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
                                list.add(new InsnNode(Opcodes.DUP_X2));
                                list.add(new InsnNode(Opcodes.POP));
                                list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
                                list.add(new InsnNode(Opcodes.POP));
                            }
                            list.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
                            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableMap", "(Ljava/util/Map;)Ljava/util/Map;"));
                        }
                    } else if (min.name.equals("copyOf")) {
                        list.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashMap"));
                        list.add(new InsnNode(Opcodes.DUP_X1));
                        list.add(new InsnNode(Opcodes.SWAP));
                        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "(Ljava/util/Map;)V"));
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableMap", "(Ljava/util/Map;)Ljava/util/Map;"));
                    }

                    if (list.size() != 0) {
                        method.instructions.insertBefore(insn, list);
                        method.instructions.remove(insn);
                    }
                }
            }
        }
    }

    private void convertStreamMethods(final ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                    final MethodInsnNode min = (MethodInsnNode) insn;
                    if (!min.owner.equals("java/util/stream/Stream")) continue;

                    final InsnList list = new InsnList();

                    if (min.name.equals("toList")) {
                        int freeVarIndex = ASMUtils.getFreeVarIndex(method);
                        list.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex));

                        list.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
                        list.add(new InsnNode(Opcodes.DUP));
                        list.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
                        list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "toArray", "()[Ljava/lang/Object;"));
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;"));
                        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "(Ljava/util/Collection;)V"));
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableList", "(Ljava/util/List;)Ljava/util/List;"));
                    }

                    if (list.size() != 0) {
                        method.instructions.insertBefore(insn, list);
                        method.instructions.remove(insn);
                    }
                }
            }
        }
    }

    private void convertMiscMethods(final ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn instanceof MethodInsnNode) {
                    final MethodInsnNode min = (MethodInsnNode) insn;
                    final InsnList list = new InsnList();

                    if (min.owner.equals("java/lang/String")) {
                        if (min.name.equals("isBlank") && min.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim", "()Ljava/lang/String;"));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "isEmpty", "()Z"));
                        }
                    } else if (min.owner.equals("java/io/InputStream")) {
                        if (min.name.equals("readAllBytes") && min.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/apache/commons/io/IOUtils", "toByteArray", "(Ljava/io/InputStream;)[B"));
                        }
                    } else if (min.owner.equals("java/nio/file/FileSystems")) {
                        if (min.name.equals("newFileSystem") && min.desc.equals("(Ljava/nio/file/Path;Ljava/util/Map;Ljava/lang/ClassLoader;)Ljava/nio/file/FileSystem;")) {
                            list.add(new InsnNode(Opcodes.DUP2_X1));
                            list.add(new InsnNode(Opcodes.POP2));
                            list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "toUri", "()Ljava/net/URI;"));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/net/URI", "toString", "()Ljava/lang/String;"));
                            list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                            list.add(new InsnNode(Opcodes.DUP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));
                            list.add(new LdcInsnNode("jar:"));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
                            list.add(new TypeInsnNode(Opcodes.NEW, "java/net/URI"));
                            list.add(new InsnNode(Opcodes.DUP_X1));
                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/net/URI", "<init>", "(Ljava/lang/String;)V"));
                            list.add(new InsnNode(Opcodes.DUP_X2));
                            list.add(new InsnNode(Opcodes.POP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/nio/file/FileSystems", "newFileSystem", "(Ljava/net/URI;Ljava/util/Map;Ljava/lang/ClassLoader;)Ljava/nio/file/FileSystem;"));
                        }
                    } else if (min.owner.equals("java/util/Objects")) {
                        if (min.name.equals("requireNonNullElse")) {
                            LabelNode elseJump = new LabelNode();
                            LabelNode endJump = new LabelNode();

                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new InsnNode(Opcodes.DUP));
                            list.add(new JumpInsnNode(Opcodes.IFNULL, elseJump));
                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new InsnNode(Opcodes.POP));
                            list.add(new JumpInsnNode(Opcodes.GOTO, endJump));
                            list.add(elseJump);
                            list.add(new InsnNode(Opcodes.POP));
                            list.add(new LdcInsnNode("defaultObj"));
                            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"));
                            list.add(endJump);
                        }
                    } else if (min.owner.equals("java/nio/file/Files")) {
                        if (min.name.equals("readString")) {
                            if (min.desc.equals("(Ljava/nio/file/Path;)Ljava/lang/String;")) {
                                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/nio/file/Files", "readAllBytes", "(Ljava/nio/file/Path;)[B"));
                                list.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;"));
                            } else if (min.desc.equals("(Ljava/nio/file/Path;Ljava/nio/charset/Charset;)Ljava/lang/String;")) {
                                list.add(new InsnNode(Opcodes.SWAP));
                                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/nio/file/Files", "readAllBytes", "(Ljava/nio/file/Path;)[B"));
                                list.add(new InsnNode(Opcodes.SWAP));
                            }
                            list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
                            list.add(new InsnNode(Opcodes.DUP_X2));
                            list.add(new InsnNode(Opcodes.DUP_X2));
                            list.add(new InsnNode(Opcodes.POP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V"));
                        }
                    } else if (min.owner.equals("java/util/regex/Matcher")) {
                        if (min.name.equals("appendReplacement") && min.desc.equals("(Ljava/lang/StringBuilder;Ljava/lang/String;)Ljava/util/regex/Matcher;")) {
                            int stringBufferIndex = ASMUtils.getFreeVarIndex(method);
                            list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuffer"));
                            list.add(new InsnNode(Opcodes.DUP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V"));
                            list.add(new VarInsnNode(Opcodes.ASTORE, stringBufferIndex));

                            list.add(new InsnNode(Opcodes.DUP2_X1));
                            list.add(new InsnNode(Opcodes.POP2));
                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new VarInsnNode(Opcodes.ALOAD, stringBufferIndex));
                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, min.owner, min.name, "(Ljava/lang/StringBuffer;Ljava/lang/String;)Ljava/util/regex/Matcher;"));
                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new VarInsnNode(Opcodes.ALOAD, stringBufferIndex));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/StringBuffer;)Ljava/lang/StringBuilder;"));
                            list.add(new InsnNode(Opcodes.POP));
                        } else if (min.name.equals("appendTail") && min.desc.equals("(Ljava/lang/StringBuilder;)Ljava/lang/StringBuilder;")) {
                            int stringBufferIndex = ASMUtils.getFreeVarIndex(method);
                            list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuffer"));
                            list.add(new InsnNode(Opcodes.DUP));
                            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V"));
                            list.add(new VarInsnNode(Opcodes.ASTORE, stringBufferIndex));

                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new VarInsnNode(Opcodes.ALOAD, stringBufferIndex));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, min.owner, min.name, "(Ljava/lang/StringBuffer;)Ljava/lang/StringBuffer;"));
                            list.add(new InsnNode(Opcodes.SWAP));
                            list.add(new VarInsnNode(Opcodes.ALOAD, stringBufferIndex));
                            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/StringBuffer;)Ljava/lang/StringBuilder;"));
                            list.add(new InsnNode(Opcodes.POP));
                        }
                    }

                    if (list.size() != 0) {
                        method.instructions.insertBefore(insn, list);
                        method.instructions.remove(insn);
                    }
                }
            }
        }
    }

    private void removeRecords(final ClassNode node) {
        if (node.superName.equals("java/lang/Record")) {
            node.access &= ~Opcodes.ACC_RECORD;
            node.superName = "java/lang/Object";

            List<MethodNode> constructors = ASMUtils.getMethodsFromCombi(node, "<init>");
            for (MethodNode method : constructors) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                        MethodInsnNode min = (MethodInsnNode) insn;
                        if (min.owner.equals("java/lang/Record")) {
                            min.owner = "java/lang/Object";
                            break;
                        }
                    }
                }
            }
        }
    }

    private int count(final String s, final char search) {
        char[] chars = s.toCharArray();
        int count = 0;
        for (char c : chars) {
            if (c == search) count++;
        }
        return count;
    }

    private InsnList convertStringConcatFactory(final String pattern, final Type[] stackArgs, final int[] stackIndices, final Object[] bsmArgs) {
        InsnList insns = new InsnList();
        char[] chars = pattern.toCharArray();
        int stackArgsIndex = 0;
        int bsmArgsIndex = 0;
        StringBuilder partBuilder = new StringBuilder();

        insns.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        insns.add(new InsnNode(Opcodes.DUP));
        insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));
        for (char c : chars) {
            if (c == STACK_ARG_CONSTANT) {
                if (partBuilder.length() != 0) {
                    insns.add(new LdcInsnNode(partBuilder.toString()));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                    partBuilder = new StringBuilder();
                }

                Type stackArg = stackArgs[stackArgsIndex++];
                int stackIndex = stackIndices[stackArgsIndex - 1];
                if (stackArg.getSort() == Type.OBJECT) {
                    insns.add(new VarInsnNode(Opcodes.ALOAD, stackIndex));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
                } else if (stackArg.getSort() == Type.ARRAY) {
                    insns.add(new VarInsnNode(Opcodes.ALOAD, stackIndex));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                } else {
                    insns.add(new VarInsnNode(stackArg.getOpcode(Opcodes.ILOAD), stackIndex));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + stackArg.getDescriptor() + ")Ljava/lang/StringBuilder;"));
                }
            } else if (c == BSM_ARG_CONSTANT) {
                insns.add(new LdcInsnNode(bsmArgs[bsmArgsIndex++]));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
            } else {
                partBuilder.append(c);
            }
        }
        if (partBuilder.length() != 0) {
            insns.add(new LdcInsnNode(partBuilder.toString()));
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
        }
        insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
        return insns;
    }

}
