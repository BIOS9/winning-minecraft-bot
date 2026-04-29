package ciaassured.yrushwinner.navigation.validators;

import ciaassured.yrushwinner.navigation.actions.special.PathAction;

public interface PathValidator {
    boolean isValid(PathAction action);
}
