package com.asm.tools.utils;

/**
 * 管理asm字节码local变量
 */
public class LocalIndexUtil {
    private static final ThreadLocal<Integer> localIndex = new ThreadLocal<>();
    /**
     * 额外局部变量声明index，0=this，1=全局StringBuffer，其它变量默认从2开始
     */
    private static final int LOCAL_BEGIN_INDEX = 2;

    public static int applyLocalIndex() {
        if(localIndex.get() == null) {
            localIndex.set(LOCAL_BEGIN_INDEX);
        }
        int index = localIndex.get() + 1;
        localIndex.set(index);
        return index;
    }

    public static void releaseLocalIndex() {
        localIndex.remove();
    }
}
