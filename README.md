# duke

🛠️ _Java's mascot and mechanic_

![icon.png](.idea/icon.png)

## Install Duke

There are several ways installing Duke so that you can use it in your project.
The recommended way is to install the source files of Duke as a Git Submodule.

### Install Duke as Git Submodule

Mounting Duke's source files inside your project as a Git Submodule grants you advantages:

- transparency: source code with specification documentation in your hand
- control: debug a run by setting break points and tweak workflows on-the-fly

Use `git` to add Duke's source files directly as a submodule into your project.

- `git submodule add https://github.com/sormuras/run.duke.git .duke/src/run.duke`

Finish the installation of Duke by launching the included initialization program.

- `java .duke/src/run.duke/run/duke/Init.java`

The following files are created by the initialization program.

```text
.duke/bin/run.duke.jar | modular JAR file
.duke/.gitignore       | intentionally untracked files to ignore
duke                   | arguments for the Java Launcher (`java @duke`) 
```

Note, that the modular JAR file `run.duke.jar` in the `.duke/bin` directory is always created.
An existing JAR file containing `run.duke` module will be replaced by the initialization program.
Other files are only created if they do not exist.

It is a widespread convention to track only text files.
For example the generated and edited `duke` argument file.
Same goes for the default or tweaked `.duke/.gitignore` file.
And also for all Duke's source files stored below the `.duke/src/run.duke` directory.

## Launch Duke in a Terminal

Launch Duke using an argument file.

- `java @duke <tool> <args...>`

The minimal content of the `duke` argument file is:

```text
--module-path=.duke/bin
--module=run.duke
```

## Launch Duke in an IDE

...

### Launch and Debug Duke in IntelliJ IDEA

...
