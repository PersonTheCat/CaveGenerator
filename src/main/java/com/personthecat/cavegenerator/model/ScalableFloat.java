package com.personthecat.cavegenerator.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Contains settings for how a particular floating point value is intended to
 * change throughout its lifetime.
 */
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class ScalableFloat {
    float startVal;
    float startValRandFactor;
    float factor;
    float randFactor;
    float exponent;
}
