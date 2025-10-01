import javax.annotation.*;

public class Sample {
    @Nonnull
    public List<String> test(int a, @Nullable String b) {
        return List.of();
    }
}