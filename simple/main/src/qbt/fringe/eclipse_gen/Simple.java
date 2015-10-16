package qbt.fringe.eclipse_gen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import qbt.QbtHashUtils;
import qbt.QbtUtils;

public class Simple {
    private static final String[] POTENTIAL_SOURCE_DIRECTORY = new String[]{"src", "test"};

    public static void main(String[] args) throws IOException {
        Path inputsDir = Paths.get(System.getenv("INPUT_DEV_PROTO_DIR"));
        Path outputsDir = Paths.get(System.getenv("OUTPUT_DEV_PROTO_DIR"));
        String packageName = System.getenv("PACKAGE_NAME");

        ImmutableList.Builder<String> classpaths = ImmutableList.builder();
        Path fixedDir = inputsDir.resolve("proto/fixed");
        Path overriddenDir = inputsDir.resolve("proto/overridden");
        Path jarDir = Paths.get(System.getenv("HOME")).resolve(".qbt/eclipse_gen-jars/v1");

        QbtUtils.mkdirs(jarDir);
        for(Path packageDir : QbtUtils.listChildren(fixedDir)) {
            Path jarsDir = packageDir.resolve("jars");
            if(!Files.isDirectory(jarsDir)) {
                continue;
            }
            for(Path jar : QbtUtils.listChildren(jarsDir)) {
                if(!Files.isRegularFile(jar)) {
                    continue;
                }
                if(!jar.getFileName().toString().endsWith(".jar")) {
                    continue;
                }
                Path cacheJar = copyToCache(jarDir, "bin", jar);
                String sourceClasspath = "";
                Path src = packageDir.resolve("sources").resolve(jar.getFileName());
                if(Files.isRegularFile(src)) {
                    Path cacheSrc = copyToCache(jarDir, "src", src);
                    sourceClasspath = " sourcepath=\"" + cacheSrc.toAbsolutePath() + "\"";
                }
                classpaths.add("<classpathentry kind=\"lib\" path=\"" + cacheJar.toAbsolutePath() + "\"" + sourceClasspath + "/>");
            }
        }

        for(Path packageDir : QbtUtils.listChildren(overriddenDir)) {
            Path projectNameFile = packageDir.resolve("projectName");
            if(Files.exists(projectNameFile)) {
                String pkg = Iterables.getOnlyElement(QbtUtils.readLines(projectNameFile));
                classpaths.add("<classpathentry kind=\"src\" path=\"/" + pkg + "\"/>");
            }
        }

        ImmutableList.Builder<String> project = ImmutableList.builder();
        project.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        project.add("<projectDescription>");
        project.add("    <name>" + packageName + "</name>");
        project.add("    <comment></comment>");
        project.add("    <projects>");
        project.add("    </projects>");
        project.add("    <buildSpec>");
        project.add("        <buildCommand>");
        project.add("            <name>org.eclipse.jdt.core.javabuilder</name>");
        project.add("            <arguments>");
        project.add("            </arguments>");
        project.add("        </buildCommand>");
        project.add("    </buildSpec>");
        project.add("    <natures>");
        project.add("        <nature>org.eclipse.jdt.core.javanature</nature>");
        project.add("    </natures>");
        project.add("</projectDescription>");
        QbtUtils.writeLines(Paths.get(".project"), project.build());

        ImmutableList.Builder<String> classpath = ImmutableList.builder();
        classpath.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        classpath.add("<classpath>");

        for(String potentialSourceDirectory : POTENTIAL_SOURCE_DIRECTORY) {
            if(Files.exists(Paths.get(potentialSourceDirectory))) {
                classpath.add("    <classpathentry kind=\"src\" path=\"" + potentialSourceDirectory + "\"/>");
            }
        }

        classpath.add("    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>");
        for(String e : classpaths.build()) {
            classpath.add("    " + e);
        }
        classpath.add("    <classpathentry kind=\"output\" path=\"bin\"/>");
        classpath.add("</classpath>");
        QbtUtils.writeLines(Paths.get(".classpath"), classpath.build());

        QbtUtils.writeLines(outputsDir.resolve("projectName"), ImmutableList.of(packageName));
    }

    private static Path copyToCache(Path cacheDir, String type, Path file) throws IOException {
        String toName = QbtHashUtils.hash(file) + "-" + type + "-" + file.getFileName();
        Path toFile = cacheDir.resolve(toName);
        if(!Files.isRegularFile(toFile)) {
            Path siblingFile = cacheDir.resolve("." + toName + "." + QbtHashUtils.random());
            Files.copy(file, siblingFile);
            try {
                Files.move(siblingFile, toFile, StandardCopyOption.ATOMIC_MOVE);
            }
            catch(RuntimeException e) {
                if(!Files.isRegularFile(toFile)) {
                    throw e;
                }
            }
            finally {
                if(Files.isRegularFile(siblingFile)) {
                    QbtUtils.delete(siblingFile);
                }
            }
        }
        return toFile;
    }
}
