package com.jyb;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

@AutoService(Processor.class)
public class SingletonProcessor extends AbstractProcessor {

	@Override
	public Set<String> getSupportedAnnotationTypes() {

		Set<String> set = new HashSet<>();
		set.add(Singleton.class.getName());
		return set;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(Singleton.class);
		roundEnv.processingOver();

		elementsAnnotatedWith.forEach(ele -> {
			Name elementName = ele.getSimpleName();

			if (ele.getKind() != ElementKind.CLASS) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Singleton Annotation must be type " + elementName);
				return;
			}

			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processsing " + elementName);
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processsing " + ele);
			TypeElement typeElement = (TypeElement) ele;

			ClassName className = ClassName.get(typeElement);

			TypeSpec typeSpec = TypeSpec.classBuilder(elementName.toString())
					.addModifiers(Modifier.PUBLIC)
					.addMethod(getPrivateConstructor())
					.addType(getLazyHolder())
					.build();

			Filer filer = processingEnv.getFiler();

			try {
				JavaFile.builder(className.packageName(), typeSpec)
						.indent("2")
						.build()
						.writeTo(filer);
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR" + e);
				e.printStackTrace();
			}

		});
		return true;
	}

	private FieldSpec getInstanceFieldSpec(Element clazz) {
		try {
			return FieldSpec.builder(
					Class.forName(clazz.toString())
					, "INSTANCE"
					, Modifier.PUBLIC
					, Modifier.FINAL
					, Modifier.STATIC)
					.build();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private MethodSpec getPrivateConstructor() {
		return MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PRIVATE)
				.build();
	}

	private TypeSpec getLazyHolder() {
		return TypeSpec.classBuilder("LazyHolder")
				.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
//				.addField(getInstanceFieldSpec(clazz))
				.build();
	}
}
