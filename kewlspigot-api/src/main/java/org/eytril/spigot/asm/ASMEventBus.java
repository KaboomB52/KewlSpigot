package org.eytril.spigot.asm;

import com.avaje.ebean.enhance.asm.ClassWriter;
import com.avaje.ebean.enhance.asm.MethodVisitor;
import com.avaje.ebean.enhance.asm.Opcodes;
import com.avaje.ebean.enhance.asm.Type;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ASM-backed Bukkit event bus.
 */
public final class ASMEventBus {

    private static final String GENERATED_PACKAGE =
            "org/eytril/spigot/asm/generated/";

    private final Map<Class<? extends Event>, CopyOnWriteArrayList<ListenerHandlerPair>> handlers =
            new ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArrayList<ListenerHandlerPair>>();

    private final AtomicLong registrationCounter = new AtomicLong();

    private final Comparator<ListenerHandlerPair> priorityComparator =
            new Comparator<ListenerHandlerPair>() {
                @Override
                public int compare(ListenerHandlerPair first, ListenerHandlerPair second) {
                    int priority = Integer.compare(
                            first.getPriority().ordinal(),
                            second.getPriority().ordinal()
                    );

                    if (priority != 0) {
                        return priority;
                    }

                    return Long.compare(
                            first.getRegistrationOrder(),
                            second.getRegistrationOrder()
                    );
                }
            };

