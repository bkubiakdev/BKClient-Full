package dev.bkclient.utility.math.calculator.tokenizer;

import dev.bkclient.utility.math.calculator.function.Function;

public class FunctionToken extends Token {
   private final Function function;

   public FunctionToken(Function function) {
      super(3);
      this.function = function;
   }

   public Function getFunction() {
      return this.function;
   }
}
