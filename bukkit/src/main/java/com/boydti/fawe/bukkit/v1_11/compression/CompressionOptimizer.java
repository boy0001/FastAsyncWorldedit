package com.boydti.fawe.bukkit.v1_11.compression;

import com.boydti.fawe.util.MainUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import net.minecraft.server.v1_11_R1.RegionFile;

public class CompressionOptimizer {

    private final ClassPool pool;

    public CompressionOptimizer() {
        this.pool = ClassPool.getDefault();
    }

    public void loadSafe(String name) throws Throwable {
        try {
            pool.get(name).toClass();
        } catch (Throwable e) {
            while (e.getCause() != null) {
                e = e.getCause();
            }
            if (e instanceof ClassNotFoundException) {
                loadSafe(e.getMessage());
            }
        }
    }

    public void loadPackage(String... packages) throws IOException {
        JarInputStream jarFile = new JarInputStream(new FileInputStream(MainUtil.getJarFile()));
        JarEntry jarEntry;
        while(true) {
            jarEntry = jarFile.getNextJarEntry();
            if(jarEntry == null){
                break;
            }
            if (jarEntry.getName ().endsWith (".class")) {
                for (String p : packages) {
                    if (jarEntry.getName ().startsWith(p)) {
                        String name = jarEntry.getName().substring(0, jarEntry.getName().length() - 6).replaceAll("/", "\\.");
                        try {
                            loadSafe(name);
                        } catch (Throwable ignore) {}
                        break;
                    }
                }
            }
        }
    }

    public void optimize() throws Throwable {
//        pool.insertClassPath(new ClassClassPath(PGZIPOutputStream.class));
//        pool.insertClassPath(new ClassClassPath(PGZIPBlock.class));
//        pool.insertClassPath(new ClassClassPath(PGZIPState.class));
//        pool.insertClassPath(new ClassClassPath(PGZIPThreadLocal.class));
//        pool.insertClassPath(new ClassClassPath(ClassPath.class));
//        pool.insertClassPath(new ClassClassPath(CompressionOptimizer.class));
        pool.insertClassPath(new LoaderClassPath(this.getClass().getClassLoader()));
        pool.get("com.boydti.fawe.object.io.PGZIPOutputStream").toClass();
        pool.get("com.boydti.fawe.object.io.PGZIPBlock").toClass();
        pool.get("com.boydti.fawe.object.io.PGZIPState").toClass();
        pool.get("com.boydti.fawe.object.io.PGZIPThreadLocal").toClass();
        pool.get("com.boydti.fawe.bukkit.v1_11.compression.CompressionOptimizer").toClass();
        pool.get("javassist.ClassPath").toClass();
        pool.importPackage("net.minecraft.server.v1_11_R1");
        pool.importPackage("java.lang");
        pool.importPackage("java.lang.reflect");
        pool.importPackage("java.io");
        pool.importPackage("com.boydti.fawe.bukkit.v1_11.compression");
//        RegionFile.class.getDeclaredClasses()[0];


        { // Optimize NBTCompressedStreamTools
            CtClass clazz = pool.get("net.minecraft.server.v1_11_R1.NBTCompressedStreamTools");
            CtMethod methodA_getStream = clazz.getDeclaredMethod("a", new CtClass[]{pool.get("net.minecraft.server.v1_11_R1.NBTTagCompound"), pool.get("java.io.OutputStream")});
            methodA_getStream.setBody("{" +
                    "java.io.DataOutputStream dataoutputstream = new java.io.DataOutputStream(new java.io.BufferedOutputStream(new com.boydti.fawe.object.io.PGZIPOutputStream($2)));" +
                    "try {" +
                    "    a($1, (java.io.DataOutput) dataoutputstream);" +
                    "} finally {" +
                    "    dataoutputstream.close();" +
                    "}" +
                    "}");
            clazz.toClass();
        }

        { // Optimize RegionFile
            CtClass clazz = pool.get("net.minecraft.server.v1_11_R1.RegionFile");
            CtMethod methodB_getStream = clazz.getDeclaredMethod("b", new CtClass[]{CtClass.intType, CtClass.intType});
            methodB_getStream.setBody("{" +
                    "Constructor constructor = $0.getClass().getDeclaredClasses()[0].getConstructors()[0];" +
                    "            constructor.setAccessible(true);" +
                    "            return $0.d($1, $2) ? null : new java.io.DataOutputStream(new java.io.BufferedOutputStream(new com.boydti.fawe.object.io.PGZIPOutputStream((OutputStream) CompressionOptimizer.newInstance(constructor, $0, $1, $2))));" +
                    "}");
            clazz.toClass();

//            RegionFile $0 = null;
//            int $1 = 0;
//            int $2 = 0;
//
//            Constructor<?> constructor = $0.getClass().getDeclaredClasses()[0].getConstructors()[0];
//            constructor.setAccessible(true);
//            return $0.d($1, $2) ? null : new java.io.DataOutputStream(new java.io.BufferedOutputStream(new com.boydti.fawe.object.io.PGZIPOutputStream((OutputStream) constructor.newInstance($1, $2))));
        }
    }

    public static Object newInstance(Constructor constructor, RegionFile file, int a, int b) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return constructor.newInstance(file, a, b);
    }
}
