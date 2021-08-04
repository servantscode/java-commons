package org.servantscode.commons;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface HtmlFilter {
    HtmlType type();
}
