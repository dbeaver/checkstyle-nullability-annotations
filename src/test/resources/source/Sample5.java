import javax.annotation.Nullable;

public class Sample {
    @javax.annotation.Nonnull
    public List<String> test(int a, @Nullable String b) {
        return List.of();
    }
}