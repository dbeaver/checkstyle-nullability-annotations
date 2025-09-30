import java.util.List;

public class Sample {
    private final @NotNull String test = "value";
    private @Nullable String test2;
    private int test3;

    public Sample(@NotNull String a, int b) {
    }

    public List<String> test(int a, @Nullable String b) {
        return List.of();
    }

    record Inner(@NotNull String a, @Nullable List<String> b, int c) {
        Inner(@NotNull String a, @Nullable List<String> b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}