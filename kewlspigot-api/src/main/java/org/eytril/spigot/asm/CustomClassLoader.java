package org.eytril.spigot.asm;

public class CustomClassLoader extends ClassLoader {

    public CustomClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytecode) {
        return super.defineClass(name, bytecode, 0, bytecode.length);
    }
}