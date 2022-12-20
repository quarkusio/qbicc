package org.qbicc.plugin.initializationcontrol;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.qbicc.context.CompilationContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class QbiccFeatureProcessor {
    public static void process(CompilationContext ctxt, List<URL> yamlFeatures, List<QbiccFeature> features) {
        if (yamlFeatures.isEmpty() && features.isEmpty()) {
            return;
        }
        FeaturePatcher fp = FeaturePatcher.get(ctxt);
        RuntimeResourceManager rm = RuntimeResourceManager.get(ctxt);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (URL feature : yamlFeatures) {
            try (InputStream fs = feature.openStream()) {
                QbiccFeature yf = mapper.readValue(fs, QbiccFeature.class);
                processFeature(yf, fp, rm);
            } catch (IOException e) {
                ctxt.error(e, "Failed to read qbicc-feature %s ", feature);
            }
        }

        for (QbiccFeature f : features) {
            processFeature(f, fp, rm);
        }
    }

    private static void processFeature(QbiccFeature qf, FeaturePatcher fp, RuntimeResourceManager rm) {
        if (qf.initializeAtRuntime != null) {
            for (String className : qf.initializeAtRuntime) {
                String internalName = className.replace('.', '/');
                fp.addRuntimeInitializedClass(internalName);
            }
        }
        if (qf.runtimeResource != null) {
            for (String name : qf.runtimeResource) {
                rm.addResource(name);
            }
        }
        if (qf.runtimeResources != null) {
            for (String name : qf.runtimeResources) {
                rm.addResources(name);
            }
        }
        if (qf.reflectiveClasses != null) {
            for (QbiccFeature.ReflectiveClass rc : qf.reflectiveClasses) {
                fp.addReflectiveClass(rc.name.replace('.', '/'), rc.fields, rc.methods, rc.constructors);
            }
        }
        if (qf.reflectiveConstructors != null) {
            for (QbiccFeature.Constructor c : qf.reflectiveConstructors) {
                fp.addReflectiveConstructor(c.declaringClass.replace('.', '/'), c.arguments);
            }
        }
        if (qf.reflectiveFields != null) {
            for (QbiccFeature.Field f : qf.reflectiveFields) {
                fp.addReflectiveField(f.declaringClass.replace('.', '/'), f.name);
            }
        }
        if (qf.reflectiveMethods != null) {
            for (QbiccFeature.Method meth : qf.reflectiveMethods) {
                fp.addReflectiveMethod(meth.declaringClass.replace('.', '/'), meth.name, meth.arguments);
            }
        }
    }
}
