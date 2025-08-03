package com.katok.arbalester.utills;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.jetbrains.annotations.Nullable;

public class DContainer {
    private final HashMap<Class<?>, Supplier<?>> providers = new HashMap<>();
    private final HashMap<Class<?>, Object> singletones = new HashMap<>();

    public <T> void register(Class<T> clazz, Supplier<T> provider) {
        if(providers.containsKey(clazz)) return;

        providers.put(clazz, provider);
    } 

    public <T> void registerSingletone(Class<T> clazz, Object singletone) {
        if(singletones.containsKey(clazz)) return;

        singletones.put(clazz, singletone);
    }

    @Nullable
    public <T> T get(Class<T> clazz) {
        Object result = singletones.get(clazz);
        if(result == null || !clazz.isInstance(result)) {
            return null;
        }
        
        return clazz.cast(result);
    }

    @Nullable
    public <T> T create(Class<T> clazz) {
        if(providers.containsKey(clazz)) return clazz.cast(((Supplier<?>) providers.get(clazz)).get());

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> injectConstructor = null;

        for(Constructor<?> constructor: constructors) {
            if(!constructor.isAnnotationPresent(Inject.class)) continue;

            if(injectConstructor != null) throw new RuntimeException("Найден больше чем один конструктор в " + clazz.getName());
            injectConstructor = constructor;
        }

        if(injectConstructor == null) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ignored) {
                return null;
            }
        }

        Object[] parameters = Arrays.stream(injectConstructor.getParameterTypes())
                                .map(this::resolve)
                                .toArray();
        
        try {
            return clazz.cast(injectConstructor.newInstance(parameters));
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            return null;
        }
    }

    @Nullable
    public <T> T resolve(Class<T> clazz) {
        T result = get(clazz);
        if(result != null) return result;
        return create(clazz);
    }
}
