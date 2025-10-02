import java.util.List;
import javax.annotation.Nonnull;

public class Sample {
    private final String test = "value";
    private String test2;
    private int test3;

    public Sample(@Nonnull String a, @Nonnull int b) {
    }

    public List<String> test(int a, String b) {
        return List.of();
    }

    record Inner(String a, List<String> b, int c) {
        Inner(String a, List<String> b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}