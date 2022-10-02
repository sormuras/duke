package run.bach;

@FunctionalInterface
public interface Creator {
  Bach createBach(Configuration configuration);
}
