package com.example.graham.scripter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Graham on 06/03/2017.
 */
public class DownloadScriptTest {
    @Test
    public void ensureHttpInURL() throws Exception {
        String input = "www.dcu.ie";
        String expected = "http://www.dcu.ie";
        String output = DownloadScript.ensureHttpInURL(input);
        assertEquals(output, expected);
    }

    @Test
    public void ensureJsInFilename() throws Exception {
        String input = "testscript";
        String expected = "testscript.js";
        String output = DownloadScript.ensureJsInFilename(input);
        assertEquals(output, expected);
    }
}