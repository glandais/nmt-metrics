package com.marekcabaj.nmt.jcmd;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.marekcabaj.nmt.bean.NativeMemoryTrackingKind;
import com.marekcabaj.nmt.bean.NativeMemoryTrackingValues;

public class NMTExtractorTest {

    private NativeMemoryTrackingValues nmtProperties;

    @Before
    public void setUp() {
     final String testJcmdOutput = """
Total: reserved=1470626KB, committed=170826KB
-                 Java Heap (reserved=65536KB, committed=46592KB)
                            (mmap: reserved=65536KB, committed=46592KB) 
 
-                     Class (reserved=1081294KB, committed=36814KB)
                            (classes #5962)
                            (malloc=4046KB #6901) 
                            (mmap: reserved=1077248KB, committed=32768KB) 
 
-                    Thread (reserved=22009KB, committed=22009KB)
                            (thread #22)
                            (stack: reserved=21504KB, committed=21504KB)
                            (malloc=65KB #112) 
                            (arena=440KB #42)
 
-                      Code (reserved=252309KB, committed=16101KB)
                            (malloc=2709KB #3757) 
                            (mmap: reserved=249600KB, committed=13392KB) 
 
-                        GC (reserved=6028KB, committed=5860KB)
                            (malloc=3468KB #184) 
                            (mmap: reserved=2560KB, committed=2392KB) 
 
-                  Compiler (reserved=8424KB, committed=8424KB)
                            (malloc=9KB #111) 
                            (arena=8415KB #8)
 
-                  Internal (reserved=4155KB, committed=4155KB)
                            (malloc=4091KB #7583) 
                            (mmap: reserved=64KB, committed=64KB) 
 
-                    Symbol (reserved=9378KB, committed=9378KB)
                            (malloc=6557KB #58783) 
                            (arena=2821KB #1)
 
-    Native Memory Tracking (reserved=1232KB, committed=1232KB)
                            (malloc=5KB #66) 
                            (tracking overhead=1227KB)
 
-               Arena Chunk (reserved=20262KB, committed=20262KB)
                            (malloc=20262KB) """;
        nmtProperties = new NMTPropertiesExtractor().extractFromJcmdOutput(testJcmdOutput);
    }

    @Test
    public void testGetTotal() {
        final String key = "total";
        assertEquals(1470626, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(170826, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetHeap() {
        final String key = "java.heap";
        assertEquals(65536, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(46592, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetClass() {
        final String key = "class";
        assertEquals(1081294, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(36814, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetThread() {
        final String key = "thread";
        assertEquals(22009, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(22009, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetCode() {
        final String key = "code";
        assertEquals(252309, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(16101, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGC() {
        final String key = "gc";
        assertEquals(6028, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(5860, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetCompiler() {
        final String key = "compiler";
        assertEquals(8424, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(8424, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetInternal() {
        final String key = "internal";
        assertEquals(4155, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(4155, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetSymbol() {
        final String key = "symbol";
        assertEquals(9378, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(9378, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetNMT() {
        final String key = "native.memory.tracking";
        assertEquals(1232, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(1232, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }

    @Test
    public void testGetArenaChunk() {
        final String key = "arena.chunk";
        assertEquals(20262, nmtProperties.get(NativeMemoryTrackingKind.RESERVED).get(key).longValue());
        assertEquals(20262, nmtProperties.get(NativeMemoryTrackingKind.COMMITTED).get(key).longValue());
    }
}
