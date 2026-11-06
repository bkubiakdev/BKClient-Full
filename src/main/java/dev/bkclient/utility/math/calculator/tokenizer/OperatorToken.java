package dev.bkclient.utility.math.calculator.tokenizer;

import dev.bkclient.utility.math.calculator.operator.Operator;

public class OperatorToken extends Token {
   private final Operator operator;

   public OperatorToken(Operator op) {
      super(2);
      if (op == null) {
         throw new IllegalArgumentException("Operator is unknown for token.");
      }

      this.operator = op;
   }

   public Operator getOperator() {
      return this.operator;
   }
}
