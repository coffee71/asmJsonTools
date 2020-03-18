package com.asm.tools.handler.impl;

import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.model.JsonContext;
import com.asm.tools.model.GenericInfo;
import com.asm.tools.utils.ClassUtils;
import com.asm.tools.utils.LocalIndexUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * todo 跟arry的实现解耦
 * Map属性toString
 * 重要：为了简化实现，和保证性能（避免运行时递归动态修改类）目前要求Map必须带有泛型
 */
public class MapToStringHandler extends ArrayCollectionToStringHandler {

    @Override
    public void appendValue(JsonContext context, Class clazz, Field field) {
        GeneratorAdapter ga = context.getGa();
        //获取map上的泛型
        GenericInfo mapGeneric = ClassUtils.getGenericTypeInfo(field.getType(), (ParameterizedType) field.getGenericType());

        super.loadFieldValue(ga, clazz, field.getName(), field.getType());

        appendMap(context, mapGeneric);
        ga.visitLdcInsn(",");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    }

    /**
     * 调用Map.entrySet 的iterator，然后遍历生成Map的json
     * @param context
     * @param genericInfo
     */
    public void appendMap(JsonContext context, GenericInfo genericInfo) {
        GeneratorAdapter ga = context.getGa();
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        ga.visitLdcInsn("{");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitInsn(POP);

        //实际声明类型可能是接口也可能是具体类型，因此这里统一用接口处理
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;");
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;");
        //用iterator遍历entitySet begin
        int iteratorIndex = LocalIndexUtil.applyLocalIndex();
        ga.visitVarInsn(ASTORE, iteratorIndex);
        Label iteratorJudgeLabel = new Label();
        ga.visitLabel(iteratorJudgeLabel);
        ga.visitVarInsn(ALOAD, iteratorIndex);
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
        Label judgeEndLabel = new Label();
        ga.visitJumpInsn(IFEQ, judgeEndLabel);
        ga.visitVarInsn(ALOAD, iteratorIndex);
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
        ga.visitTypeInsn(CHECKCAST, "java/util/Map$Entry");
        int entityIndex = LocalIndexUtil.applyLocalIndex();
        ga.visitVarInsn(ASTORE, entityIndex);

        appendMapKey(context, genericInfo, entityIndex);
        ga.visitLdcInsn(":");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        appendMapValue(context, genericInfo, entityIndex);

        ga.visitVarInsn(ALOAD, iteratorIndex);
        //如果不是最后一个元素则拼接","

        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
        Label lastOneLabel = new Label();
        ga.visitJumpInsn(IFEQ, lastOneLabel);
        ga.visitLdcInsn(",");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitLabel(lastOneLabel);


        ga.visitJumpInsn(GOTO, iteratorJudgeLabel);
        ga.visitLabel(judgeEndLabel);


        //用iterator遍历entitySet end
        ga.visitInsn(POP);
        //封闭json
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        ga.visitLdcInsn("}");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    }

    public void appendMapKey(JsonContext context, GenericInfo mapGeneric, int entityIndex) {
        GeneratorAdapter ga = context.getGa();
        Class keyClass = mapGeneric.getKeyClass();

        ga.visitVarInsn(ALOAD, entityIndex);

        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;");
        ga.visitTypeInsn(CHECKCAST, ClassUtils.getClazzName(keyClass));
        boolean isMapValue = Map.class.isAssignableFrom(keyClass);

        if(isMapValue) {
            appendMap(context, mapGeneric.getKeyMapInfo());
        } else {
            super.appendElementValue(context, keyClass, mapGeneric.getKeyMapInfo());
        }
    }

    /**
     * 直接覆盖了父类 appendValue，Map的泛型涉及key-value，无法用单个元素类型表达。
     * @param field
     * @return
     */
    @Override
    protected Class getElementClass(Field field) {
        return null;
    }

    private void appendMapValue(JsonContext context, GenericInfo mapGeneric, int entityIndex) {
        GeneratorAdapter ga = context.getGa();
        Class valueClass = mapGeneric.getValueClass();

        ga.visitVarInsn(ALOAD, entityIndex);

        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;");
        ga.visitTypeInsn(CHECKCAST, ClassUtils.getClazzName(valueClass));
        boolean isMapValue = Map.class.isAssignableFrom(valueClass);

        if(isMapValue) {
            appendMap(context, mapGeneric.getValueMapInfo());
        } else {
            super.appendElementValue(context, valueClass, mapGeneric.getValueMapInfo());
        }
    }
}
