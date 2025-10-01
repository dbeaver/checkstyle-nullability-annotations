public class Sample {
    @javax.annotation.Nonnull
    public List<String> test(int a, @javax.annotation.Nullable String b) {
        return List.of();
    }

    public void test2(String value) {
    }

    public void test3(@lombok.NonNull String value) {
    }
}