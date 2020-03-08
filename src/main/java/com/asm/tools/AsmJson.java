package com.asm.tools;

import com.asm.tools.adapter.MethoChangeClassVisitor;
import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.exception.AsmBusinessException;
import com.asm.tools.handler.ToStringHandler;
import com.asm.tools.handler.ToStringHandlerFactory;
import com.asm.tools.utils.ClassUtils;
import com.asm.tools.utils.LocalIndexUtil;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

/**
 * https://www.cnblogs.com/mylove7/articles/5381544.html
 * TODO 1、需要考虑怎么处理jar中的class？ 2、数组集合中的null和空数组处理
 * 使用asm重写toString方法，为类型对象生成json格式字符串（等同于JSONObject.toJSONString())
 * 注意：
 * 1、Map、集合必须声明泛型
 * 2、
 */
public class AsmJson {

    /**
     * 通过asm重写对象toString，并且不更新原来class文件
     *
     * @param clazz
     */
    public static void overwriteToJsonString(Class clazz) throws Throwable {
        overwriteToJsonString(clazz, false);
    }

    /**
     * 通过asm重写对象toString
     *
     * @param clazz
     * @param updateClassFile 是否需要更新class文件（写到磁盘）
     */
    public static Class overwriteToJsonString(Class clazz, boolean updateClassFile) throws Throwable {
        return overwriteToJsonString(clazz, new HotspotClassLoader(), updateClassFile);
    }

    /**
     * 通过asm重写对象toString
     *
     * @param clazz
     * @param hotspotClassLoader
     * @param updateClassFile 是否需要更新class文件（写到磁盘）
     */
    public static Class overwriteToJsonString(Class clazz, HotspotClassLoader hotspotClassLoader, boolean updateClassFile) {
        try {
            return overwriteToJsonStringInner(clazz, hotspotClassLoader, updateClassFile);
        } finally {
            //释放线程变量
            LocalIndexUtil.releaseLocalIndex();
        }
    }

    /**
     * 为目标类生成toJsonString方法，但是不包含释放变量资源（用于内部递归编译调用），外部入口请调用overwriteToJsonString
     * @param clazz
     * @param hotspotClassLoader
     * @param updateClassFile
     * @return
     */
    public static Class overwriteToJsonStringInner(Class clazz, HotspotClassLoader hotspotClassLoader, boolean updateClassFile) {
        try {
            ClassReader cr = new ClassReader(clazz.getName());

            //对本地变量和操作数栈的大小设置受ClassWriter的flag取值影响：
            //
            //（1）new ClassWriter(0),表明需要手动计算栈帧大小、本地变量和操作数栈的大小；
            //
            //（2）new ClassWriter(ClassWriter.COMPUTE_MAXS)需要自己计算栈帧大小，但本地变量与操作数已自动计算好，当然也可以调用visitMaxs方法，只不过不起作用，参数会被忽略；
            //
            //（3）new ClassWriter(ClassWriter.COMPUTE_FRAMES)栈帧本地变量和操作数栈都自动计算，不需要调用visitFrame和visitMaxs方法，即使调用也会被忽略。
            //ASM GeneratorAdapter 屏蔽了visitFrame很多操作，所以这里一定要用COMPUTE_FRAMES，否则生成if else、for等控制语句时出错
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new MethoChangeClassVisitor(cw);
            //如果存在toString方法先删除
            cr.accept(cv, F_FULL);
            //新增加一个方法
            generateToStringMethod(cw, clazz, hotspotClassLoader, updateClassFile);

            byte[] updatedClassBytes = cw.toByteArray();
            //返回的对象时被加载类的class
            if (updateClassFile) {
                //TODO 需要考虑怎么处理jar中的class？
                ClassUtils.saveClassFile(cw, clazz);
//                return null;
            }
//            else {
                Class updatedClass = hotspotClassLoader.defineClassByName(clazz.getName(), updatedClassBytes, 0, updatedClassBytes.length);
                return updatedClass;
//            }
        } catch (Throwable e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
    }

    /**
     * 生成toString toString方法，将对象返回json格式字符串，值为空的变量不生成key
     *
     * @param cw
     * @param clazz
     * @param classLoader
     * @param updateClassFile
     */
    private static void generateToStringMethod(ClassWriter cw, Class clazz, HotspotClassLoader classLoader, boolean updateClassFile) {
        //声明toString方法
        Type StringType = Type.getType(String.class);
        Method toStringMethod = new Method(ToStringHandlerConstants.TO_JSON_METHOD_NAME, StringType, new Type[]{});
        //signature标注是方法签名，不知道有什么用，可以传空
        MethodVisitor methodVisitor = cw.visitMethod(Opcodes.ACC_PUBLIC, toStringMethod.getName(), toStringMethod.getDescriptor(), null, null);
        GeneratorAdapter ga = new GeneratorAdapter(methodVisitor, Opcodes.ACC_PUBLIC, toStringMethod.getName(), toStringMethod.getDescriptor());
        ga.visitCode();

        //new 一个StringBuffer用来拼接json字符串
        ga.visitTypeInsn(NEW, "java/lang/StringBuffer");
        //将变量声明的指针压栈一次，因为接下来new 赋值会调用弹出2次
        ga.visitInsn(DUP);
        //声明StringBuffer并将刚才new的对象赋值
        ga.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V");
        //保存到变量表
        ga.visitVarInsn(ASTORE, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        ga.visitLdcInsn("{");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitInsn(POP);

        ToStringHandlerFactory toStringHandlerFactory = ToStringHandlerFactory.getInstance();
        //遍历属性生成对应的key-value
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field -> ClassUtils.isAccessAble(clazz, field)).collect(Collectors.toList());
        for (Field field : fields) {
            ToStringHandler handler = toStringHandlerFactory.getToStringHandler(field.getType());
            handler.toJsonStringIfNotNull(cw, ga, clazz, field, classLoader, updateClassFile);
        }
        //删除StringBuffer多余的逗号,并补充终结符}
        removeLastComma(ga);

        ga.returnValue();
        ga.endMethod();
    }

    /**
     * 删除StringBuffer多余的逗号,并补充终结符}
     * @param ga
     */
    private static void removeLastComma(GeneratorAdapter ga) {
        //这里为了方便理解用了jump-goto的指令实现if{return;}else{return;}，可以简化为只用jump(if{return;} return;
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "length", "()I");
        ga.visitInsn(ICONST_1);
        Label sizeJudgeLabel = new Label();
        //StringBuffer.length() < 1跳转
        ga.visitJumpInsn(IF_ICMPLE, sizeJudgeLabel);

        //简化程序提前压栈为 StringBuffer.substring(0,1)做准备
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        ga.visitInsn(ICONST_0);
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        //计算StringBuffer.length() - 1
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "length", "()I");
        ga.visitInsn(ICONST_1);
        ga.visitInsn(ISUB);

        //调用StringBuffer.substring(0,1)
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "substring", "(II)Ljava/lang/String;");
        int localIndex = LocalIndexUtil.applyLocalIndex();
        ga.visitVarInsn(ASTORE, localIndex);

        ga.visitTypeInsn(NEW, "java/lang/StringBuffer");
        ga.visitInsn(DUP);
        ga.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V");
        ga.visitVarInsn(ALOAD, localIndex);
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitLdcInsn("}");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        //调用StringBuffer.toString方法
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");

        //进了if分支则跳过else代码
        Label elseLabel = new Label();
        ga.visitJumpInsn(GOTO, elseLabel);
        ga.visitLabel(sizeJudgeLabel);

        //else 代码
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        ga.visitLdcInsn("}");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
        ga.visitLabel(elseLabel);
    }
}
