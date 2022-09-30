package com.skydo.lib.fsm.internal.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface StateMachine {

    String initialState();

    Transition[] config() default {};

    Class<? extends StateTransition> classConfig() default StateTransitionVoid.class;
}
