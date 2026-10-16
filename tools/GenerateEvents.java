import java.time.Instant;
import java.util.Random;

/**
 * Generates synthetic payment events as JSON lines (no real customer data).
 * Usage: java tools/GenerateEvents.java 200 > sample-data/events.jsonl
 * Roughly 5% of events are deliberate duplicates and 2% are invalid, to exercise the pipeline.
 */
public class GenerateEvents {
    public static void main(String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        Random rnd = new Random(42);
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        String last = null;
        for (int i = 0; i < count; i++) {
            if (last != null && rnd.nextDouble() < 0.05) {
                System.out.println(last);
                continue;
            }
            String paymentId = "pay-" + (1000 + i / 3);
            String type = switch (i % 3) { case 0 -> "AUTHORIZED"; case 1 -> "CAPTURED"; default -> "REFUNDED"; };
            long amount = switch (i % 3) { case 0 -> 10_000; case 1 -> 10_000; default -> 2_500; };
            String currency = rnd.nextDouble() < 0.02 ? "???" : "USD";
            t = t.plusSeconds(1 + rnd.nextInt(30));
            last = String.format(
                    "{\"eventId\":\"evt-%05d\",\"paymentId\":\"%s\",\"type\":\"%s\",\"amountMinor\":%d,\"currency\":\"%s\",\"occurredAt\":\"%s\"}",
                    i, paymentId, type, amount, currency, t);
            System.out.println(last);
        }
    }
}
