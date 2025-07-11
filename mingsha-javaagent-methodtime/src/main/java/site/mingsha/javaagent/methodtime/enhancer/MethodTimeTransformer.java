package site.mingsha.javaagent.methodtime.enhancer;

import site.mingsha.javaagent.methodtime.collector.MethodTimeBuffer;
import site.mingsha.javaagent.methodtime.collector.MethodTimeRecord;
import site.mingsha.javaagent.methodtime.config.AgentConfig;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import java.util.Random;

/**
 * 字节码增强：方法耗时采集，包范围过滤。
 * Bytecode transformer for method time profiling and package filtering.
 * 支持采样率、最小耗时过滤，增强指定包下所有非抽象/非native方法。
 * Supports sampling rate, min duration filter, and enhances all non-abstract/non-native methods in specified packages.
 *
 * @author mingsha
 */
public class MethodTimeTransformer implements ClassFileTransformer {
    private static final Random random = new Random();
    /**
     * 字节码增强入口，实现方法耗时采集和包范围过滤。
     * Entry for bytecode transformation, implements method time profiling and package filtering.
     * 仅增强配置包范围内的非抽象/非native方法，采集纳秒级耗时。
     * Only enhances non-abstract/non-native methods in configured packages, collects nanosecond-level duration.
     * 支持采样率控制、最小耗时过滤。
     * Supports sampling rate and min duration filter.
     * @param loader              类加载器 | class loader
     * @param className           类名（/分隔）| class name (slash separated)
     * @param classBeingRedefined 被重定义的类（可为null）| class being redefined (nullable)
     * @param protectionDomain    保护域 | protection domain
     * @param classfileBuffer     原始字节码 | original bytecode
     * @return 增强后的字节码，若不增强返回null | transformed bytecode or null if not enhanced
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        // 包范围过滤 | package filter
        String dotName = className.replace('/', '.');
        String[] packages = AgentConfig.getCollectorPackages().split(",");
        boolean match = false;
        for (String pkg : packages) {
            if (pkg.endsWith(".*")) {
                if (dotName.startsWith(pkg.substring(0, pkg.length() - 2))) {
                    match = true;
                    break;
                }
            } else if (dotName.equals(pkg)) {
                match = true;
                break;
            }
        }
        if (!match) return null;
        // ASM 增强 | ASM enhancement
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ((access & Opcodes.ACC_ABSTRACT) != 0 || (access & Opcodes.ACC_NATIVE) != 0) {
                    return mv;
                }
                return new AdviceAdapter(Opcodes.ASM9, mv, access, name, desc) {
                    private int startTimeVarIdx;
                    private Label skipLabel;
                    private Label skipRecordLabel;
                    
                    @Override
                    protected void onMethodEnter() {
                        // 采样率控制 | sampling rate
                        double rate = AgentConfig.getSamplingRate();
                        if (rate < 1.0) {
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "random", "()D", false);
                            mv.visitLdcInsn(rate);
                            mv.visitInsn(DCMPG);
                            skipLabel = new Label();
                            mv.visitJumpInsn(IFGT, skipLabel);
                        }
                        
                        // long start = System.nanoTime();
                        startTimeVarIdx = newLocal(Type.LONG_TYPE);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                        mv.visitVarInsn(LSTORE, startTimeVarIdx);
                        
                        if (rate < 1.0) {
                            mv.visitLabel(skipLabel);
                        }
                    }
                    
                    @Override
                    protected void onMethodExit(int opcode) {
                        // 如果采样率小于1.0，检查是否需要跳过记录
                        double rate = AgentConfig.getSamplingRate();
                        if (rate < 1.0) {
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "random", "()D", false);
                            mv.visitLdcInsn(rate);
                            mv.visitInsn(DCMPG);
                            skipRecordLabel = new Label();
                            mv.visitJumpInsn(IFGT, skipRecordLabel);
                        }
                        
                        // long end = System.nanoTime();
                        int endTimeVarIdx = newLocal(Type.LONG_TYPE);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                        mv.visitVarInsn(LSTORE, endTimeVarIdx);
                        
                        // long duration = end - start;
                        int durationVarIdx = newLocal(Type.LONG_TYPE);
                        mv.visitVarInsn(LLOAD, endTimeVarIdx);
                        mv.visitVarInsn(LLOAD, startTimeVarIdx);
                        mv.visitInsn(LSUB);
                        mv.visitVarInsn(LSTORE, durationVarIdx);
                        
                        // 最小耗时过滤 | min duration filter
                        mv.visitVarInsn(LLOAD, durationVarIdx);
                        mv.visitLdcInsn(AgentConfig.getMinDurationNs());
                        Label skipMinDuration = new Label();
                        mv.visitInsn(LCMP);
                        mv.visitJumpInsn(IFLT, skipMinDuration);
                        
                        // MethodTimeBuffer.offer(new MethodTimeRecord(...))
                        mv.visitTypeInsn(NEW, "site/mingsha/javaagent/methodtime/collector/MethodTimeRecord");
                        mv.visitInsn(DUP);
                        mv.visitLdcInsn(dotName);
                        mv.visitLdcInsn(name);
                        mv.visitVarInsn(LLOAD, startTimeVarIdx);
                        mv.visitVarInsn(LLOAD, endTimeVarIdx);
                        mv.visitVarInsn(LLOAD, durationVarIdx);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
                        mv.visitLdcInsn(""); // extraInfo
                        mv.visitMethodInsn(INVOKESPECIAL, "site/mingsha/javaagent/methodtime/collector/MethodTimeRecord", "<init>", "(Ljava/lang/String;Ljava/lang/String;JJJLjava/lang/String;Ljava/lang/String;)V", false);
                        mv.visitMethodInsn(INVOKESTATIC, "site/mingsha/javaagent/methodtime/collector/MethodTimeBuffer", "offer", "(Lsite/mingsha/javaagent/methodtime/collector/MethodTimeRecord;)Z", false);
                        mv.visitInsn(POP);
                        mv.visitLabel(skipMinDuration);
                        
                        if (rate < 1.0) {
                            mv.visitLabel(skipRecordLabel);
                        }
                    }
                };
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
} 