# Interprocess shared memory proof of concept

## Demo

This repository contains a working Python-first demo of interprocess shared
memory with no backing file on disk. Here are the steps to try it:

1. Create and activate a conda environment with pyimagej installed.

2. Run this command:
   ```shell
   python -i src/main/python/open-with-pyimagej-into-shm.py /path/to/data/image.ome.tif
   ```
   Where `/path/to/data/image.ome.tif` is the path an image file, which will
   be opened using [SCIFIO](https://scif.io/) into an ImageJ2 `Dataset`,
   then copied into a NumPy ndarray backed by a new shared memory buffer.

   The `-i` is important to keep the process alive after the work finished,
   and so that you can have fun playing around across multiple processes.

3. In a separate console, run the command emitted by the first program. E.g.:
   ```shell
   python -i src/main/python/receive-numpy-array-from-shm.py psm_4b597b2c '(85, 33, 512, 768)' uint8
   ```
   Where `psm_4b597b2c` is the name of the shared memory buffer,
   `'(85, 33, 512, 768)'` is the shape of the image stored in that buffer,
   and `uint8` is the image's dtype.

   This second program will access the given shared memory, wrapping it up
   the same way to provide an equivalent image backed by the same memory.
   Changes to `arr_shared` in the first REPL will be reflected in `rai`
   in the second REPL, and vice versa. Note that the respective indexing
   orders of `arr_shared` (Python/NumPy) and `rai` (Java/ImgLib2) are
   *backwards* from one another.

## Planned components

1. Structured data schema for exchange of typed input/output parameters
   * this is the "data model of Ops" -- STANDS ALONE, LANGUAGE AGNOSTIC
   * if written to XML, can be validated by an XSD
   * if written to JSON, can be validated by a JSON-based schema validation mechanism
   * if written to YAML, can be validated by a YAML-based schema validation mechanism
   * analogous to takari.io's dialects
   * we start with JSON, because it's easy to process in Python & JS & Java & Julia

2. Named shared memory between processes -- LANGUAGE AND PROCESS AGNOSTIC
   * Perfect for zero-copy sharing of ndarrays that fit in RAM
   * Works across platforms
     * POSIX has shm mechanism
     * Windows has (Create|Open)FileHandle with `INVALID_FILE_POINTER` and non-null name
   * Works across languages
     * Python has `multiprocessing.shared_memory.SharedMemory`
     * Java has LArray (needs a patch from Mark H)
   * Works across ENVIRONMENTS
     * Pythons in different conda environments still share ndarrays
     * JVMs with different (class|module)paths still share ndarrays
     * etc.
   * We already have a working demo between Java ImgLib2 and Python numpy;
     see [Demo](#demo) above.

3. For Java-based apps: a process manager to exec processes from Java for Op execution
   * Simple mode: each Op execution execs a process
     * Feeds inputs in (1)'s JSON format to process's stdin
     * Receives outputs in (1)'s JSON format from process's stdout
     * Could also harvest status updates from process's stdout
     * Later, could send I/O in fancier formats like Apache Arrow -- but for now, KISS
   * Persistent mode: each environment has a persistent process,
     accepting commands via JSON lines to stdin, and producing output via JSON
     lines to stdout. In this way, we avoid the overhead of starting a new
     process for every command execution, which is important if executing
     commands thousands or millions of times in a loop from the other process.

4. For Java-based apps: a Java library offering a conda environment manager layer
   * Easily construct and update conda environments from Java code
   * Separate GUI layer on top so users can do it from Java applications
   * Coupled with (1) and (2), let's graphical Java applications execute
     plugins in other heterogeneous environments! E.g. execute deep learning
     models with divergent dependencies each in its own process from its own
     conda environment.

## Notes

[numpy.memmap] - Create a memory-map to an array stored in a binary file on disk.
"This subclass of ndarray has some unpleasant interactions with some operations"
"An alternative to using this subclass is to create the mmap object yourself"

Mark K suggests looking into "implementing Buffer Protocol in ImgLib2."
https://numpy.org/doc/stable/reference/arrays.interface.html#the-array-interface-protocol
We don't need it explicitly in Java though, it turns out, with the plan above.

## How to read .npy in Java

* https://github.com/JetBrains-Research/npy
* https://github.com/bioimage-io/model-runner-java/blob/main/src/main/java/io/bioimage/modelrunner/numpy/DecodeNumpy.java (not thoroughly tested)

[numpy.memmap]: https://numpy.org/doc/stable/reference/generated/numpy.memmap.html
