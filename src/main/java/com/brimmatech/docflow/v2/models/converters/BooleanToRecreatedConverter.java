package com.brimmatech.docflow.v2.models.converters;

import jakarta.persistence.AttributeConverter;

public class BooleanToRecreatedConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        return (attribute != null && attribute) ? "YES" : "NO";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return dbData.equals("YES");
    }

}
