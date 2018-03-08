package com.example.graham.scripter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Graham on 06/03/2017.
 */
public class MonitorStateTest {
    @Test
    public void evaluateJS() throws Exception {
        String input0 = "2+2";
        String input1 = "testscript.js";
        String expected = "4";
        String output = new MonitorState().evaluateJS(input0, input1);
        assertEquals(output, expected);
    }

    @Test
    public void addVariableToScope() throws Exception {
        assertEquals(new MonitorState().evaluateJS("dayOfMonth", "testscript.js"), String.valueOf(MonitorState.getStateByIndex(0)));
    }

    @Test
    public void addFunctionToScope() throws Exception {

    }

    @Test
    public void setEqualTo() throws Exception {
        Object[] input0 = new Object[3];
        Object[] input1 = {"Object 1", "Object 2", "Object 3"};
        boolean expected = true;
        boolean output = new MonitorState().setEqualTo(input0, input1);
        assertEquals(output, expected);
    }

    @Test
    public void isEqualTo() throws Exception {
        Object[] input0 = {"Object 1", "Object 2", "Object 3"};
        Object[] input1 = {"Object 1", "Object 2", "Object 3"};
        boolean expected = true;
        boolean output = new MonitorState().isEqualTo(input0, input1);
        assertEquals(output, expected);
    }

    @Test
    public void getDayOfWeek() throws Exception {
        int input = 2;
        String expected = "Monday";
        String output = MonitorState.getDayOfWeek(input);
        assertEquals(output, expected);
    }

    @Test
    public void updateStateByIndex() throws Exception {
        int input0 = 3;
        Object input1 = "Object";
        MonitorState.updateStateByIndex(input0, input1);
        assertEquals(MonitorState.getStateByIndex(3), input1);
    }

    @Test
    public void getStateByIndex() throws Exception {
        int input = 3;
        Object expected = "Object";
        MonitorState.updateStateByIndex(input, expected);
        assertEquals(MonitorState.getStateByIndex(3), expected);
    }
}