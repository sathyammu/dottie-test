package com.brimmatech.saas;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings({"PMD.ShortVariable", "PMD.ShortClassName"})
public class Term implements Serializable {
    public static final String AND = "and";
    public static final String OR = "or";
    private String operator;
    private Set<Term> terms;
    private String canonicalName;
    private String value;
    private String matchType;
    private String precision;
    private String include;

    public Term(String operator, Set<Term> terms) {
        this.operator = operator;
        this.terms = terms;
    }

    public Term(String canonicalName, String value, String matchType, String precision) {
        this.canonicalName = canonicalName;
        this.value = value;
        this.matchType = matchType;
        this.precision = precision;
    }

    public Term(String canonicalName, String value, String matchType) {
        this.canonicalName = canonicalName;
        this.value = value;
        this.matchType = matchType;
    }

    public Term(String canonicalName, String matchType) {
        this.canonicalName = canonicalName;
        this.matchType = matchType;
    }

    public Term(String canonicalName, String value, String matchType, String include, String precision) {
        this.canonicalName = canonicalName;
        this.value = value;
        this.matchType = matchType;
        this.include = include;
        this.precision = precision;
    }
}
