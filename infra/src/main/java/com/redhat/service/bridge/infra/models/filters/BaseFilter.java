package com.redhat.service.bridge.infra.models.filters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = BaseFilter.FILTER_TYPE_FIELD)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StringBeginsWith.class, name = StringBeginsWith.FILTER_TYPE_NAME),
        @JsonSubTypes.Type(value = StringContains.class, name = StringContains.FILTER_TYPE_NAME),
        @JsonSubTypes.Type(value = StringEquals.class, name = StringEquals.FILTER_TYPE_NAME),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseFilter {
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String FILTER_TYPE_FIELD = "type";

    @JsonProperty(FILTER_TYPE_FIELD)
    protected String type;

    @JsonProperty("key")
    protected String key;

    public BaseFilter() {
    }

    public BaseFilter(String key, String value) {
        this.key = key;
        setValueFromString(value);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @JsonIgnore
    public abstract String getValueAsString();

    @JsonIgnore
    public abstract Object getValue();

    @JsonIgnore
    protected abstract void setValueFromString(String value);
}
