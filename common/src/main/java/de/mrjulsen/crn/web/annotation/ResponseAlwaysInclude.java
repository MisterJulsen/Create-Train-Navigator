package de.mrjulsen.crn.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field of a JSON response model that must never be dropped by a {@code ?fields=...}
 * selection, as long as the object it belongs to is itself selected. Put it on the identifier of a
 * response type so a partial response always stays referenceable, e.g. a train's id or a reference's
 * key.
 * <p>
 * The rule is per level: a top-level {@code @AlwaysInclude} field is present in every response,
 * whereas one on a nested type only appears when that nested object is part of the selection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD})
public @interface ResponseAlwaysInclude {}
