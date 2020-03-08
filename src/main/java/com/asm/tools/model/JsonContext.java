package com.asm.tools.model;

import com.asm.tools.classloader.HotspotClassLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * 用于生成toJsonString方法过程中递归传递的上下文参数
 */
public class JsonContext {
    private ClassWriter cw;
    private GeneratorAdapter ga;

    /**
     * 复杂对象嵌套时需要递归热加载类，因此需要传递classLoader
     */
    private HotspotClassLoader classLoader;
    private boolean updateClassFile;

    public JsonContext() {
    }

    public JsonContext(ClassWriter cw, GeneratorAdapter ga, HotspotClassLoader classLoader, boolean updateClassFile) {
        this.cw = cw;
        this.ga = ga;
        this.classLoader = classLoader;
        this.updateClassFile = updateClassFile;
    }

    public ClassWriter getCw() {
        return cw;
    }

    public void setCw(ClassWriter cw) {
        this.cw = cw;
    }

    public GeneratorAdapter getGa() {
        return ga;
    }

    public void setGa(GeneratorAdapter ga) {
        this.ga = ga;
    }

    public HotspotClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(HotspotClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public boolean isUpdateClassFile() {
        return updateClassFile;
    }

    public void setUpdateClassFile(boolean updateClassFile) {
        this.updateClassFile = updateClassFile;
    }
}
