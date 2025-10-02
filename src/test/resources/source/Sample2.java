import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Sample {
    private final @Nonnull String test = "value";
    private @Nullable String test2;
    private int test3;

    public Sample(@Nonnull String a, int b) {
    }

    @Nonnull
    public List<String> test(int a, @Nullable String b) {
        return List.of();
    }

    record Inner(@Nonnull String a, @Nullable List<String> b, int c) {
        Inner(@Nonnull String a, @Nullable List<String> b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}