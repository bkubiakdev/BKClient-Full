package dev.bkclient.utility.render.bkengine;

@FunctionalInterface
public interface Easing {
    double ease(double value);
}