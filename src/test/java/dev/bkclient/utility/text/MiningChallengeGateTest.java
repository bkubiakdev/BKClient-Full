package dev.bkclient.utility.text;
import java.util.Set;
public class MiningChallengeGateTest {
 public static void main(String[] args) {
  var gate=new MiningChallengeGate();
  if(!"kopanie 00123".equals(gate.observe(true,Set.of("00123"),100)))throw new AssertionError("initial submit");
  if(gate.observe(true,Set.of("00123"),5000)!=null)throw new AssertionError("duplicate");
  gate.observe(false,Set.of(),5500);if(!gate.waiting())throw new AssertionError("brief disappearance");
  gate.observe(false,Set.of(),6000);if(!gate.waiting())throw new AssertionError("disappearance is not confirmation");
  if(!gate.confirm()||gate.waiting())throw new AssertionError("confirmed resume");
  if(gate.observe(true,Set.of("00123"),6100)!=null||gate.waiting())throw new AssertionError("lingering hologram");
  gate.observe(false,Set.of(),7200);
  if(!"kopanie 00123".equals(gate.observe(true,Set.of("00123"),7300)))throw new AssertionError("new challenge");
  if(gate.observe(true,Set.of("456"),7100)!=null)throw new AssertionError("rate limit");
  if(!"kopanie 456".equals(gate.observe(true,Set.of("456"),11000)))throw new AssertionError("changed code");
  gate.reset();gate.observe(true,Set.of(),11000);if(!gate.waiting())throw new AssertionError("unknown must pause");
  if(gate.observe(true,Set.of("12","34"),12000)!=null)throw new AssertionError("ambiguous");
  if(gate.observe(true,Set.of("bad command"),13000)!=null)throw new AssertionError("untrusted command");
  if(!MiningChallengeGate.isSuccessMessage("§aPrawidłowy kod! Możesz dalej kopać!")
    || MiningChallengeGate.isSuccessMessage("Player: Prawidłowy kod! Możesz dalej kopać!"))throw new AssertionError("confirmation filter");
  gate.reset();if(gate.waiting())throw new AssertionError("disable/world reset");
  System.out.println("Mining challenge: submission, deduplication, resume, rate limit, unknown and reset passed");
 }
}
