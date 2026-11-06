package dev.bkclient.systems.commands;

import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public sealed interface ValidationResult permits ValidationResult.Ok, ValidationResult.Error {
   static <T> ValidationResult.Ok<T> ok(T value) {
      return new ValidationResult.Ok<>(value);
   }

   static ValidationResult.Error error(String msg) {
      MessageUtility.error(Text.of(msg));
      return new ValidationResult.Error(msg);
   }

   record Error(String message) implements ValidationResult {
   }

   record Ok<T>(T value) implements ValidationResult {
   }
}
