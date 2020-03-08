package com.asm.tools.model;

import com.asm.tools.exception.AsmBusinessException;

/**
 * Map 泛型信息，递归解析
 */
public class GenericInfo {

    public String getKeyClassName() {
        return keyClassName;
    }

    public String getValueClassName() {
        return valueClassName;
    }

    /**
     * key全类名
     */
    private String keyClassName;

    /**
     * value全类名
     */
    private String valueClassName;

    /**
     * key类型
     */
    private Class keyClass;

    /**
     * 当key 是Map时不为空，存放key的泛型信息
     */
    private GenericInfo keyMapInfo;

    /**
     * value类型
     */
    private Class valueClass;
    /**
     * 当value 是map时不为空，存放value的泛型信息
     */
    private GenericInfo valueMapInfo;

    public GenericInfo(String keyClassName, String valueClassName) {
        this.keyClassName = keyClassName;
        this.valueClassName = valueClassName;

        try {
            if(keyClassName.indexOf("<") >= 0) {
                keyClassName = keyClassName.substring(0, keyClassName.indexOf("<"));
            }
            if(valueClassName.indexOf("<") >= 0) {
                valueClassName = valueClassName.substring(0, valueClassName.indexOf("<"));
            }

            this.keyClass = Class.forName(keyClassName);
            this.valueClass = Class.forName(valueClassName);
        } catch (Exception e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
    }

    public GenericInfo(String valueClassName) {
        this.valueClassName = valueClassName;

        try {
            if(valueClassName.indexOf("<") >= 0) {
                valueClassName = valueClassName.substring(0, valueClassName.indexOf("<"));
            }
            this.valueClass = Class.forName(valueClassName);
        } catch (Exception e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
    }

    public Class getKeyClass() {
        return keyClass;
    }

    public GenericInfo getKeyMapInfo() {
        return keyMapInfo;
    }

    public void setKeyMapInfo(GenericInfo keyMapInfo) {
        this.keyMapInfo = keyMapInfo;
    }

    public Class getValueClass() {
        return valueClass;
    }


    public GenericInfo getValueMapInfo() {
        return valueMapInfo;
    }

    public void setValueMapInfo(GenericInfo valueMapInfo) {
        this.valueMapInfo = valueMapInfo;
    }

    @Override
    public String toString() {
        return "MapGeneric{" +
                "keyClassName='" + keyClassName + '\'' +
                ", valueClassName='" + valueClassName + '\'' +
                ", keyClass=" + keyClass.getCanonicalName() +
                ", keyMapInfo=" + keyMapInfo +
                ", valueClass=" + valueClass.getCanonicalName() +
                ", valueMapInfo=" + valueMapInfo +
                '}';
    }
}
