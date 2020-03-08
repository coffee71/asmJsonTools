package com.asm.tools.adapter;

import com.asm.tools.constants.ToStringHandlerConstants;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class MethoChangeClassVisitor extends ClassAdapter {

    /**
     * Constructs a new {@link ClassAdapter} object.
     *
     * @param cv the class visitor to which this adapter must delegate calls.
     */
    public MethoChangeClassVisitor(ClassVisitor cv) {
        super(cv);
    }

    //删除toString方法
    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        if (ToStringHandlerConstants.TO_JSON_METHOD_NAME.equals(name) && "()Ljava/lang/String;".equals(desc)) {
            return null;
        }
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }
}
