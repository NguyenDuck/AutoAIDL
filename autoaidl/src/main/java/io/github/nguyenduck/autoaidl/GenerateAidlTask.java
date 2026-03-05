package io.github.nguyenduck.autoaidl;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.file.*;
import java.text.Format;
import java.util.*;
import java.util.Optional;
import java.util.zip.CRC32;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.*;

public abstract class GenerateAidlTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getSourceDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() throws Exception {
        File src = getSourceDir().get().getAsFile();
        File aidlDir = getOutputDir().get().getAsFile();

        JavaParser parser = new JavaParser();

        Files.walk(src.toPath())
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> processFile(parser, path, aidlDir));
    }

    @SuppressWarnings("NewApi")
    private void processFile(JavaParser parser,
                             Path path,
                             File aidlDir) {
        try {

            CompilationUnit cu =
                    parser.parse(path).getResult().orElse(null);

            if (cu == null) return;

            List<ClassOrInterfaceDeclaration> interfaces = cu.findAll(ClassOrInterfaceDeclaration.class)
                    .stream()
                    .filter(ClassOrInterfaceDeclaration::isInterface)
                    .filter(i -> i.getAnnotationByName("AIDLInterface").isPresent()
                            || i.getAnnotationByName("ShizukuInterface").isPresent())
                    .toList();

            for (ClassOrInterfaceDeclaration i : interfaces) {
                generateAidl(i, cu, aidlDir, path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void generateAidl(ClassOrInterfaceDeclaration iface,
                              CompilationUnit cu,
                              File aidlDir,
                              Path srcPath) throws Exception {

        String pkg = cu.getPackageDeclaration()
                .map(NodeWithName::getNameAsString)
                .orElse("");

        String name = iface.getNameAsString();

        Map<Integer, MethodDeclaration> idMap = new TreeMap<>();
        Set<Integer> used = new HashSet<>();

        boolean isShizukuInterface = iface.getAnnotationByName("ShizukuInterface").isPresent();

        for (MethodDeclaration m : iface.getMethods()) {
            int id;

            boolean hasMethodId = m.getAnnotationByName("MethodId").isPresent();

            if (hasMethodId) {
                id = Integer.parseInt(
                        m.getAnnotationByName("MethodId")
                                .get()
                                .asSingleMemberAnnotationExpr()
                                .getMemberValue()
                                .toString());
            } else {
                id = isShizukuInterface ? hashSignature(m) & 0xFFFF99 : hashSignature(m);
            }

            while (used.contains(id)) {
                if (hasMethodId) {
                    Optional<Position> begin = m.getBegin();
                    String info = "unknown";
                    if (begin.isPresent()) {
                        Position pos = begin.get();
                        info = String.format("%s:%s:%s", srcPath, pos.line, pos.column);
                    }
                    throw new RuntimeException(String.format("Method id is duplicated at %s, please use other id!", info));
                }
                id++;
                if (isShizukuInterface) id &= 0xFFFF99;
            }
            used.add(id);

            idMap.put(id, m);
        }

        if (isShizukuInterface) {
            idMap.put(0xFFFF9A, null);
        }

        File pkgDir = new File(aidlDir, pkg.replace(".", "/"));
        pkgDir.mkdirs();

        File file = new File(pkgDir, name + "AIDL.aidl");

        try (PrintWriter out = new PrintWriter(file)) {

            out.println("package " + pkg + ";");
            out.println();
            out.println("interface " + name + "AIDL {");
            out.println();

            for (Map.Entry<Integer, MethodDeclaration> e : idMap.entrySet()) {

                int id = e.getKey();
                MethodDeclaration m = e.getValue();

                if (m == null) {
                    out.println("    void destroy() = 16777114;");
                    continue;
                }

                out.print("    " +
                        m.getType().asString() +
                        " " +
                        m.getNameAsString() +
                        "(");

                for (int i = 0; i < m.getParameters().size(); i++) {

                    if (i > 0) out.print(", ");

                    out.print("in " +
                            m.getParameter(i).getType().asString() +
                            " " +
                            m.getParameter(i).getNameAsString());
                }

                out.println(") = " + id + ";");
                out.println();
            }

            out.println("}");
        }
    }

    private int hashSignature(MethodDeclaration m) {

        String sig = m.getDeclarationAsString(false, false, false);

        CRC32 crc = new CRC32();
        crc.update(sig.getBytes());

        return (int) (crc.getValue() & 0x7FFFFFFF);
    }
}
