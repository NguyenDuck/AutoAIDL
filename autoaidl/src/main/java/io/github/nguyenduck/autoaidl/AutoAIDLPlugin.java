package io.github.nguyenduck.autoaidl;

import com.android.build.api.variant.AndroidComponentsExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

public class AutoAIDLPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        String version = AutoAIDLPlugin.class
                .getPackage()
                .getImplementationVersion();

        // Auto add annotation dependency
        project.getDependencies().add("compileOnly", "io.github.nguyenduck:autoaidl:" + version);

        project.getPlugins().withId("com.android.application", plugin ->
                configureAndroid(project));

        project.getPlugins().withId("com.android.library", plugin ->
                configureAndroid(project));
    }

    private void configureAndroid(Project project) {

        AndroidComponentsExtension<?, ?, ?> androidComponents =
                project.getExtensions()
                        .getByType(AndroidComponentsExtension.class);

        androidComponents.onVariants(androidComponents.selector().all(), variant -> {

            String taskName = "generate" + capitalize(variant.getName()) + "AutoAidl";

            File generatedDir = new File(
                    project.getLayout().getBuildDirectory().getAsFile().get(),
                    "generated/aidl/" + variant.getName()
            );

            TaskProvider<GenerateAidlTask> taskProvider =
                    project.getTasks().register(taskName, GenerateAidlTask.class, task -> {
                        task.setGroup("build");
                        task.getSourceDir().set(new File(project.getProjectDir(), "src/main/java"));
                        task.getOutputDir().set(generatedDir);
                    });

            // Inject AIDL source folder
            variant.getSources()
                    .getAidl()
                    .addGeneratedSourceDirectory(
                            taskProvider,
                            GenerateAidlTask::getOutputDir
                    );
        });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}