import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Sample {
    @Nonnull
    public List<String> test(int a, @Nullable String b) {
        return List.of();
    }
}