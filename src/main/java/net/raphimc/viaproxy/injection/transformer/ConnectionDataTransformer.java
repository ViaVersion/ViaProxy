package net.raphimc.viaproxy.injection.transformer;

import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.blockconnections.ConnectionData;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CASM;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@CTransformer(ConnectionData.class)
public abstract class ConnectionDataTransformer {

    @CASM("update")
    public static void preventBlockChangeSpam1(MethodNode method) {
        LabelNode continueLabel = new LabelNode();
        InsnList checkCode = new InsnList();
        checkCode.add(new VarInsnNode(Opcodes.ILOAD, 7));
        checkCode.add(new VarInsnNode(Opcodes.ILOAD, 9));
        checkCode.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, continueLabel));

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (checkCode != null && insn.getOpcode() == Opcodes.ISTORE) {
                VarInsnNode varInsn = (VarInsnNode) insn;
                if (varInsn.var == 9) {
                    method.instructions.insert(insn, checkCode);
                    checkCode = null;
                }
            } else if (continueLabel != null && insn.getOpcode() == Opcodes.IINC) {
                method.instructions.insertBefore(insn, continueLabel);
                continueLabel = null;
            }
        }
    }

    @CASM("updateBlock")
    public static void preventBlockChangeSpam2(MethodNode method) {
        LabelNode addLabel = new LabelNode();
        InsnList checkCode = new InsnList();
        checkCode.add(new VarInsnNode(Opcodes.ILOAD, 3));
        checkCode.add(new VarInsnNode(Opcodes.ILOAD, 5));
        checkCode.add(new JumpInsnNode(Opcodes.IF_ICMPNE, addLabel));
        checkCode.add(new InsnNode(Opcodes.RETURN));
        checkCode.add(addLabel);

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                method.instructions.insertBefore(insn, checkCode);
                break;
            }
        }
    }

}
