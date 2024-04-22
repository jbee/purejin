class Rebuild {
  public static void main(String... args) {
    var project = Project.ofCurrentWorkingDirectory();
    project.clean();
    project.build();
  }
}
