package com.cliqz.nove;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

// Visible for testing, utility inner class that encapsulate loading of the specific, generated
// dispatcher via reflection. It forward the calls to the compile time generated post methods
// by caching the reference
class Dispatcher<T> {

    private static final ClassLoader loader = Dispatcher.class.getClassLoader();

    private final Object dispatcher;
    private final Method post;
    private final Class[] messageTypes;

    Dispatcher(T object, Class<T> clazz) {
        final String dispatcherClassName = clazz.getCanonicalName() + Bus.DISPATCHER_POSTFIX;
        try {
            final Class<?> dispatcherClass = loader.loadClass(dispatcherClassName);
            final Constructor<?> constructor =
                    dispatcherClass.getConstructor(clazz);
            dispatcher = constructor.newInstance(object);

            post = dispatcherClass
                    .getDeclaredMethod(Bus.POST_METHOD_NAME, Object.class);
            messageTypes = (Class[]) dispatcherClass
                    .getDeclaredField(Bus.MESSAGE_TYPES_FIELD_NAME).get(null);
        } catch (ClassNotFoundException cnfe) {
            // This is only useful to properly address problems due to class hierarchies
            final Class sup = clazz.getSuperclass();
            if (sup != null && !sup.isInterface() && !sup.isPrimitive()) {
                // Check if a concrete or abstract parent class has a Dispatcher
                final String disName = sup.getCanonicalName() + Bus.DISPATCHER_POSTFIX;
                try {
                    final Class dispatcherClass = loader.loadClass(disName);
                    throw new SubclassRegistrationException(sup);
                } catch (ClassNotFoundException innerCnfe) {
                    // NOP
                }
            }
            // The class should have at least one Subscribe annotated method
            throw new DispatcherNotFoundException(dispatcherClassName);
        } catch (Exception e) {
            // Re-throw any other exception as a RuntimeException
            throw new RuntimeException(e);
        }
    }

    void post(Object message) {
        try {
            post.invoke(dispatcher, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Class[] getMessageTypes() {
        return messageTypes;
    }
}