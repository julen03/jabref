package org.jabref.logic.bibtex;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.os.OS;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.strings.StringUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldWriterTest {

    private FieldWriter writer;

    static Stream<Arguments> keepHashSignInComment() {
        return Stream.of(Arguments.of("""
                        # Changelog

                        All notable changes to this project will be documented in this file.
                        The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
                        We refer to [GitHub issues](https://github.com/JabRef/jabref/issues) by using `#NUM`.
                        In case, there is no issue present, the pull request implementing the feature is linked.

                        Note that this project **does not** adhere to [Semantic Versioning](http://semver.org/).

                        ## [Unreleased]"""),
                // Source: https://github.com/JabRef/jabref/issues/7010#issue-720030293
                Arguments.of(
                        """
                                #### Goal
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit,
                                #### Achievement\s
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit,
                                #### Method
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit,"""
                ),
                // source: https://github.com/JabRef/jabref/issues/8303 --> bug2.txt
                Arguments.of("Particularly, we equip SOVA &#x2013; a Semantic and Ontological Variability Analysis method")
                );
    }

    @BeforeEach
    void setUp() {
        FieldPreferences fieldPreferences = new FieldPreferences(true, List.of(StandardField.MONTH), List.of());
        writer = new FieldWriter(fieldPreferences);
    }

    @ParameterizedTest
    @MethodSource
    void keepHashSignInComment(String text) throws InvalidFieldValueException {
        String writeResult = writer.write(StandardField.COMMENT, text);
        String resultWithLfAsNewLineSeparator = StringUtil.unifyLineBreaks(writeResult, "\n");
        assertEquals("{" + text + "}", resultWithLfAsNewLineSeparator);
    }

    @Test
    void noNormalizationOfNewlinesInAbstractField() throws InvalidFieldValueException {
        String text = "lorem" + OS.NEWLINE + " ipsum lorem ipsum\nlorem ipsum \rlorem ipsum\r\ntest";
        String result = writer.write(StandardField.ABSTRACT, text);
        // The normalization is done at org.jabref.logic.exporter.BibWriter, so no need to normalize here
        String expected = "{" + text + "}";
        assertEquals(expected, result);
    }

    @Test
    void preserveNewlineInAbstractField() throws InvalidFieldValueException {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = writer.write(StandardField.ABSTRACT, text);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    void preserveMultipleNewlinesInAbstractField() throws InvalidFieldValueException {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = writer.write(StandardField.ABSTRACT, text);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    void preserveNewlineInReviewField() throws InvalidFieldValueException {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = writer.write(StandardField.REVIEW, text);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    void whitespaceFromNonMultiLineFieldsKept() throws InvalidFieldValueException {
        // This was a decision on 2024-06-15 when fixing https://github.com/JabRef/jabref/issues/4877
        // We want to have a clean architecture for reading and writing
        // Normalizing is done during write (and not during read)
        // Furthermore, normalizing is done in the BibDatabaseWriter#applySaveActions and not in the fielld writer

        String original = "I\nshould\nnot\ninclude\nadditional\nwhitespaces  \nor\n\ttabs.";
        String expected = "{" + original + "}";

        String title = writer.write(StandardField.TITLE, original);
        String any = writer.write(new UnknownField("anyotherfield"), original);

        assertEquals(expected, title);
        assertEquals(expected, any);
    }

    @Test
    void reportUnbalancedBracing() {
        String unbalanced = "{";

        assertThrows(InvalidFieldValueException.class, () -> writer.write(new UnknownField("anyfield"), unbalanced));
    }

    @Test
    void reportUnbalancedBracingWithEscapedBraces() {
        String unbalanced = "{\\}";

        assertThrows(InvalidFieldValueException.class, () -> writer.write(new UnknownField("anyfield"), unbalanced));
    }

    @Test
    void tolerateBalancedBrace() throws InvalidFieldValueException {
        String text = "Incorporating evolutionary {Measures into Conservation Prioritization}";

        assertEquals("{" + text + "}", writer.write(new UnknownField("anyfield"), text));
    }

    @Test
    void tolerateEscapeCharacters() throws InvalidFieldValueException {
        String text = "Incorporating {\\O}evolutionary {Measures into Conservation Prioritization}";

        assertEquals("{" + text + "}", writer.write(new UnknownField("anyfield"), text));
    }

    @Test
    void hashEnclosedWordsGetRealStringsInMonthField() throws InvalidFieldValueException {
        String text = "#jan# - #feb#";
        assertEquals("jan # { - } # feb", writer.write(StandardField.MONTH, text));
    }

    @Test
    void hashWorksSimple() throws InvalidFieldValueException {
        String text = "#text";
        assertEquals("{#text}", writer.write(StandardField.MONTH, text));
    }

    @Test
    void escapedHashWorksSimple() throws InvalidFieldValueException {
        String text = "\\#text";
        assertEquals("{\\#text}", writer.write(StandardField.MONTH, text));
    }

    @Test
    void doubleHashesRemoved() throws InvalidFieldValueException {
        String text = "te##xt";
        assertEquals("{text}", writer.write(StandardField.MONTH, text));
    }

    @Test
    void multipleSpacesNotShrunkOnSingleLineField() throws InvalidFieldValueException {
        String text = "t  w  o";
        assertEquals("{t  w  o}", writer.write(StandardField.MONTH, text));
    }

    @Test
    void doubleSpacesAreKept() throws InvalidFieldValueException {
        String text = "  text      ";
        assertEquals("{  text      }", writer.write(StandardField.MONTH, text));
    }

    @Test
    void spacesAreNotTrimmedAtMultilineField() throws InvalidFieldValueException {
        String text = "  text      ";
        assertEquals("{  text      }", writer.write(StandardField.COMMENT, text));
        // Note: Spaces are trimmed at BibDatabaseWriter#applySaveActions
    }

    @Test
    void multipleSpacesKeptOnMultiLineField() throws InvalidFieldValueException {
        String text = "t  w  o";
        assertEquals("{t  w  o}", writer.write(StandardField.COMMENT, text));
    }

    @Test
    void finalNewLineIsKeptAtMultilineField() throws InvalidFieldValueException {
        String text = "  text      " + OS.NEWLINE;
        assertEquals("{" + text + "}", writer.write(StandardField.COMMENT, text));
        // Note: Spaces are trimmed at BibDatabaseWriter#applySaveActions
    }
}
