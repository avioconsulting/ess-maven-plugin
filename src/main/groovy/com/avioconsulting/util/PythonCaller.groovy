package com.avioconsulting.util

import org.python.core.*
import weblogic.management.scripting.utils.WLSTInterpreter

class PythonCaller {
    private final WLSTInterpreter interpreter

    PythonCaller() {
        def props = [
                (WLSTInterpreter.ENABLE_SCRIPT_MODE): Boolean.TRUE
        ]
        this.interpreter = new WLSTInterpreter(props)
    }

    def methodCall(String name) {
        return methodCall(name, [], [:])
    }

    def methodCall(String name, List regularArgs) {
        return methodCall(name, regularArgs, [:])
    }

    def methodCall(String name, Map keywordArgs) {
        return methodCall(name, [], keywordArgs)
    }

    def cmoGet(String name) {
        PyJavaInstance cmo = this.interpreter.get 'cmo'
        assert cmo != null: "WLST cmo does not exist!"
        String methodName = "get${name.capitalize()}"
        // for some reason, does not work w/o intern
        cmo.invoke(methodName.intern())
    }

    def methodCall(String name, List regularArgs, Map keywordArgs) {
        PyFunction function = this.interpreter.get name
        assert function != null: "WLST function ${name} does not exist!"
        List<PyObject> pyObjectArgs = regularArgs.collect { arg ->
            convertValue(arg)
        }
        List<String> kwArgs = []
        keywordArgs.each { key, value ->
            kwArgs << key
            pyObjectArgs << convertValue(value)
        }
        return function.__call__((PyObject[]) pyObjectArgs.toArray(), (String[]) kwArgs.toArray())
    }

    String withInterceptedStdout(Closure closure) {
        // command outputs to stdout and doesn't return anything
        def writer = new StringWriter()
        def interpreter = this.interpreter
        def origOut = interpreter.out
        interpreter.out = writer
        try {
            closure()
        }
        finally {
            interpreter.out = origOut
        }
        writer.close()
        return writer.toString()
    }

    private PyObject convertValue(value) {
        if (value instanceof Map) {
            return getDictionary(value)
        }
        return Py.java2py(value)
    }

    private PyDictionary getDictionary(Map map) {
        def dict = new PyDictionary()
        map.each { key, value ->
            dict.__setitem__(convertValue(key), convertValue(value))
        }
        dict
    }
}
