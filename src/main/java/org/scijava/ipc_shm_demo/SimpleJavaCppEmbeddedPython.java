package org.scijava.ipc_shm_demo;

import org.bytedeco.javacpp.*;
import static org.bytedeco.cpython.global.python.*;

/** Copied from the javacpp-embedded-python docs. */
public class SimpleJavaCppEmbeddedPython {
    public static void main(String[] args) throws Exception {
        Pointer program = Py_DecodeLocale(SimpleJavaCppEmbeddedPython.class.getSimpleName(), null);
        if (program == null) {
            System.err.println("Fatal error: cannot decode class name");
            System.exit(1);
        }
        Py_SetProgramName(program);  /* optional but recommended */
        Py_Initialize(cachePackages());
        PyRun_SimpleString("from time import time,ctime\n"
                         + "print('Today is', ctime(time()))\n");
        if (Py_FinalizeEx() < 0) {
            System.exit(120);
        }
        PyMem_RawFree(program);
        //org.bytedeco.cpython.global.python.PyObject_AsReadBuffer(...)
        System.exit(0);
    }
}
