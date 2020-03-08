package com.asm.tools.handler.impl;

import com.asm.tools.AsmJson;
import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.exception.AsmBusinessException;
import com.asm.tools.handler.ToStringHandler;
import com.asm.tools.handler.ToStringHandlerFactory;
import com.asm.tools.model.JsonContext;
import com.asm.tools.utils.ClassUtils;
import com.asm.tools.utils.LocalIndexUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public abstract class ArrayCollectionToStringHandler extends BaseToStringHandler {
    /**
     * 将属性value append到StringBuffer
     *
     * @param context
     * @param clazz
     * @param field
     */
    @Override
    public void appendValue(JsonContext context, Class clazz, Field field) {
        GeneratorAdapter ga = context.getGa();
        try {
            ga.visitLdcInsn("[");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

            loadFieldValue(ga, clazz, field.getName(), field.getType());

            //for循环遍历数组
            Class elementClass = getElementClass(field);

            int arrayIndex = LocalIndexUtil.applyLocalIndex();
            ga.visitVarInsn(ASTORE, arrayIndex);
            ga.visitVarInsn(ALOAD, arrayIndex);

            int lengthIndex = loadLength(ga);
            //判断数组/集合大小,等于0的时候就直接拼"]"return
            endIfEmpty(ga);


            int iIndex = LocalIndexUtil.applyLocalIndex();
            //初始化循环体i值
            ga.visitInsn(ICONST_0);
            ga.visitVarInsn(ISTORE, iIndex);
            //for循环体
            Label forEachCycleLabel = new Label();
            ga.visitLabel(forEachCycleLabel);
            ga.visitVarInsn(ILOAD, iIndex);
            ga.visitVarInsn(ILOAD, lengthIndex);
            //for循环条件判断跳转
            Label forEachBreakLabel = new Label();
            //如果 i >= array.length 跳出循环
            ga.visitJumpInsn(IF_ICMPGE, forEachBreakLabel);

            ga.visitVarInsn(ALOAD, arrayIndex);
            ga.visitVarInsn(ILOAD, iIndex);

            /*
             指令格式：  aaload
             功能描述：  栈顶的数组下标（index）、数组引用
             （arrayref）出栈，并根据这两个数值
             取出对应的数组元素值（value）进栈。
             */
            ga.visitInsn(AALOAD);
            ga.visitTypeInsn(CHECKCAST, ClassUtils.getClazzName(elementClass));

            //元素生成json-value
            //将Class clazz 参数由clazz改为传field.getClass()，递归调用时clazz应该传的是正在解析的类型  2020.03.08
            appendElementValue(context, field, field.getClass(), elementClass);
            //判断如果当前不是最后一个元素则加,
            appendComma(ga, iIndex, lengthIndex);
            //i++
            ga.visitIincInsn(iIndex, 1);
            //开始下一轮循环
            ga.visitJumpInsn(GOTO, forEachCycleLabel);
            ga.visitLabel(forEachBreakLabel);
            //for循环体end

            ga.visitLdcInsn("]");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            ga.visitLdcInsn(",");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        } catch (Throwable e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
    }

    /**
     * 判断数组/集合大小,等于0的时候就直接拼"]"return
     * @param ga
     */
    private void endIfEmpty(GeneratorAdapter ga) {
    }

    /**
     * 拼接逗号
     * @param ga
     * @param iIndex
     * @param arrayLengthIndex
     */
    /**
     * if(i < arr.length-1) {
     *     StringBuffer.append(",");
     * }
     * @param ga
     * @param iIndex
     * @param lengthIndex
     */
    protected void appendComma(GeneratorAdapter ga, int iIndex, int lengthIndex) {
        ga.visitVarInsn(ILOAD, iIndex);
        //计算length - 1
        ga.visitVarInsn(ILOAD, lengthIndex);
        int lastIndex = LocalIndexUtil.applyLocalIndex();
        ga.visitVarInsn(ISTORE, lastIndex);
        ga.visitIincInsn(lastIndex, -1);
        ga.visitVarInsn(ILOAD, lastIndex);

        Label lengthJudgeLabel = new Label();
        ga.visitJumpInsn(IF_ICMPGE, lengthJudgeLabel);
        ga.visitLdcInsn(",");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitLabel(lengthJudgeLabel);

    }


    /**
     * 集合元素生成json-value
     * @param context
     * @param field
     * @param clazz
     * @param elementClass
     */
    protected void appendElementValue(JsonContext context, Field field, Class clazz, Class elementClass) {
        GeneratorAdapter ga = context.getGa();

        ToStringHandler elementHandler = ToStringHandlerFactory.getInstance().getToStringHandler(elementClass);

        if (elementClass.isPrimitive()) {
            //基础类型 直接append
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", String.format("(%s)Ljava/lang/StringBuffer;", Type.getType(elementClass).getDescriptor()));
        } else if (Number.class.isAssignableFrom(elementClass)) {
            //数字类型
            ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(elementClass), "toString", "()Ljava/lang/String;");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        } else if (CharSequence.class.isAssignableFrom(elementClass) || Character.class.isAssignableFrom(elementClass)) {
            int elementIndex = LocalIndexUtil.applyLocalIndex();
            ga.visitVarInsn(ASTORE, elementIndex);
            //字符(串)
            ga.visitLdcInsn("\"");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            ga.visitVarInsn(ALOAD, elementIndex);
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", String.format("(%s)Ljava/lang/StringBuffer;", Type.getType(elementClass).getDescriptor()));
            ga.visitLdcInsn("\"");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        } else if (elementClass.isArray() || Collection.class.isAssignableFrom(elementClass)) {
            //数组、集合、Map直接赋值就行
            ((ArrayCollectionToStringHandler)elementHandler).appendValueRecursion(context, field, clazz, elementClass);
        } else if(Map.class.isAssignableFrom(elementClass)) {
            ToStringHandler toStringHandler = ToStringHandlerFactory.getInstance().getToStringHandler(Map.class);
            toStringHandler.appendValue(context, clazz, field);
        } else {
            //复杂对象
            int elementIndex = LocalIndexUtil.applyLocalIndex();
            ga.visitVarInsn(ASTORE, elementIndex);
            AsmJson.overwriteToJsonStringInner(elementClass, context.getClassLoader(), context.isUpdateClassFile());
            ga.visitVarInsn(ALOAD, elementIndex);
            ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(elementClass), ToStringHandlerConstants.TO_JSON_METHOD_NAME, "()Ljava/lang/String;");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        }
    }

    protected abstract Class getElementClass(Field field);

    protected int loadLength(GeneratorAdapter ga) {
        //读取数组长度
        ga.visitInsn(ARRAYLENGTH);
        int lengthIndex = LocalIndexUtil.applyLocalIndex();
        ga.visitVarInsn(ISTORE, lengthIndex);
        return lengthIndex;
    }


    /**
     * 多维数组递归拼接jsonArray
     *
     * @param context
     * @param field
     * @param clazz
     * @param arrayFieldClass
     */
    public void appendValueRecursion(JsonContext context, Field field, Class clazz, Class arrayFieldClass) {
        GeneratorAdapter ga = context.getGa();
        try {
            int elementIndex = LocalIndexUtil.applyLocalIndex();
            ga.visitVarInsn(ASTORE, elementIndex);
            ga.visitLdcInsn("[");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

            ga.visitVarInsn(ALOAD, elementIndex);
            //for循环遍历数组
            //获取数组元素类型
            Class elementClass = arrayFieldClass.getComponentType();

            int arrayIndex = LocalIndexUtil.applyLocalIndex();
            ga.visitVarInsn(ASTORE, arrayIndex);
            ga.visitVarInsn(ALOAD, arrayIndex);

            //读取数组长度
            ga.visitInsn(ARRAYLENGTH);
            int arrayLengthIndex = LocalIndexUtil.applyLocalIndex();
            ga.visitVarInsn(ISTORE, arrayLengthIndex);
            int iIndex = LocalIndexUtil.applyLocalIndex();
            ga.visitInsn(ICONST_0);
            ga.visitVarInsn(ISTORE, iIndex);
            //for循环体
            Label forEachCycleLabel = new Label();
            ga.visitLabel(forEachCycleLabel);
            ga.visitVarInsn(ILOAD, iIndex);
            ga.visitVarInsn(ILOAD, arrayLengthIndex);
            //for循环条件判断跳转
            Label forEachBreakLabel = new Label();
            //如果 i >= array.length 跳出循环
            ga.visitJumpInsn(IF_ICMPGE, forEachBreakLabel);

            ga.visitVarInsn(ALOAD, arrayIndex);
            ga.visitVarInsn(ILOAD, iIndex);
            /*
             指令格式：  aaload
             功能描述：  栈顶的数组下标（index）、数组引用
             （arrayref）出栈，并根据这两个数值
             取出对应的数组元素值（value）进栈。
             */
            ga.visitInsn(AALOAD);
//            int elementIndex = LocalIndexUtil.applyLocalIndex();
//            ga.visitVarInsn(ASTORE, elementIndex);
            //元素生成json-value
            appendElementValue(context, field, clazz, elementClass);
            //判断如果当前不是最后一个元素则加,
            appendComma(ga, iIndex, arrayLengthIndex);
            //i++
            ga.visitIincInsn(iIndex, 1);
            //开始下一轮循环
            ga.visitJumpInsn(GOTO, forEachCycleLabel);
            ga.visitLabel(forEachBreakLabel);
            //for循环体end

            ga.visitLdcInsn("]");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        } catch (Throwable e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
    }
}
