package com.github.auties00.daedalus.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A property type that supports converter management for serialization and deserialization.
 *
 * <p>Property types hold a list of converters that form a chain from the source type
 * to the wire format type (for serialization) or vice versa (for deserialization).
 * The common processor framework uses this interface to resolve unattributed converters
 * into attributed ones via the converter graph.
 */
public interface DaedalusFieldType {

    /**
     * Returns the converters associated with this property type.
     *
     * @return an unmodifiable list of converters
     */
    List<DaedalusConverterElement> converters();

    /**
     * Adds a converter to this property type.
     *
     * @param element the converter to add
     */
    void addConverter(DaedalusConverterElement element);

    /**
     * Removes all converters from this property type.
     */
    void clearConverters();

    /**
     * Returns the type of the element that describes this property.
     *
     * <p>This is the input type used by the deserializer and builder, and matches
     * the parameter type in the constructor of the enclosing type.
     *
     * @return the descriptor element type
     */
    TypeMirror descriptorElementType();

    /**
     * Returns the default value expression for this property type.
     *
     * <p>For primitive types this is {@code 0} (or {@code false}). For objects it may
     * be {@code null}, or the value provided by a {@code @TypeDefaultValue} annotation
     * in a mixin.
     *
     * @return the default value expression as a string
     */
    String descriptorDefaultValue();

    /**
     * Returns the mixin types associated with this property type.
     *
     * <p>Mixins provide additional serialization, deserialization, and default value
     * logic. They are used by the builder mixin resolver to discover
     * {@code @TypeBuilder.Mixin} methods applicable to this field.
     *
     * @return an unmodifiable set of mixin type elements
     */
    Set<TypeElement> mixins();

    /**
     * Returns the nested field types whose converters should also be attributed
     * when this field is processed.
     *
     * <p>Composite types like collections and maps override this to expose their
     * element types so that the framework recurses into them during attribution.
     * Simple field types return an empty collection.
     *
     * @return the nested field types contained by this property type
     */
    Collection<? extends DaedalusFieldType> nestedTypes();
}
