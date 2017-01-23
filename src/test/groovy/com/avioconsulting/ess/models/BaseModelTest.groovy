package com.avioconsulting.ess.models

import groovy.test.GroovyAssert
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class BaseModelTest {
    @Test
    void validateName_letters() {
        // arrange

        // act
        def result = BaseModel.validateName 'ABC'

        // assert
        assertThat result,
                   is(equalTo(true))
    }

    @Test
    void validateName_underscore() {
        // arrange

        // act
        def result = BaseModel.validateName 'ABC_DEF'

        // assert
        assertThat result,
                   is(equalTo(true))
    }

    @Test
    void validateName_numbers_at_end() {
        // arrange

        // act
        def result = BaseModel.validateName 'ABC_123'

        // assert
        assertThat result,
                   is(equalTo(true))
    }

    @Test
    void validateName_number_at_start() {
        // arrange

        // act + assert
        def error = GroovyAssert.shouldFail {
            BaseModel.validateName '123_ABC'
        }

        assertThat error.message,
                   is(equalTo( "Name value '123_ABC' not allowed. ESS does not allow leading numbers, special characters!"))
    }

    @Test
    void validateName_specialCharacters() {
        // arrange

        // act + assert
        def error = GroovyAssert.shouldFail {
            BaseModel.validateName 'ABC_%'
        }

        assertThat error.message,
                   is(equalTo("Name value 'ABC_%' not allowed. ESS does not allow special characters!"))
    }

    @Test
    void validateName_spaces() {
        // arrange

        // act + assert
        def error = GroovyAssert.shouldFail {
            BaseModel.validateName 'ABC DEF'
        }

        assertThat error.message,
                   is(equalTo( "Name value 'ABC DEF' not allowed. ESS does not allow spaces, special characters!"))
    }
}
