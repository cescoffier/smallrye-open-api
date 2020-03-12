package io.smallrye.openapi.runtime.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import io.smallrye.openapi.api.constants.OpenApiConstants;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.PathsImpl;
import io.smallrye.openapi.api.models.media.ContentImpl;
import io.smallrye.openapi.api.models.media.MediaTypeImpl;
import io.smallrye.openapi.api.models.responses.APIResponsesImpl;
import io.smallrye.openapi.api.util.MergeUtil;

/**
 * Class with some convenience methods useful for working with the OAI data model.
 * 
 * @author eric.wittmann@gmail.com
 */
public class ModelUtil {

    /**
     * Constructor.
     */
    private ModelUtil() {
    }

    /**
     * Adds a {@link Tag} to the {@link OpenAPI} model. If a tag having the same
     * name already exists in the model, the tags' attributes are merged, with the
     * new tag's attributes overriding the value of any attributes specified on
     * both.
     * 
     * @param openApi the OpenAPI model
     * @param tag a new {@link Tag} to add
     */
    public static void addTag(OpenAPI openApi, Tag tag) {
        List<Tag> tags = openApi.getTags();

        if (tags == null || tags.isEmpty()) {
            openApi.addTag(tag);
            return;
        }

        Tag current = tags.stream().filter(t -> t.getName().equals(tag.getName())).findFirst().orElse(null);
        int currentIndex = tags.indexOf(current);

        if (current != null) {
            Tag replacement = MergeUtil.mergeObjects(current, tag);
            tags = new ArrayList<>(tags);
            tags.set(currentIndex, replacement);
            openApi.setTags(tags);
        } else {
            openApi.addTag(tag);
        }
    }