    /**
     * Registers all @EventHandler methods in a listener.
     */
    public void registerListener(Listener listener, Plugin plugin) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        for (Class<?> type = listener.getClass();
             type != null && type != Object.class;
             type = type.getSuperclass()) {

            Method[] methods = type.getDeclaredMethods();

            for (Method method : methods) {
                EventHandler annotation = method.getAnnotation(EventHandler.class);

                if (annotation == null) {
                    continue;
                }

                validateEventMethod(method);

                @SuppressWarnings("unchecked")
                Class<? extends Event> eventType =
                        (Class<? extends Event>) method.getParameterTypes()[0];

                ASMEventHandler generatedHandler = generateHandler(listener, method);

                registerListener(
                        listener,
                        plugin,
                        eventType,
                        annotation.priority(),
                        annotation.ignoreCancelled(),
                        generatedHandler,
                        null
                );
            }
        }
    }

    /**
     * Registers a direct EventExecutor.
     *
     * This keeps compatibility with Bukkit's registerEvent(...) API.
     */
    public void registerExecutor(
            Class<? extends Event> eventType,
            Listener listener,
            EventPriority priority,
            EventExecutor executor,
            Plugin plugin,
            boolean ignoreCancelled
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }

        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }

        if (executor == null) {
            throw new IllegalArgumentException("Executor cannot be null");
        }

        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        ASMEventHandler handler = new ASMEventHandler() {
            @Override
            public void handle(Event event) throws Throwable {
                executor.execute(listener, event);
            }
        };

        registerListener(
                listener,
                plugin,
                eventType,
                priority,
                ignoreCancelled,
                handler,
                executor
        );
    }

    private void registerListener(
            Listener listener,
            Plugin plugin,
            Class<? extends Event> eventType,
            EventPriority priority,
            boolean ignoreCancelled,
            ASMEventHandler handler,
            EventExecutor executor
    ) {
        CopyOnWriteArrayList<ListenerHandlerPair> eventHandlers =
                handlers.get(eventType);

        if (eventHandlers == null) {
            CopyOnWriteArrayList<ListenerHandlerPair> created =
                    new CopyOnWriteArrayList<ListenerHandlerPair>();

            CopyOnWriteArrayList<ListenerHandlerPair> existing =
                    handlers.putIfAbsent(eventType, created);

            eventHandlers = existing == null ? created : existing;
        }

        eventHandlers.add(new ListenerHandlerPair(
                listener,
                plugin,
                eventType,
                priority,
                ignoreCancelled,
                handler,
                executor,
                registrationCounter.getAndIncrement()
        ));
    }

    /**
     * Dispatches an event through every matching registered handler.
     *
     * @throws EventDispatchException when a listener throws
     */
    public void dispatchEvent(Event event) throws EventDispatchException {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        List<ListenerHandlerPair> matchingHandlers =
                new ArrayList<ListenerHandlerPair>();

        for (Map.Entry<Class<? extends Event>, CopyOnWriteArrayList<ListenerHandlerPair>> entry
                : handlers.entrySet()) {

            if (!entry.getKey().isAssignableFrom(event.getClass())) {
                continue;
            }

            for (ListenerHandlerPair registration : entry.getValue()) {
                if (registration.accepts(event)) {
                    matchingHandlers.add(registration);
                }
            }
        }

        Collections.sort(matchingHandlers, priorityComparator);

        for (ListenerHandlerPair registration : matchingHandlers) {
            Plugin plugin = registration.getPlugin();

            if (plugin != null && !plugin.isEnabled()) {
                continue;
            }

            if (registration.isIgnoreCancelled()
                    && event instanceof Cancellable
                    && ((Cancellable) event).isCancelled()) {
                continue;
            }

            try {
                registration.getHandler().handle(event);
            } catch (Throwable throwable) {
                throw new EventDispatchException(
                        registration,
                        event,
                        throwable
                );
            }
        }
    }

    /**
     * Removes all handlers owned by a plugin.
     */
    public void unregisterPlugin(Plugin plugin) {
        if (plugin == null) {
            return;
        }

        for (CopyOnWriteArrayList<ListenerHandlerPair> eventHandlers
                : handlers.values()) {

            for (ListenerHandlerPair registration : eventHandlers) {
                if (plugin.equals(registration.getPlugin())) {
                    eventHandlers.remove(registration);
                }
            }
        }

        removeEmptyLists();
    }

    /**
     * Removes all handlers belonging to a listener instance.
     */
    public void unregisterListener(Listener listener) {
        if (listener == null) {
            return;
        }

        for (CopyOnWriteArrayList<ListenerHandlerPair> eventHandlers
                : handlers.values()) {

            for (ListenerHandlerPair registration : eventHandlers) {
                if (listener.equals(registration.getListener())) {
                    eventHandlers.remove(registration);
                }
            }
        }

        removeEmptyLists();
    }

    /**
     * Removes every registered handler.
     */
    public void clear() {
        handlers.clear();
    }

    private void removeEmptyLists() {
        for (Map.Entry<Class<? extends Event>, CopyOnWriteArrayList<ListenerHandlerPair>> entry
                : handlers.entrySet()) {

            if (entry.getValue().isEmpty()) {
                handlers.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Generates a handler whose handle(...) method directly invokes the
     * listener method through generated bytecode.
     */
    public ASMEventHandler generateHandler(
            Listener listener,
            Method method
    ) {
        if (!Modifier.isPublic(method.getModifiers())
                || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            /*
             * Generated bytecode cannot directly invoke inaccessible methods.
             * Bukkit event methods are normally public, but this fallback keeps
             * unusual plugins from crashing during registration.
             */
            method.setAccessible(true);

            return new ASMEventHandler() {
                @Override
                public void handle(Event event) throws Throwable {
                    method.invoke(listener, event);
                }
            };
        }

        try {
            String generatedName = GENERATED_PACKAGE
                    + method.getDeclaringClass().getName()
                    .replace('.', '_')
                    + "_"
                    + method.getName()
                    + "_"
                    + registrationCounter.get();

            String generatedInternalName = generatedName.replace('.', '/');
            String listenerInternalName =
                    Type.getInternalName(listener.getClass());

            String eventInternalName =
                    Type.getInternalName(method.getParameterTypes()[0]);

            String handlerInternalName =
                    Type.getInternalName(ASMEventHandler.class);

            String listenerDescriptor =
                    Type.getDescriptor(listener.getClass());

            String eventDescriptor =
                    Type.getDescriptor(method.getParameterTypes()[0]);

            String methodDescriptor =
                    "(" + eventDescriptor + ")V";

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

            writer.visit(
                    0 << 16 | 51, // 1_7
                    Opcodes.ACC_PUBLIC
                            | Opcodes.ACC_FINAL
                            | Opcodes.ACC_SUPER,
                    generatedInternalName,
                    null,
                    Type.getInternalName(Object.class),
                    new String[]{handlerInternalName}
            );

            writer.visitField(
                    Opcodes.ACC_PRIVATE
                            | Opcodes.ACC_FINAL,
                    "listener",
                    listenerDescriptor,
                    null,
                    null
            ).visitEnd();

            MethodVisitor constructor = writer.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    "<init>",
                    "(" + listenerDescriptor + ")V",
                    null,
                    null
            );

            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    Type.getInternalName(Object.class),
                    "<init>",
                    "()V"
            );

            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitVarInsn(Opcodes.ALOAD, 1);
            constructor.visitFieldInsn(
                    Opcodes.PUTFIELD,
                    generatedInternalName,
                    "listener",
                    listenerDescriptor
            );
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(0, 0);
            constructor.visitEnd();

            MethodVisitor handle = writer.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    "handle",
                    "(L" + Type.getInternalName(Event.class) + ";)V",
                    null,
                    new String[]{Type.getInternalName(Throwable.class)}
            );

            handle.visitCode();

            handle.visitVarInsn(Opcodes.ALOAD, 0);
            handle.visitFieldInsn(
                    Opcodes.GETFIELD,
                    generatedInternalName,
                    "listener",
                    listenerDescriptor
            );

            handle.visitVarInsn(Opcodes.ALOAD, 1);
            handle.visitTypeInsn(Opcodes.CHECKCAST, eventInternalName);

            int invokeOpcode = method.getDeclaringClass().isInterface()
                    ? Opcodes.INVOKEINTERFACE
                    : Opcodes.INVOKEVIRTUAL;

            handle.visitMethodInsn(
                    invokeOpcode,
                    Type.getInternalName(method.getDeclaringClass()),
                    method.getName(),
                    methodDescriptor
            );

            handle.visitInsn(Opcodes.RETURN);
            handle.visitMaxs(0, 0);
            handle.visitEnd();

            writer.visitEnd();

            byte[] bytecode = writer.toByteArray();

            ClassLoader parent = listener.getClass().getClassLoader();

            if (parent == null) {
                parent = ASMEventBus.class.getClassLoader();
            }

            CustomClassLoader classLoader = new CustomClassLoader(parent);
            Class<?> generatedClass =
                    classLoader.defineClass(generatedName, bytecode);

            Constructor<?> constructord =
                    generatedClass.getConstructor(listener.getClass());

            return (ASMEventHandler) constructord.newInstance(listener);

        } catch (Throwable failure) {
            /*
             * Safe fallback for old ASM versions, unusual class loaders, or
             * listener classes that cannot be referenced from generated code.
             */
            method.setAccessible(true);

            return new ASMEventHandler() {
                @Override
                public void handle(Event event) throws Throwable {
                    method.invoke(listener, event);
                }
            };
        }
    }

    private void validateEventMethod(Method method) {
        if (method.getParameterTypes().length != 1) {
            throw new IllegalArgumentException(
                    "Event handler " + method
                            + " must have exactly one parameter"
            );
        }

        if (!Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new IllegalArgumentException(
                    "Event handler " + method
                            + " parameter must extend Event"
            );
        }

        if (method.getReturnType() != Void.TYPE) {
            throw new IllegalArgumentException(
                    "Event handler " + method
                            + " must return void"
            );

        }

        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(
                    "Event handler " + method
                            + " cannot be static"
            );
        }
    }
}