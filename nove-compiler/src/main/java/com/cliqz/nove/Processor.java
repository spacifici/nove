package com.cliqz.nove;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

@SupportedAnnotationTypes("com.cliqz.nove.Subscribe")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class Processor extends AbstractProcessor{


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }
        final Set<? extends Element> annotatedElements =
                roundEnv.getElementsAnnotatedWith(Subscribe.class);
        if (annotatedElements.isEmpty()) {
            return true;
        }

        final HashMap<TypeElement, DispatcherWriter.Builder> builders = new HashMap<>();
//        final DispatcherWriter.Builder builder = new DispatcherWriter.Builder();
//        final Element anyElement = annotatedElements.iterator().next();
//        final TypeElement clazz = (TypeElement) anyElement.getEnclosingElement();
//        builder.setTypeElement(clazz);
        for (Element e: annotatedElements) {
            final TypeElement clazz = (TypeElement) e.getEnclosingElement();
            DispatcherWriter.Builder builder = builders.get(clazz);
            if (builder == null) {
                builder = DispatcherWriter.builder().setTypeElement(clazz);
                builders.put(clazz, builder);
            }
            if (ElementKind.METHOD.equals(e.getKind())) {
                final ExecutableElement ee = (ExecutableElement) e;
                checkNotPrivate(ee);
                checkReturnVoid(ee);
                checkParameters(ee);
                builder.addSubscriberMethod(ee);
            }
        }

        for (DispatcherWriter.Builder builder: builders.values()) {
            final DispatcherWriter writer = builder.build();
            writer.write(processingEnv.getFiler());
        }
        return true;
    }

    private void checkNotPrivate(ExecutableElement e) {
        for (Modifier modifier: e.getModifiers()) {
            if (modifier.equals(Modifier.PRIVATE) ||
                    modifier.equals(Modifier.PROTECTED) ||
                    modifier.equals(Modifier.ABSTRACT)) {
                processingEnv.getMessager()
                        .printMessage(Kind.ERROR, "Invalid modifier: " + modifier.name(), e);
            }
        }
    }

    private void checkParameters(ExecutableElement e) {
        final List<? extends VariableElement> params = e.getParameters();
        final int size = params.size();
        if (size != 1) {
            processingEnv.getMessager()
                    .printMessage(Kind.ERROR, "Subscriber must have a single parameter", e);
        }

        final VariableElement param = params.get(0);
        if (param.asType().getKind().isPrimitive()) {
            processingEnv.getMessager()
                    .printMessage(Kind.ERROR, "Subscriber can't use primitives as parameters", e);
        }
    }

    private void checkReturnVoid(ExecutableElement e) {
        final TypeMirror returnType = e.getReturnType();
        if (!returnType.getKind().equals(TypeKind.VOID)) {
            processingEnv.getMessager()
                    .printMessage(Kind.ERROR, "Subscriber must return void", e);
        }
    }
}
