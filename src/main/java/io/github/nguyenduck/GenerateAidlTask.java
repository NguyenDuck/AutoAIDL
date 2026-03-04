package io.github.nguyenduck;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

public abstract class GenerateAidlTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {

        File src = new File(getProject()
                .getProjectDir(),
                "src/main/java");

        File aidlDir = new File(getProject()
                .getProjectDir(),
                "src/main/aidl");

        JavaParser parser = new JavaParser();

        Files.walk(src.toPath())
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> processFile(parser, path, aidlDir));
    }

    private void processFile(JavaParser parser,
                             Path path,
                             File aidlDir) {

        try {

            CompilationUnit cu =
                    parser.parse(path).getResult().orElse(null);

            if (cu == null) return;

            cu.findAll(ClassOrInterfaceDeclaration.class)
                    .stream()
                    .filter(ClassOrInterfaceDeclaration::isInterface)
                    .filter(i -> i.getAnnotationByName("AIDLInterface").isPresent()
                            || i.getAnnotationByName("ShizukuInterface").isPresent())
                    .forEach(i -> {
                        try {
                            generateAidl(i, cu, aidlDir);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void generateAidl(ClassOrInterfaceDeclaration iface,
                              CompilationUnit cu,
                              File aidlDir) throws Exception {

        String pkg = cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse("");

        String name = iface.getNameAsString();

        Map<Integer, MethodDeclaration> idMap = new TreeMap<>();
        Set<Integer> used = new HashSet<>();

        for (MethodDeclaration m : iface.getMethods()) {

            int id;

            if (m.getAnnotationByName("AIDLMethod").isPresent()) {
                id = Integer.parseInt(
                        m.getAnnotationByName("AIDLMethod")
                                .get()
                                .asSingleMemberAnnotationExpr()
                                .getMemberValue()
                                .toString());
            } else {
                id = hashSignature(m);
            }

            while (used.contains(id)) id++;
            used.add(id);

            idMap.put(id, m);
        }

        if (iface.getAnnotationByName("ShizukuInterface").isPresent()) {
            idMap.put(1000, null);
        }

        File pkgDir = new File(aidlDir, pkg.replace(".", "/"));
        pkgDir.mkdirs();

        File file = new File(pkgDir, name + ".aidl");

        try (PrintWriter out = new PrintWriter(file)) {

            out.println("package " + pkg + ";");
            out.println();
            out.println("interface " + name + " {");
            out.println();

            for (Map.Entry<Integer, MethodDeclaration> e : idMap.entrySet()) {

                int id = e.getKey();
                MethodDeclaration m = e.getValue();

                if (m == null) {
                    out.println("    int getUid() = 1000;");
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
