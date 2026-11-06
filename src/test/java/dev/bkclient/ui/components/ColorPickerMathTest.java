package dev.bkclient.ui.components;

public final class ColorPickerMathTest {
    public static void main(String[] args) {
        pair(ColorPickerMath.hsvToHsl(1, 1), 1, .5F);
        pair(ColorPickerMath.hsvToHsl(0, 1), 0, 1);
        pair(ColorPickerMath.hslToHsv(1, 0), 0, 0);
        pair(ColorPickerMath.hslToHsv(1, 1), 0, 1);
        for (int s = 0; s <= 100; s++) for (int v = 1; v <= 100; v++) {
            float[] hsl = ColorPickerMath.hsvToHsl(s / 100F, v / 100F);
            pair(ColorPickerMath.hslToHsv(hsl[0], hsl[1]), s / 100F, v / 100F);
        }
        near(ColorPickerMath.parseChannel(" 50% ", 100), 50);
        near(ColorPickerMath.parseChannel("255", 255), 255);
        for (String invalid : new String[]{"NaN", "Infinity", "-1", "256", "", "#ff00ff"}) {
            try { ColorPickerMath.parseChannel(invalid, 255); throw new AssertionError("Accepted " + invalid); }
            catch (IllegalArgumentException expected) {}
        }
        System.out.println("Color picker: conversion boundaries, 10100 round trips and input validation passed");
    }
    private static void pair(float[] actual, float a, float b) { near(actual[0], a); near(actual[1], b); }
    private static void near(float actual, float expected) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > .00003F) throw new AssertionError(actual + " != " + expected);
    }
}
