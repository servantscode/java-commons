package org.servantscode.commons.search;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FieldTransformer {
    public class Transformation<T extends Object> {
        private String inputFieldName;
        private String outputFieldName;
        private Function<String, T> valueTransform = null;

        public Transformation(String inputFieldName, String outputFieldName) {
            this.inputFieldName = inputFieldName;
            this.outputFieldName = outputFieldName;
        }

        public Transformation(String inputFieldName, String outputFieldName, Function<String, T> valueTransform) {
            this.inputFieldName = inputFieldName;
            this.outputFieldName = outputFieldName;
            this.valueTransform = valueTransform;
        }

        public String fieldName() {
            return outputFieldName;
        }

        public Object transform(String value) {
            return valueTransform != null? valueTransform.apply(value): value;
        }

        public String toString() {
            return String.format("Trans(%s => %s)", inputFieldName, outputFieldName);
        }
    }

    private final HashMap<String, Transformation> fieldTransforms;

    public FieldTransformer() {
        fieldTransforms = new HashMap<>(8);
    }

    public FieldTransformer(Map<String, String> fieldMap) {
        if(fieldMap == null) {
            fieldTransforms = new HashMap<>(8);
            return;
        }

        fieldTransforms = new HashMap<>(fieldMap.size());
        for(Map.Entry<String, String> entry : fieldMap.entrySet())
            fieldTransforms.put(entry.getKey(), new Transformation(entry.getKey(), entry.getValue()));
    }

    public String transformFieldName(String fieldName) {
        Transformation transformation = fieldTransforms.get(fieldName);
        return transformation != null? transformation.fieldName(): fieldName;
    }

    public void put(String inputField, String outputField) {
        fieldTransforms.put(inputField, new Transformation(inputField, outputField));
    }

    public <T extends Object> void  put(String inputField, String outputField, Function<String, T> transformFunction) {
        fieldTransforms.put(inputField, new Transformation<T>(inputField, outputField, transformFunction));
    }

    public Transformation get(String fieldName) {
        Transformation transformation = fieldTransforms.get(fieldName);
        return transformation != null? transformation: new Transformation<String>(fieldName, fieldName);
    }
}
