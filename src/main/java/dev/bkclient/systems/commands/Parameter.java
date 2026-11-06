package dev.bkclient.systems.commands;

public record Parameter<T>(String name, boolean required, boolean vararg, ParameterValidator<T> validator) {
}
