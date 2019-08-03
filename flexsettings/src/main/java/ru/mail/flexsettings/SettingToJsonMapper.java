package ru.mail.flexsettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import ru.mail.flexsettings.field.Field;
import ru.mail.flexsettings.field.FreeObjectField;
import ru.mail.flexsettings.field.StrictObjectField;

public class SettingToJsonMapper {

    public String map(StrictObjectField baseField) throws JSONException {
        return mapStrictObject(baseField).toString();
    }

    private Object mapField(Field field) throws JSONException {
        // TODO fix for required fields
//        if (!field.isChanged()) {
//            return null;
//        }

        if (field.isString()) {
            return field.asString().getValue();
        } else if (field.isBoolean()) {
            return field.asBoolean().getValue();
        } else if (field.isInteger()) {
            return field.asInteger().getValue();
        } else if (field.isLong()) {
            return field.asLong().getValue();
        } else if (field.isStrictObject()) {
            return mapStrictObject(field.asStrictObject());
        } else if (field.isFreeObject()) {
            return mapFreeObject(field.asFreeObject());
        } else {
            return null;
        }
    }

    private JSONObject mapFieldList(JSONObject baseJson, List<Field> fieldList) throws JSONException {
        for (Field field : fieldList) {
            baseJson.put(field.getName(), mapField(field));
        }
        return baseJson;
    }

    private JSONObject mapStrictObject(StrictObjectField baseField) throws JSONException {
        return mapFieldList(new JSONObject(), baseField.getAllFields());
    }

    private JSONObject mapFreeObject(FreeObjectField baseField) throws JSONException {
        return mapFieldList(new JSONObject(), baseField.getAllFields());
    }
}