    /**
     * Gets the {@link Components} from the OAI model. If it doesn't exist, creates it.
     * 
     * @param openApi OpenAPI
     * @return Components
     */
    public static Components components(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new ComponentsImpl());
        }
        return openApi.getComponents();
    }

    /**
     * Gets the {@link Paths} from the OAI model. If it doesn't exist, creates it.
     * 
     * @param openApi OpenAPI
     * @return Paths
     */
    public static Paths paths(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            openApi.setPaths(new PathsImpl());
        }
        return openApi.getPaths();
    }

    /**
     * Gets the {@link APIResponses} child model from the given operation. If it's null
     * then it will be created and returned.
     * 
     * @param operation Operation
     * @return APIResponses
     */
    public static APIResponses responses(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new APIResponsesImpl());
        }
        return operation.getResponses();
    }

    /**
     * Returns true only if the given {@link Parameter} has a schema defined
     * for it. A schema can be defined either via the parameter's "schema"
     * property, or any "content.*.schema" property.
     * 
     * @param parameter Parameter
     * @return Whether the parameter has a schema
     */
    public static boolean parameterHasSchema(Parameter parameter) {
        if (parameter.getSchema() != null) {
            return true;
        }
        Map<String, MediaType> mediaTypes = getMediaTypesOrEmpty(parameter.getContent());
        if (!mediaTypes.isEmpty()) {
            for (MediaType mediaType : mediaTypes.values()) {
                if (mediaType.getSchema() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the list of {@link Schema}s defined for the given {@link Parameter}.
     * A schema can be defined either via the parameter's "schema" property, or any
     * "content.*.schema" property.
     *
     * @param parameter Parameter
     * @return list of schemas, never null
     */
    public static List<Schema> getParameterSchemas(Parameter parameter) {
        if (parameter.getSchema() != null) {
            return Arrays.asList(parameter.getSchema());
        }
        Map<String, MediaType> mediaTypes = getMediaTypesOrEmpty(parameter.getContent());
        if (!mediaTypes.isEmpty()) {
            List<Schema> schemas = new ArrayList<>(mediaTypes.size());

            for (MediaType mediaType : mediaTypes.values()) {
                if (mediaType.getSchema() != null) {
                    schemas.add(mediaType.getSchema());
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Sets the given {@link Schema} on the given {@link Parameter}. This is tricky
     * because the paramater may EITHER have a schema property or it may have a
     * {@link Content} child which itself has zero or more {@link MediaType} children
     * which will contain the {@link Schema}.
     *
     * The OpenAPI specification requires that a parameter have *either* a schema
     * or a content, but not both.
     * 
     * @param parameter Parameter
     * @param schema Schema
     */
    public static void setParameterSchema(Parameter parameter, Schema schema) {
        if (schema == null) {
            return;
        }
        if (parameter.getContent() == null) {
            parameter.schema(schema);
            return;
        }
        Content content = parameter.getContent();
        Map<String, MediaType> mediaTypes = getMediaTypesOrEmpty(content);
        if (mediaTypes.isEmpty()) {
            String[] defMediaTypes = OpenApiConstants.DEFAULT_MEDIA_TYPES.get();
            for (String mediaTypeName : defMediaTypes) {
                MediaType mediaType = new MediaTypeImpl();
                mediaType.setSchema(schema);
                content.addMediaType(mediaTypeName, mediaType);
            }
            return;
        }
        for (String mediaTypeName : mediaTypes.keySet()) {
            MediaType mediaType = content.getMediaType(mediaTypeName);
            mediaType.setSchema(schema);
        }
    }

    /**
     * Returns true only if the given {@link RequestBody} has a schema defined
     * for it. A schema would be found within the request body's Content/MediaType
     * children.
     * 
     * @param requestBody RequestBody
     * @return Whether RequestBody has a schema
     */
    public static boolean requestBodyHasSchema(RequestBody requestBody) {
        Map<String, MediaType> mediaTypes = getMediaTypesOrEmpty(requestBody.getContent());
        if (!mediaTypes.isEmpty()) {
            for (MediaType mediaType : mediaTypes.values()) {
                if (mediaType.getSchema() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets the given {@link Schema} on the given {@link RequestBody}.
     * 
     * @param requestBody RequestBody
     * @param schema Schema
     * @param mediaTypes String array
     */
    public static void setRequestBodySchema(RequestBody requestBody, Schema schema, String[] mediaTypes) {
        Content content = requestBody.getContent();
        if (content == null) {
            content = new ContentImpl();
            requestBody.setContent(content);
        }
        Map<String, MediaType> contentMediaTypes = getMediaTypesOrEmpty(content);
        if (contentMediaTypes.isEmpty()) {
            String[] requestBodyTypes;
            if (mediaTypes != null && mediaTypes.length > 0) {
                requestBodyTypes = mediaTypes;
            } else {
                requestBodyTypes = OpenApiConstants.DEFAULT_MEDIA_TYPES.get();
            }
            for (String mediaTypeName : requestBodyTypes) {
                MediaType mediaType = new MediaTypeImpl();
                mediaType.setSchema(schema);
                content.addMediaType(mediaTypeName, mediaType);
            }
            return;
        }
        for (String mediaTypeName : contentMediaTypes.keySet()) {
            MediaType mediaType = content.getMediaType(mediaTypeName);
            mediaType.setSchema(schema);
        }
    }

    static Map<String, MediaType> getMediaTypesOrEmpty(Content content) {
        if (content != null && content.getMediaTypes() != null) {
            return content.getMediaTypes();
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the name component of the ref.
     * 
     * @param ref String
     * @return Name
     */
    public static String nameFromRef(String ref) {
        String[] split = ref.split("/");
        return split[split.length - 1];
    }

    public static <V> Map<String, V> unmodifiableMap(Map<String, V> map) {
        return map != null ? Collections.unmodifiableMap(map) : null;
    }

    public static <V> Map<String, V> replace(Map<String, V> modified, UnaryOperator<Map<String, V>> factory) {

        final Map<String, V> replacement;

        if (modified == null) {
            replacement = null;
        } else {
            replacement = factory.apply(modified);
        }

        return replacement;
    }

    public static <V> Map<String, V> add(String key, V value, Map<String, V> map, Supplier<Map<String, V>> factory) {
        if (value != null) {
            if (map == null) {
                map = factory.get();
            }
            map.put(key, value);
        }
        return map;
    }

    public static <V> void remove(Map<String, V> map, String key) {
        if (map != null) {
            map.remove(key);
        }
    }

    public static <V> List<V> unmodifiableList(List<V> list) {
        return list != null ? Collections.unmodifiableList(list) : null;
    }

    public static <V> List<V> replace(List<V> modified, UnaryOperator<List<V>> factory) {
        final List<V> replacement;

        if (modified == null) {
            replacement = null;
        } else {
            replacement = factory.apply(modified);
        }

        return replacement;
    }

    public static <V> List<V> add(V value, List<V> list, Supplier<List<V>> factory) {
        if (value != null) {
            if (list == null) {
                list = factory.get();
            }
            list.add(value);
        }
        return list;
    }

    public static <V> void remove(List<V> list, V value) {
        if (list != null) {
            list.remove(value);
        }
    }
}
